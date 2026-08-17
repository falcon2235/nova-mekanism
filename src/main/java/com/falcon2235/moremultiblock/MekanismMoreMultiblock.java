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

    /** Sanity log so override problems are visible in the log instead of silently reverting recipes. */
    private static void onServerStarted(ServerStartedEvent event) {
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
