package com.falcon2235.moremultiblock;

import com.falcon2235.moremultiblock.content.ChemRegistry;
import com.falcon2235.moremultiblock.multiblock.ParallelClaimRegistry;

import java.nio.file.Path;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.PathPackResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MekanismMoreMultiblock.MODID)
public class MekanismMoreMultiblock {

    public static final String MODID = "mekanism_more_multiblock";
    private static final Logger LOGGER = LoggerFactory.getLogger("MekanismMoreMultiblock");

    public MekanismMoreMultiblock() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MMMRegistry.register(modEventBus);
        ChemRegistry.register(modEventBus);
        com.falcon2235.moremultiblock.worldgen.ConfigurableOreModifier.register(modEventBus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON, MMMConfig.SPEC);
        // Recipes bake config values in, so drop the cache whenever the config (re)loads.
        modEventBus.addListener((net.minecraftforge.fml.event.config.ModConfigEvent event) -> {
            if (event.getConfig().getSpec() == MMMConfig.SPEC) {
                com.falcon2235.moremultiblock.machine.ChemRecipes.invalidateCache();
            }
        });
        modEventBus.addListener(MekanismMoreMultiblock::addPackFinders);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> ParallelClaimRegistry.clear());
        MinecraftForge.EVENT_BUS.addListener(MekanismMoreMultiblock::onServerStarted);
    }

    /**
     * Registers the bundled "overrides" data pack at TOP priority. Same-path recipe
     * overrides in a mod's normal datapack lose to mods that load later in the pack
     * order (e.g. mekanismgenerators sorts after this mod), so replacements for other
     * mods' recipes must live in this always-on, highest-priority pack.
     */
    private static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        if (!MMMConfig.hardenMekanismRecipes()) {
            // Modpack authors can opt out of the Mekanism recipe nerfs entirely.
            LOGGER.info("Recipe override pack disabled by config (integration.hardenMekanismRecipes)");
            return;
        }
        Path path = ModList.get().getModFileById(MODID).getFile().findResource("overrides");
        Pack pack = Pack.readMetaAndCreate(
                MODID + ":overrides",
                Component.literal("MMM Recipe Overrides"),
                true,
                id -> new PathPackResources(id, true, path),
                PackType.SERVER_DATA,
                Pack.Position.TOP,
                PackSource.BUILT_IN);
        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        } else {
            LOGGER.error("Failed to create the recipe override pack");
        }
    }

    /**
     * Reports any part a multiblock needs that nothing can make: every block in each
     * structure's bill of materials, plus every controller, checked against the recipe
     * manager and the mod's own machine recipes. An unobtainable part makes a machine
     * simply impossible to build, which is invisible until a player tries.
     */
    private static void obtainabilityAudit(ServerStartedEvent event) {
        var server = event.getServer();
        var recipes = server.getRecipeManager();
        java.util.Set<net.minecraft.world.item.Item> makeable = new java.util.HashSet<>();
        for (var r : recipes.getRecipes()) {
            try {
                ItemStack out = r.getResultItem(server.registryAccess());
                if (!out.isEmpty()) {
                    makeable.add(out.getItem());
                }
            } catch (Exception ignored) {
                // a recipe type that cannot resolve a result without a real inventory
            }
        }
        // the mod's own machine recipes produce parts too
        for (var type : com.falcon2235.moremultiblock.machine.ChemMachineType.values()) {
            for (var r : com.falcon2235.moremultiblock.machine.ChemRecipes.get(type)) {
                for (ItemStack out : r.allPossibleOutputs()) {
                    makeable.add(out.getItem());
                }
            }
        }

        java.util.List<String> problems = new java.util.ArrayList<>();
        for (var type : com.falcon2235.moremultiblock.machine.ChemMachineType.values()) {
            var controllerBlock = MMMRegistry.CHEM_CONTROLLERS.get(type).get();
            if (!makeable.contains(controllerBlock.asItem())) {
                problems.add(type.id + ": CONTROLLER has no recipe");
            }
            var state = controllerBlock.defaultBlockState()
                    .setValue(com.falcon2235.moremultiblock.block.ChemMachineBlock.FACING,
                            net.minecraft.core.Direction.NORTH);
            var cells = com.falcon2235.moremultiblock.multiblock.StructureBlueprint
                    .forController(net.minecraft.core.BlockPos.ZERO, state);
            if (cells == null) {
                continue;
            }
            java.util.Set<net.minecraft.world.level.block.Block> needed = new java.util.LinkedHashSet<>();
            cells.forEach(c -> needed.add(c.block()));
            for (var block : needed) {
                var item = block.asItem();
                if (item != net.minecraft.world.item.Items.AIR && !makeable.contains(item)) {
                    problems.add(type.id + ": needs " + block.getName().getString() + " which has no recipe");
                }
            }
        }
        if (problems.isEmpty()) {
            LOGGER.info("Obtainability audit — every multiblock part can be made");
        } else {
            LOGGER.warn("Obtainability audit — {} unobtainable part(s):", problems.size());
            problems.forEach(p -> LOGGER.warn("  {}", p));
        }
    }

    /** Sanity log so override problems are visible in the log instead of silently reverting recipes. */
    private static void onServerStarted(ServerStartedEvent event) {
        // Recipe reachability: a machine picks the first recipe it can satisfy, so a
        // broad recipe can hide a narrower one forever. Report any that are unreachable.
        var shadowed = com.falcon2235.moremultiblock.machine.ChemRecipes.findShadowedRecipes();
        if (shadowed.isEmpty()) {
            LOGGER.info("Recipe audit — every recipe is reachable");
        } else {
            LOGGER.warn("Recipe audit — {} unreachable recipe(s):", shadowed.size());
            shadowed.forEach(line -> LOGGER.warn("  {}", line));
        }
        obtainabilityAudit(event);
        // Integration debug: which optional mods and key items resolved. If a mod shows
        // loaded=true but an item false, the item id is wrong for that mod version.
        for (String[] probe : new String[][]{
                {"ae2", "charged_certus_quartz_crystal"},
                {"botania", "terrasteel_ingot"},
                {"megacells", "cell_component_4m"},
                {"draconicevolution", "awakened_core"},
                {"draconicevolution", "chaos_shard"},
                {"draconicevolution", "wyvern_core"},
                {"ars_nouveau", "source_gem"}}) {
            boolean loaded = ModList.get().isLoaded(probe[0]);
            boolean item = loaded && net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .containsKey(new ResourceLocation(probe[0], probe[1]));
            LOGGER.info("Integration check — {}: loaded={}, {} resolved={}", probe[0], loaded, probe[1], item);
        }
        var recipes = event.getServer().getRecipeManager();
        boolean steelGated = recipes.byKey(new ResourceLocation("mekanism", "processing/steel/enriched_iron_to_dust")).isEmpty();
        LOGGER.info("Override pack check — mekanism enriched-iron-to-steel disabled: {}", steelGated);
        if (ModList.get().isLoaded("mekanismgenerators")) {
            recipes.byKey(new ResourceLocation("mekanismgenerators", "fission_reactor/casing")).ifPresent(recipe -> {
                boolean hardened = recipe.getIngredients().stream()
                        .anyMatch(ing -> ing.test(new ItemStack(MMMRegistry.SPECIAL_STEEL_INGOT.get())));
                LOGGER.info("Override pack check — fission reactor casing uses special steel: {}", hardened);
            });
        }
    }
}
