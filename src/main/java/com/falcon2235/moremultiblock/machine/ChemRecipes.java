package com.falcon2235.moremultiblock.machine;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.content.ChemRegistry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.common.registries.MekanismGases;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * The hardcoded titanium production chain, built lazily after registration
 * (so gas/fluid registry objects are resolved) and cached per machine type.
 */
public final class ChemRecipes {

    private static final Map<ChemMachineType, List<ChemRecipe>> CACHE = new EnumMap<>(ChemMachineType.class);

    private ChemRecipes() {
    }

    public static synchronized List<ChemRecipe> get(ChemMachineType type) {
        return CACHE.computeIfAbsent(type, ChemRecipes::build);
    }

    /** Recipes ordered for MATCHING (see {@link #getForMatching}). */
    private static final Map<ChemMachineType, List<ChemRecipe>> MATCH_CACHE = new EnumMap<>(ChemMachineType.class);

    /**
     * The recipe list a machine should test against, most specific first.
     *
     * <p>A machine runs the first recipe whose inputs it can satisfy, so a recipe with
     * FEWER requirements shadows every later recipe that needs everything it needs plus
     * more. In the fusion reactor, for instance, "helium plasma + antimatter -> stellar
     * matter" would otherwise fire forever and the naquadria step of the neutronium
     * chain (same gases, plus an ingot) could never run. Sorting by descending
     * requirement count fixes that class of conflict for every machine at once.
     * Display order (JEI) is deliberately left untouched.
     */
    public static synchronized List<ChemRecipe> getForMatching(ChemMachineType type) {
        return MATCH_CACHE.computeIfAbsent(type, t -> {
            List<ChemRecipe> sorted = new ArrayList<>(get(t));
            sorted.sort((a, b) -> {
                int bySpecificity = Integer.compare(requirementCount(b), requirementCount(a));
                if (bySpecificity != 0) {
                    return bySpecificity;
                }
                // Same inputs, different coil requirement: run the best recipe the
                // installed coil allows, so upgrading a blast furnace actually pays off
                // (ancient debris -> netherite ingot on a plutonium coil, not scrap).
                return Integer.compare(b.coilTier, a.coilTier);
            });
            return List.copyOf(sorted);
        });
    }

    /** How many distinct things a recipe demands; higher = more specific. */
    private static int requirementCount(ChemRecipe r) {
        int n = 0;
        for (ItemStack in : new ItemStack[]{r.itemInput, r.itemInput2, r.itemInput3, r.itemInput4, r.itemInput5}) {
            if (!in.isEmpty()) {
                n++;
            }
        }
        if (!r.gasInput.isEmpty()) {
            n++;
        }
        if (!r.gasInput2.isEmpty()) {
            n++;
        }
        if (!r.fluidInput.isEmpty()) {
            n++;
        }
        if (!r.requiredUpgrade.isEmpty()) {
            n++;
        }
        if (r.manaCost > 0) {
            n++;
        }
        return n;
    }

    /**
     * Reports recipes that can never be selected because an earlier one in the matching
     * order demands a subset of the same things — i.e. whenever the shadowed recipe
     * could run, the earlier one runs instead. Returns human-readable lines; an empty
     * list means every recipe is reachable.
     */
    public static List<String> findShadowedRecipes() {
        List<String> problems = new ArrayList<>();
        for (ChemMachineType type : ChemMachineType.values()) {
            // These machines drive themselves from custom tick logic; their "recipes"
            // exist only to be displayed in JEI, so overlap between them is meaningless.
            // (The oil rig is NOT here: it runs its no-input recipe through findRecipe.)
            if (type == ChemMachineType.VOID_MINER || type == ChemMachineType.COMBUSTION_GENERATOR
                    || type == ChemMachineType.ANNIHILATION_GENERATOR) {
                continue;
            }
            List<ChemRecipe> ordered = getForMatching(type);
            for (int j = 0; j < ordered.size(); j++) {
                for (int i = 0; i < j; i++) {
                    if (shadows(ordered.get(i), ordered.get(j))) {
                        problems.add(String.format("%s: recipe #%d (%s) is shadowed by #%d (%s)",
                                type.id, j, describe(ordered.get(j)), i, describe(ordered.get(i))));
                        break;
                    }
                }
            }
        }
        return problems;
    }

    /** True when {@code a}'s requirements are a subset of {@code b}'s, so a always wins. */
    private static boolean shadows(ChemRecipe a, ChemRecipe b) {
        if (a == b) {
            return false;
        }
        // A different required module makes them mutually exclusive, never shadowing.
        if (!a.requiredUpgrade.isEmpty()
                && !ItemStack.isSameItemSameTags(a.requiredUpgrade, b.requiredUpgrade)) {
            return false;
        }
        if (a.coilTier > b.coilTier || a.manaCost > b.manaCost) {
            return false;
        }
        for (ItemStack need : a.itemOutputsInputs()) {
            if (!coveredBy(need, b)) {
                return false;
            }
        }
        if (!gasCoveredBy(a.gasInput, b) || !gasCoveredBy(a.gasInput2, b)) {
            return false;
        }
        return a.fluidInput.isEmpty()
                || (a.fluidInput.isFluidEqual(b.fluidInput) && a.fluidInput.getAmount() <= b.fluidInput.getAmount());
    }

    private static boolean coveredBy(ItemStack need, ChemRecipe b) {
        int available = 0;
        for (ItemStack in : b.itemOutputsInputs()) {
            if (ItemStack.isSameItemSameTags(in, need)) {
                available += in.getCount();
            }
        }
        return available >= need.getCount();
    }

    private static boolean gasCoveredBy(GasStack need, ChemRecipe b) {
        if (need.isEmpty()) {
            return true;
        }
        long available = 0;
        for (GasStack in : new GasStack[]{b.gasInput, b.gasInput2}) {
            if (!in.isEmpty() && in.isTypeEqual(need)) {
                available += in.getAmount();
            }
        }
        return available >= need.getAmount();
    }

    private static String describe(ChemRecipe r) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack in : r.itemOutputsInputs()) {
            sb.append(in.getCount()).append('x').append(in.getItem()).append(' ');
        }
        if (!r.gasInput.isEmpty()) {
            sb.append("gas:").append(r.gasInput.getAmount()).append(' ');
        }
        if (!r.fluidInput.isEmpty()) {
            sb.append("fluid:").append(r.fluidInput.getAmount()).append(' ');
        }
        List<ItemStack> outs = r.itemOutputs();
        sb.append("-> ").append(outs.isEmpty() ? "(gas/fluid)" : outs.get(0).getItem());
        return sb.toString().trim();
    }

    /** Drops all cached recipes so the next access rebuilds them with fresh config values. */
    public static synchronized void invalidateCache() {
        CACHE.clear();
        MATCH_CACHE.clear();
    }

    private static List<ChemRecipe> build(ChemMachineType type) {
        List<ChemRecipe> list = buildRaw(type);
        // Global balance multipliers. The generator/miner machines are excluded: their
        // JEI entries describe custom per-tick logic tuned in their own config sections.
        boolean customLogicDisplay = type == ChemMachineType.COMBUSTION_GENERATOR
                || type == ChemMachineType.ANNIHILATION_GENERATOR
                || type == ChemMachineType.VOID_MINER;
        double energyMult = com.falcon2235.moremultiblock.MMMConfig.recipeEnergyMultiplier();
        double timeMult = com.falcon2235.moremultiblock.MMMConfig.recipeTimeMultiplier();
        if (!customLogicDisplay && (energyMult != 1.0D || timeMult != 1.0D)) {
            for (ChemRecipe recipe : list) {
                recipe.energyPerTick = Math.max(1L, (long) Math.ceil(recipe.energyPerTick * energyMult));
                recipe.ticks = Math.max(1, (int) Math.round(recipe.ticks * timeMult));
            }
        }
        return list;
    }

    private static List<ChemRecipe> buildRaw(ChemMachineType type) {
        List<ChemRecipe> list = new ArrayList<>();
        switch (type) {
            case BLAST_FURNACE -> {
                // --- tier 0 (copper coil): basic ore smelting ---
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.RAW_IRON, 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(net.minecraft.world.item.Items.IRON_INGOT, 2), GasStack.EMPTY, FluidStack.EMPTY,
                        200, 100L, 0));
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.RAW_COPPER, 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(net.minecraft.world.item.Items.COPPER_INGOT, 2), GasStack.EMPTY, FluidStack.EMPTY,
                        200, 100L, 0));
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.RAW_GOLD, 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(net.minecraft.world.item.Items.GOLD_INGOT, 2), GasStack.EMPTY, FluidStack.EMPTY,
                        200, 100L, 0));
                // iron -> steel directly (electric alternative to the primitive blast furnace)
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.IRON_INGOT, 1), GasStack.EMPTY, FluidStack.EMPTY,
                        mekItem("ingot_steel", 1), GasStack.EMPTY, FluidStack.EMPTY,
                        400, 200L, 0));
                // nickel + cupronickel smelting
                list.add(new ChemRecipe(
                        item(MMMRegistry.NICKEL_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.NICKEL_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        240, 150L, 0));
                list.add(new ChemRecipe(
                        item(MMMRegistry.CUPRONICKEL_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.CUPRONICKEL_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        240, 150L, 0));

                // --- tier 1 (cupronickel coil): the titanium chain ---
                // stage 1: raw titanium -> titanium oxide
                list.add(new ChemRecipe(
                        item(MMMRegistry.RAW_TITANIUM.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.TITANIUM_OXIDE.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        240, 200L, 1));
                // magnesium dust -> liquid magnesium (reductant for stage 4)
                list.add(new ChemRecipe(
                        item(MMMRegistry.MAGNESIUM_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, liquidMagnesium(144),
                        200, 150L, 1));
                // stage 5: titanium sponge -> titanium ingot
                list.add(new ChemRecipe(
                        item(MMMRegistry.TITANIUM_SPONGE.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.TITANIUM_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        280, 300L, 1));

                // --- tier 2 (titanium coil): nether-grade metallurgy ---
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.ANCIENT_DEBRIS, 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(net.minecraft.world.item.Items.NETHERITE_SCRAP, 2), GasStack.EMPTY, FluidStack.EMPTY,
                        400, 500L, 2));
                list.add(new ChemRecipe(
                        mekItem("dust_refined_obsidian", 1), GasStack.EMPTY, FluidStack.EMPTY,
                        mekItem("ingot_refined_obsidian", 1), GasStack.EMPTY, FluidStack.EMPTY,
                        320, 600L, 2));

                // --- tier 3 (plutonium coil): direct netherite + gem synthesis ---
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.ANCIENT_DEBRIS, 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(net.minecraft.world.item.Items.NETHERITE_INGOT, 1), GasStack.EMPTY, FluidStack.EMPTY,
                        520, 1_000L, 3));

                // --- tier 4 (antimatter coil): carbon compression ---
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.COAL_BLOCK, 8), GasStack.EMPTY, FluidStack.EMPTY,
                        item(net.minecraft.world.item.Items.DIAMOND, 1), GasStack.EMPTY, FluidStack.EMPTY,
                        600, 2_000L, 4));

                // --- tier 5 (graviton coil): gravitational compression ---
                // Nothing below a graviton coil can hold this heat: raw netherite scrap
                // is squeezed straight into a full ingot, no gold needed.
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.NETHERITE_SCRAP.asItem(), 4), GasStack.EMPTY, FluidStack.EMPTY,
                        item(net.minecraft.world.item.Items.NETHERITE_INGOT, 1), GasStack.EMPTY, FluidStack.EMPTY,
                        400, 8_000L, 5));
                // recycle spent graviton alloy back into neutronium
                list.add(new ChemRecipe(
                        item(MMMRegistry.GRAVITON_ALLOY.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.NEUTRONIUM.get(), 2), GasStack.EMPTY, FluidStack.EMPTY,
                        500, 20_000L, 5));

                // special steel chain smelting (tier 1)
                list.add(new ChemRecipe(
                        item(MMMRegistry.ALUMINA.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.ALUMINUM_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        240, 250L, 1));
                list.add(new ChemRecipe(
                        item(MMMRegistry.SPECIAL_STEEL_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.SPECIAL_STEEL_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        280, 400L, 2));

                // zinc: raw ore and dust both smelt to the ingot (tier 0, it melts easily)
                list.add(new ChemRecipe(
                        item(MMMRegistry.RAW_ZINC.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.ZINC_INGOT.get(), 2), GasStack.EMPTY, FluidStack.EMPTY,
                        200, 100L, 0));
                list.add(new ChemRecipe(
                        item(MMMRegistry.ZINC_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.ZINC_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        200, 100L, 0));
                // extra super duralumin (7075): the blended dust needs a hot, even soak
                list.add(new ChemRecipe(
                        item(MMMRegistry.DURALUMIN_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.DURALUMIN_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        300, 500L, 2));

                // platinum-group metal smelting (dust -> ingot; higher metals need hotter coils)
                list.add(new ChemRecipe(
                        item(MMMRegistry.PLATINUM_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.PLATINUM_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        240, 300L, 1));
                list.add(new ChemRecipe(
                        item(MMMRegistry.PALLADIUM_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.PALLADIUM_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        240, 300L, 1));
                list.add(new ChemRecipe(
                        item(MMMRegistry.RHODIUM_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.RHODIUM_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        280, 400L, 2));
                list.add(new ChemRecipe(
                        item(MMMRegistry.RUTHENIUM_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.RUTHENIUM_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        280, 400L, 2));
                list.add(new ChemRecipe(
                        item(MMMRegistry.IRIDIUM_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.IRIDIUM_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        400, 600L, 3));

                // --- naquadah line: hot-blast reduction of the sulfate/sulfide dusts ---
                // enriched naquadah sulfate + hydrogen -> enriched naquadah ingot + sulfuric acid
                list.add(new ChemRecipe(
                        item(MMMRegistry.ENRICHED_NAQUADAH_SULFATE.get(), 6), new GasStack(MekanismGases.HYDROGEN, 2_000L), FluidStack.EMPTY,
                        item(MMMRegistry.NAQUADAH_ENRICHED_INGOT.get(), 1), new GasStack(MekanismGases.SULFURIC_ACID, 1_000L), FluidStack.EMPTY,
                        500, 900L, 3));
                // naquadria sulfate + hydrogen -> naquadria ingot + sulfuric acid
                list.add(new ChemRecipe(
                        item(MMMRegistry.NAQUADRIA_SULFATE.get(), 6), new GasStack(MekanismGases.HYDROGEN, 2_000L), FluidStack.EMPTY,
                        item(MMMRegistry.NAQUADRIA_INGOT.get(), 1), new GasStack(MekanismGases.SULFURIC_ACID, 1_000L), FluidStack.EMPTY,
                        600, 1_200L, 4));
                // trinium sulfide -> trinium ingot
                list.add(new ChemRecipe(
                        item(MMMRegistry.TRINIUM_SULFIDE.get(), 2), new GasStack(MekanismGases.HYDROGEN, 1_000L), FluidStack.EMPTY,
                        item(MMMRegistry.TRINIUM_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        750, 1_000L, 3));
            }
            case REACTOR -> {
                // stage 2: titanium oxide + chlorine -> titanium tetrachloride
                list.add(new ChemRecipe(
                        item(MMMRegistry.TITANIUM_OXIDE.get(), 1), new GasStack(MekanismGases.CHLORINE, 200L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(ChemRegistry.TITANIUM_TETRACHLORIDE, 100L), FluidStack.EMPTY,
                        160, 400L));
                // stage 4: purified TiCl4 + liquid magnesium -> titanium sponge
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, new GasStack(ChemRegistry.PURIFIED_TITANIUM_TETRACHLORIDE, 100L), liquidMagnesium(144),
                        item(MMMRegistry.TITANIUM_SPONGE.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        180, 400L));
                // carbon monoxide from coal/charcoal + water
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.COAL, 1), GasStack.EMPTY, water(500),
                        ItemStack.EMPTY, new GasStack(ChemRegistry.CARBON_MONOXIDE, 1_000L), FluidStack.EMPTY,
                        100, 200L));
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.CHARCOAL, 1), GasStack.EMPTY, water(500),
                        ItemStack.EMPTY, new GasStack(ChemRegistry.CARBON_MONOXIDE, 1_000L), FluidStack.EMPTY,
                        100, 200L));
                // Mond process: fine nickel powder + CO -> nickel dust
                list.add(new ChemRecipe(
                        item(MMMRegistry.FINE_NICKEL_POWDER.get(), 1), new GasStack(ChemRegistry.CARBON_MONOXIDE, 200L), FluidStack.EMPTY,
                        item(MMMRegistry.NICKEL_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        120, 300L));

                // --- special steel chain ---
                // sodium hydroxide: Mekanism sodium gas (brine electrolysis) + water
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, new GasStack(MekanismGases.SODIUM, 500L), water(500),
                        ItemStack.EMPTY, new GasStack(ChemRegistry.SODIUM_HYDROXIDE, 500L), FluidStack.EMPTY,
                        100, 200L));
                // carbon dioxide from coal/charcoal + oxygen
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.COAL, 1), new GasStack(MekanismGases.OXYGEN, 500L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(ChemRegistry.CARBON_DIOXIDE, 1_000L), FluidStack.EMPTY,
                        100, 200L));
                list.add(new ChemRecipe(
                        item(net.minecraft.world.item.Items.CHARCOAL, 1), new GasStack(MekanismGases.OXYGEN, 500L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(ChemRegistry.CARBON_DIOXIDE, 1_000L), FluidStack.EMPTY,
                        100, 200L));
                // sodium carbonate: NaOH + CO2 (two-gas reaction)
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.SODIUM_HYDROXIDE, 500L), new GasStack(ChemRegistry.CARBON_DIOXIDE, 500L), FluidStack.EMPTY,
                        item(MMMRegistry.SODIUM_CARBONATE.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        120, 300L, 0));
                // sodium dichromate: enriched chromium ore + sodium carbonate
                list.add(new ChemRecipe(
                        item(MMMRegistry.ENRICHED_CHROMIUM_ORE.get(), 1), item(MMMRegistry.SODIUM_CARBONATE.get(), 1),
                        GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.SODIUM_DICHROMATE.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        140, 400L, 0));
                // sodium dichromate crystal: dichromate + sulfuric acid
                list.add(new ChemRecipe(
                        item(MMMRegistry.SODIUM_DICHROMATE.get(), 1), new GasStack(MekanismGases.SULFURIC_ACID, 200L), FluidStack.EMPTY,
                        item(MMMRegistry.SODIUM_DICHROMATE_CRYSTAL.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        140, 400L));
                // alumina: raw bauxite + sodium hydroxide (Bayer process)
                list.add(new ChemRecipe(
                        item(MMMRegistry.RAW_BAUXITE.get(), 1), new GasStack(ChemRegistry.SODIUM_HYDROXIDE, 200L), FluidStack.EMPTY,
                        item(MMMRegistry.ALUMINA.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        120, 300L));
                // chromium: aluminothermic reduction of dichromate crystal with aluminum dust
                list.add(new ChemRecipe(
                        item(MMMRegistry.SODIUM_DICHROMATE_CRYSTAL.get(), 1), item(MMMRegistry.ALUMINUM_DUST.get(), 1),
                        GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.CHROMIUM_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        200, 600L, 0));

                // --- platinum-group line (GTCEu platline) ---
                // nitric acid: saltpeter + sulfuric acid (classic HNO3 route)
                list.add(new ChemRecipe(
                        item(MMMRegistry.SALTPETER.get(), 1), new GasStack(MekanismGases.SULFURIC_ACID, 500L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(ChemRegistry.NITRIC_ACID, 500L), FluidStack.EMPTY,
                        100, 200L));
                // GTCEu: nitric acid washing of PGM-bearing ores -> platinum group sludge
                list.add(new ChemRecipe(
                        item(MMMRegistry.RAW_COOPERITE.get(), 1), new GasStack(ChemRegistry.NITRIC_ACID, 100L), FluidStack.EMPTY,
                        item(MMMRegistry.PLATINUM_GROUP_SLUDGE.get(), 4), GasStack.EMPTY, FluidStack.EMPTY,
                        50, 200L));
                list.add(new ChemRecipe(
                        item(MMMRegistry.RAW_NICKEL.get(), 1), new GasStack(ChemRegistry.NITRIC_ACID, 100L), FluidStack.EMPTY,
                        item(MMMRegistry.PLATINUM_GROUP_SLUDGE.get(), 2), GasStack.EMPTY, FluidStack.EMPTY,
                        50, 200L));
                // GTCEu: palladium raw + HCl -> palladium + ammonium chloride
                list.add(new ChemRecipe(
                        item(MMMRegistry.PALLADIUM_RAW.get(), 5), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(MekanismGases.HYDROGEN_CHLORIDE, 1_000L), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.PALLADIUM_DUST.get(), 1), item(MMMRegistry.AMMONIUM_CHLORIDE.get(), 2),
                        ItemStack.EMPTY, ItemStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        200, 300L, 0));
                // GTCEu: inert metal mixture + sulfuric acid -> ruthenium tetroxide + rhodium sulfate + hydrogen
                list.add(new ChemRecipe(
                        item(MMMRegistry.INERT_METAL_MIXTURE.get(), 6), new GasStack(MekanismGases.SULFURIC_ACID, 1_500L), FluidStack.EMPTY,
                        item(MMMRegistry.RUTHENIUM_TETROXIDE.get(), 5), new GasStack(MekanismGases.HYDROGEN, 3_000L), rhodiumSulfate(500),
                        450, 1_000L));
                // GTCEu: ruthenium tetroxide + carbon (coal) -> ruthenium + CO2
                list.add(new ChemRecipe(
                        item(MMMRegistry.RUTHENIUM_TETROXIDE.get(), 5), item(net.minecraft.world.item.Items.COAL, 2),
                        GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.RUTHENIUM_DUST.get(), 1), new GasStack(ChemRegistry.CARBON_DIOXIDE, 2_000L), FluidStack.EMPTY,
                        200, 300L, 0));
                // GTCEu (LCR): rarest metal mixture + HCl -> iridium metal residue + acidic osmium solution + hydrogen
                list.add(new ChemRecipe(
                        item(MMMRegistry.RAREST_METAL_MIXTURE.get(), 7), new GasStack(MekanismGases.HYDROGEN_CHLORIDE, 4_000L), FluidStack.EMPTY,
                        item(MMMRegistry.IRIDIUM_METAL_RESIDUE.get(), 5), new GasStack(MekanismGases.HYDROGEN, 3_000L), acidicOsmiumSolution(2_000),
                        400, 1_500L));
                // GTCEu: osmium tetroxide + hydrogen -> osmium (Mekanism's) + water
                list.add(new ChemRecipe(
                        item(MMMRegistry.OSMIUM_TETROXIDE.get(), 5), new GasStack(MekanismGases.HYDROGEN, 8_000L), FluidStack.EMPTY,
                        mekItem("dust_osmium", 1), GasStack.EMPTY, water(4_000),
                        200, 200L));
                // GTCEu: iridium chloride + hydrogen -> iridium + HCl back
                list.add(new ChemRecipe(
                        item(MMMRegistry.IRIDIUM_CHLORIDE.get(), 4), new GasStack(MekanismGases.HYDROGEN, 3_000L), FluidStack.EMPTY,
                        item(MMMRegistry.IRIDIUM_DUST.get(), 1), new GasStack(MekanismGases.HYDROGEN_CHLORIDE, 3_000L), FluidStack.EMPTY,
                        100, 200L));

                // --- naquadah line (GTCEu naquadah processing) ---
                // hydrofluoric acid: fluorite + sulfuric acid (bootstrap fluorine source)
                list.add(new ChemRecipe(
                        mekItem("fluorite_gem", 1), new GasStack(MekanismGases.SULFURIC_ACID, 1_000L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(ChemRegistry.HYDROFLUORIC_ACID, 1_000L), FluidStack.EMPTY,
                        100, 300L));
                // recycle byproduct fluorine back into HF (H2 + F2 -> 2 HF)
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.FLUORINE, 500L), new GasStack(MekanismGases.HYDROGEN, 500L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(ChemRegistry.HYDROFLUORIC_ACID, 1_000L), FluidStack.EMPTY,
                        80, 200L, 0));
                // antimony -> antimony trioxide
                list.add(new ChemRecipe(
                        item(MMMRegistry.ANTIMONY_DUST.get(), 2), new GasStack(MekanismGases.OXYGEN, 3_000L), FluidStack.EMPTY,
                        item(MMMRegistry.ANTIMONY_TRIOXIDE.get(), 5), GasStack.EMPTY, FluidStack.EMPTY,
                        60, 200L));
                // antimony trioxide + HF -> antimony trifluoride + water
                list.add(new ChemRecipe(
                        item(MMMRegistry.ANTIMONY_TRIOXIDE.get(), 5), new GasStack(ChemRegistry.HYDROFLUORIC_ACID, 6_000L), FluidStack.EMPTY,
                        item(MMMRegistry.ANTIMONY_TRIFLUORIDE.get(), 8), GasStack.EMPTY, water(3_000),
                        60, 300L));
                // antimony trifluoride + HF -> fluoroantimonic acid (super-acid) + hydrogen
                list.add(new ChemRecipe(
                        item(MMMRegistry.ANTIMONY_TRIFLUORIDE.get(), 4), new GasStack(ChemRegistry.HYDROFLUORIC_ACID, 4_000L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(MekanismGases.HYDROGEN, 2_000L), fluoroantimonicAcid(1_000),
                        300, 600L));
                // (LCR) fluoroantimonic acid dissolves naquadah -> the two impure solutions + titanium byproduct
                list.add(new ChemRecipe(
                        item(MMMRegistry.NAQUADAH_DUST.get(), 6), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        GasStack.EMPTY, GasStack.EMPTY, fluoroantimonicAcid(1_000),
                        item(MMMRegistry.TITANIUM_DUST.get(), 1), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.IMPURE_NAQUADRIA_SOLUTION, 2_000L), impureEnrichedNaquadah(2_000),
                        600, 2_500L, 0));
                // alternative polonium route (Mekanism's default is nuclear waste): irradiate
                // lead with oxygen into polonium gas. Requires the antimatter-forged polonium
                // synthesis module installed in the reactor, so it is a deliberate late-game gate.
                list.add(new ChemRecipe(
                        mekItem("ingot_lead", 2), new GasStack(MekanismGases.OXYGEN, 1_000L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(MekanismGases.POLONIUM, 100L), FluidStack.EMPTY,
                        200, 400L)
                        .requireUpgrade(new ItemStack(MMMRegistry.POLONIUM_SYNTHESIS_UPGRADE.get())));
                // --- matter replication line, stage 1: dissolution ---
                // Tear a rare artefact apart in aqua regia. The plasma still carries its
                // structure; the artefact itself is gone, so this is a one-off cost per
                // pattern, not a duplication step.
                record Dissolve(ItemStack rare, int plasma, int ticks,
                                net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> pattern) {
                }
                java.util.List<Dissolve> rares = new ArrayList<>();
                rares.add(new Dissolve(item(net.minecraft.world.item.Items.NETHER_STAR, 1), 2_000, 1_200,
                        MMMRegistry.PATTERN_NETHER_STAR));
                rares.add(new Dissolve(item(net.minecraft.world.item.Items.DRAGON_EGG, 1), 4_000, 2_400,
                        MMMRegistry.PATTERN_DRAGON_EGG));
                if (loaded("draconicevolution")) {
                    ItemStack shard = modItem("draconicevolution", "chaos_shard", 1);
                    if (!shard.isEmpty()) {
                        rares.add(new Dissolve(shard, 3_000, 1_800, MMMRegistry.PATTERN_CHAOS_SHARD));
                    }
                }
                if (loaded("botania")) {
                    ItemStack gaia = modItem("botania", "life_essence", 1);
                    if (!gaia.isEmpty()) {
                        rares.add(new Dissolve(gaia, 2_000, 1_200, MMMRegistry.PATTERN_GAIA_SPIRIT));
                    }
                }
                for (Dissolve d : rares) {
                    // The blank pattern is imprinted here, in the same step that destroys
                    // the artefact. Imprinting used to be a separate research-station step
                    // fed only by plasma, but every artefact yields the same plasma — so
                    // those recipes were indistinguishable and only the cheapest could ever
                    // run. Consuming the artefact itself keeps each recipe unique.
                    list.add(new ChemRecipe(
                            d.rare(), item(MMMRegistry.BLANK_MATTER_PATTERN.get(), 1), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                            new GasStack(ChemRegistry.AQUA_REGIA, 1_000L), GasStack.EMPTY, FluidStack.EMPTY,
                            item(d.pattern().get(), 1), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                            new GasStack(ChemRegistry.EXOTIC_PLASMA, d.plasma()), FluidStack.EMPTY,
                            d.ticks(), 20_000_000L, 0));
                }
                // Exotic plasma is the dissolution's other product: condensed with
                // antimatter it becomes replicator feedstock, so nothing is wasted.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.EXOTIC_PLASMA, 2_000L), new GasStack(MekanismGases.ANTIMATTER, 50L), FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, primordialMatter(1_000),
                        500, 300_000_000L, 0));

                // --- the stellar loop: ash is re-ignited into molten stellar matter ---
                // Every distillation and freeze sheds ash; four of them plus a little
                // helium plasma go back in, so the stellar line partly feeds itself.
                list.add(new ChemRecipe(
                        item(MMMRegistry.STELLAR_ASH.get(), 4), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.HELIUM_PLASMA, 500L), GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, moltenStellarMatter(300),
                        500, 100_000_000L, 0));

                // --- trans-dimensional metal, step 2 of 2 ---
                // The stabilizer now sheds singularity fragments instead of finished metal;
                // they only stabilise once dissolved in primordial matter, which links the
                // black-hole line into the replication line.
                list.add(new ChemRecipe(
                        item(MMMRegistry.SINGULARITY_FRAGMENT.get(), 3), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        GasStack.EMPTY, GasStack.EMPTY, primordialMatter(500),
                        item(MMMRegistry.TRANSDIMENSIONAL_METAL.get(), 1), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        GasStack.EMPTY, FluidStack.EMPTY,
                        900, 600_000_000L, 0)
                        .withChance(item(MMMRegistry.EXOTIC_RESIDUE.get(), 1), 30)
                        .withNote(stepNote("transdim", 2, 2, "done")));

                // --- stage 3: primordial matter, the replicator's feedstock ---
                // Antimatter annihilated against stellar matter, held as raw mass-energy.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(MekanismGases.ANTIMATTER, 100L), GasStack.EMPTY, moltenStellarMatter(500),
                        ItemStack.EMPTY, GasStack.EMPTY, primordialMatter(1_000),
                        600, 500_000_000L, 0));
                // --- the loop: exotic residue is dissolved back into feedstock ---
                // Each replication returns residue; four of them plus a little antimatter
                // rebuild a full bucket of matter, so a running line feeds itself.
                list.add(new ChemRecipe(
                        item(MMMRegistry.EXOTIC_RESIDUE.get(), 4), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(MekanismGases.ANTIMATTER, 20L), GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, primordialMatter(750),
                        400, 200_000_000L, 0));

                // GT desulfurization: hydrogen strips the sulfur out of the fuel fraction,
                // yielding clean diesel + a sulfur dust byproduct.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, new GasStack(MekanismGases.HYDROGEN, 200L), sulfuricFuel(1_000),
                        mekItem("dust_sulfur", 1), GasStack.EMPTY, diesel(850),
                        100, 2_000L));
            }
            case DISTILLATION -> {
                // stage 3: titanium tetrachloride -> purified titanium tetrachloride
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, new GasStack(ChemRegistry.TITANIUM_TETRACHLORIDE, 100L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(ChemRegistry.PURIFIED_TITANIUM_TETRACHLORIDE, 100L), FluidStack.EMPTY,
                        120, 250L));
                // GTCEu: acidic osmium solution -> osmium tetroxide + HCl + water
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, acidicOsmiumSolution(2_000),
                        item(MMMRegistry.OSMIUM_TETROXIDE.get(), 5), new GasStack(MekanismGases.HYDROGEN_CHLORIDE, 1_000L), water(1_000),
                        400, 300L));
                // Stellar core chain, step 2: boil molten stellar matter down to the
                // plasma the vacuum freezer can actually condense. Ash falls out here too.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, moltenStellarMatter(500),
                        ItemStack.EMPTY, new GasStack(ChemRegistry.STELLAR_PLASMA, 500L), FluidStack.EMPTY,
                        400, 40_000_000L)
                        .withChance(item(MMMRegistry.STELLAR_ASH.get(), 1), 40)
                        .withNote(stepNote("stellar", 2, 3, "freezer")));
                // GT oil processing: distill crude oil into the sulfur-laden fuel fraction
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, crudeOil(1_000),
                        ItemStack.EMPTY, GasStack.EMPTY, sulfuricFuel(700),
                        100, 2_000L));
            }
            case MIXER -> {
                // copper dust + nickel dust -> 2 cupronickel dust
                list.add(new ChemRecipe(
                        mekItem("dust_copper", 1), item(MMMRegistry.NICKEL_DUST.get(), 1),
                        GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.CUPRONICKEL_DUST.get(), 2), GasStack.EMPTY, FluidStack.EMPTY,
                        100, 200L, 0));
                // extra super duralumin (7075) = aluminium + zinc + magnesium + copper
                list.add(new ChemRecipe(
                        item(MMMRegistry.ALUMINUM_DUST.get(), 3), item(MMMRegistry.ZINC_DUST.get(), 1),
                        item(MMMRegistry.MAGNESIUM_DUST.get(), 1), mekItem("dust_copper", 1),
                        GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.DURALUMIN_DUST.get(), 4), GasStack.EMPTY, FluidStack.EMPTY,
                        160, 350L, 0));
                // chromium + iron + nickel dust -> 3 special steel dust
                list.add(new ChemRecipe(
                        item(MMMRegistry.CHROMIUM_DUST.get(), 1), mekItem("dust_iron", 1), item(MMMRegistry.NICKEL_DUST.get(), 1),
                        GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.SPECIAL_STEEL_DUST.get(), 3), GasStack.EMPTY, FluidStack.EMPTY,
                        140, 300L, 0));
                // GTCEu: aqua regia = nitric acid + hydrochloric acid (two-gas mix)
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.NITRIC_ACID, 1_000L), new GasStack(MekanismGases.HYDROGEN_CHLORIDE, 2_000L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(ChemRegistry.AQUA_REGIA, 3_000L), FluidStack.EMPTY,
                        60, 200L, 0));

                // --- naquadah line ---
                // osmiridium alloy: osmium (Mekanism) + iridium dust
                list.add(new ChemRecipe(
                        mekItem("dust_osmium", 3), item(MMMRegistry.IRIDIUM_DUST.get(), 1),
                        GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.OSMIRIDIUM_DUST.get(), 4), GasStack.EMPTY, FluidStack.EMPTY,
                        120, 300L, 0));
                // enriched naquadah solution + sulfuric acid -> acidic enriched naquadah solution (fluid + gas)
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(MekanismGases.SULFURIC_ACID, 2_000L), GasStack.EMPTY, enrichedNaquadah(1_000),
                        ItemStack.EMPTY, GasStack.EMPTY, acidicEnrichedNaquadah(3_000),
                        100, 400L, 0));
                // naquadria solution + sulfuric acid -> acidic naquadria solution (two-gas)
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.NAQUADRIA_SOLUTION, 1_000L), new GasStack(MekanismGases.SULFURIC_ACID, 2_000L), FluidStack.EMPTY,
                        ItemStack.EMPTY, new GasStack(ChemRegistry.ACIDIC_NAQUADRIA_SOLUTION, 3_000L), FluidStack.EMPTY,
                        100, 400L, 0));
            }
            case ALLOY_BLAST_FURNACE -> {
                // GT-style direct route: throw ALL the components in and get the molten
                // alloy straight away — no mixer pre-step needed.
                // Requires a plutonium coil (tier 3); higher tiers speed it up.
                list.add(new ChemRecipe(
                        item(MMMRegistry.SPECIAL_STEEL_DUST.get(), 1), item(MMMRegistry.CHROMIUM_DUST.get(), 1),
                        item(MMMRegistry.TITANIUM_DUST.get(), 1), mekItem("alloy_atomic", 2),
                        GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, moltenSuperAlloy(288),
                        600, 2_000L, 3));
                // naquadah alloy: naquadah + osmiridium + trinium -> molten naquadah alloy (blast temp 7200)
                // Requires an antimatter coil (tier 4).
                list.add(new ChemRecipe(
                        item(MMMRegistry.NAQUADAH_DUST.get(), 2), item(MMMRegistry.OSMIRIDIUM_DUST.get(), 1),
                        item(MMMRegistry.TRINIUM_INGOT.get(), 1), ItemStack.EMPTY,
                        GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, moltenNaquadahAlloy(144),
                        700, 3_000L, 4));
                // Neutronium chain, step 3 of 4: press the neutron-rich mass together.
                // The binder is a superconductor, NOT graviton alloy — graviton alloy is
                // made from neutronium, so using it here would close a crafting loop.
                // Antimatter coil (tier 4); 160,000,000 RF/t.
                list.add(new ChemRecipe(
                        item(MMMRegistry.NEUTRON_RICH_MASS.get(), 4), item(MMMRegistry.SUPERCONDUCTOR.get(), 1),
                        ItemStack.EMPTY, ItemStack.EMPTY,
                        GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, moltenNeutronium(288),
                        800, 400_000_000L, 4)
                        .withNote(stepNote("neutronium", 3, 4, "freezer")));
                // graviton alloy: neutronium compressed with a stellar core and
                // superconductor windings. Still an antimatter-coil (tier 4) job — this is
                // the material the tier-5 coil is made of, so it must not need one.
                // 200,000,000 RF/t = 500,000,000 J/t.
                list.add(new ChemRecipe(
                        item(MMMRegistry.NEUTRONIUM.get(), 2), item(MMMRegistry.STELLAR_CORE.get(), 1),
                        item(MMMRegistry.SUPERCONDUCTOR.get(), 2), ItemStack.EMPTY,
                        GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, moltenGravitonAlloy(144),
                        900, 500_000_000L, 4));
                // trans-dimensional alloy: alloy trans-dim metal + neutronium + naquadah
                // alloy. 300,000,000 RF/t = 750,000,000 J/t; needs the graviton coil (tier 5).
                list.add(new ChemRecipe(
                        item(MMMRegistry.TRANSDIMENSIONAL_METAL.get(), 2), item(MMMRegistry.NEUTRONIUM.get(), 1),
                        item(MMMRegistry.NAQUADAH_ALLOY_INGOT.get(), 1), ItemStack.EMPTY,
                        GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, moltenTransAlloy(144),
                        800, 750_000_000L, 5));
            }
            case VACUUM_FREEZER -> {
                // molten super alloy -> solid super alloy ingot (2 per 288 mB)
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, moltenSuperAlloy(288),
                        item(MMMRegistry.SUPER_ALLOY_INGOT.get(), 2), GasStack.EMPTY, FluidStack.EMPTY,
                        400, 800L, 0));
                // molten naquadah alloy -> solid naquadah alloy ingot
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, moltenNaquadahAlloy(144),
                        item(MMMRegistry.NAQUADAH_ALLOY_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        400, 1_000L, 0));
                // Stellar core chain, final step: freeze distilled stellar plasma into a
                // core. (Molten stellar matter can no longer be frozen directly — it must
                // be distilled first, see the distillation tower.) Some of the charge
                // always burns out as stellar ash, which the reactor recycles.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, new GasStack(ChemRegistry.STELLAR_PLASMA, 500L), FluidStack.EMPTY,
                        item(MMMRegistry.STELLAR_CORE.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        600, 20_000_000L, 0)
                        .withChance(item(MMMRegistry.STELLAR_ASH.get(), 1), 30)
                        .withNote(stepNote("stellar", 3, 3, "done")));
                // Neutronium chain, step 4 of 4: freeze it solid. Some of the mass sloughs
                // off as exotic residue, which the replication line turns back into feedstock.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, moltenNeutronium(288),
                        item(MMMRegistry.NEUTRONIUM.get(), 2), GasStack.EMPTY, FluidStack.EMPTY,
                        600, 200_000_000L, 0)
                        .withChance(item(MMMRegistry.EXOTIC_RESIDUE.get(), 1), 25)
                        .withNote(stepNote("neutronium", 4, 4, "done")));
                // molten graviton alloy -> solid graviton alloy ingot
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, moltenGravitonAlloy(144),
                        item(MMMRegistry.GRAVITON_ALLOY.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        700, 300_000_000L, 0));
                // molten trans-dimensional alloy -> solid alloy ingot (200,000,000 RF/t = 500,000,000 J/t)
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, moltenTransAlloy(144),
                        item(MMMRegistry.TRANSDIMENSIONAL_ALLOY.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        800, 500_000_000L, 0));
                // helium plasma (fusion product) condensed into liquid helium — the
                // annihilation generator's coolant.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, new GasStack(ChemRegistry.HELIUM_PLASMA, 200L), FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, liquidHelium(100),
                        100, 2_000L, 0));
            }
            case FUSION_REACTOR -> {
                // D-T fusion: deuterium + tritium -> helium plasma (needs Mekanism Generators).
                // The gases come from Mekanism Generators, so this recipe is only added when
                // that mod is present (looked up at runtime, no hard compile dependency).
                Gas deuterium = generatorsGas("deuterium");
                Gas tritium = generatorsGas("tritium");
                if (deuterium != null && tritium != null) {
                    list.add(new ChemRecipe(
                            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                            new GasStack(deuterium, 1_000L), new GasStack(tritium, 1_000L), FluidStack.EMPTY,
                            ItemStack.EMPTY, new GasStack(ChemRegistry.HELIUM_PLASMA, 1_000L), FluidStack.EMPTY,
                            200, 125_000L, 0));
                }
                // stellar fusion: helium plasma + antimatter -> molten stellar matter.
                // Endgame: draws 1,000,000,000 J/t = 400,000,000 RF/t.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.HELIUM_PLASMA, 1_000L), new GasStack(MekanismGases.ANTIMATTER, 10L), FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, moltenStellarMatter(100),
                        400, 1_000_000_000L, 0));
                // Neutronium chain, step 1 of 4: crush naquadria past its electron shell
                // into degenerate matter. (It is no longer a one-shot neutronium recipe —
                // the gas has to be centrifuged, pressed and frozen; see the centrifuge,
                // alloy blast furnace and vacuum freezer.)
                // With Draconic Evolution the fusion needs a draconium seed.
                ItemStack draconium = loaded("draconicevolution")
                        ? modItem("draconicevolution", "draconium_ingot", 4) : ItemStack.EMPTY;
                list.add(new ChemRecipe(
                        item(MMMRegistry.NAQUADRIA_INGOT.get(), 1), draconium, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.HELIUM_PLASMA, 1_000L), new GasStack(MekanismGases.ANTIMATTER, 50L), FluidStack.EMPTY,
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.DEGENERATE_MATTER, 1_000L), FluidStack.EMPTY,
                        600, 800_000_000L, 0)
                        .withNote(stepNote("neutronium", 1, 4, "centrifuge")));
            }
            case STABILIZER -> {
                // stabilize a black hole seed into 10 trans-dimensional metal.
                // 30 minutes (36000 ticks) at 2,500,000,000 J/t = 1,000,000,000 RF/t.
                // With Draconic Evolution the containment lattice consumes a chaos shard.
                ItemStack chaosShard = loaded("draconicevolution")
                        ? modItem("draconicevolution", "chaos_shard", 1) : ItemStack.EMPTY;
                // Trans-dimensional metal, step 1 of 2: the collapsing seed sheds
                // singularity fragments. They are not yet stable metal — the large
                // chemical reactor dissolves them in primordial matter to finish the job.
                list.add(new ChemRecipe(
                        item(MMMRegistry.BLACK_HOLE_SEED.get(), 1), chaosShard,
                        GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.SINGULARITY_FRAGMENT.get(), 36), GasStack.EMPTY, FluidStack.EMPTY,
                        36_000, 2_500_000_000L, 0)
                        .withChance(item(MMMRegistry.SINGULARITY_FRAGMENT.get(), 12), 25)
                        .withNote(stepNote("transdim", 1, 2, "reactor")));
            }
            case HADRON_COLLIDER -> {
                // Alternative antimatter route: collide hydrogen (protons) directly into
                // antimatter — no polonium/SPS needed. All rates are config-driven
                // (default 500 mB per 5s = 5 mB/t at 100M RF/t): keeps the
                // annihilation-generator loop strong without trivializing the SPS route.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(MekanismGases.HYDROGEN,
                                com.falcon2235.moremultiblock.MMMConfig.colliderHydrogenMbPerOp()),
                        GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY,
                        new GasStack(MekanismGases.ANTIMATTER,
                                com.falcon2235.moremultiblock.MMMConfig.colliderAntimatterMbPerOp()),
                        FluidStack.EMPTY,
                        100, com.falcon2235.moremultiblock.MMMConfig.colliderJPerTick(), 0));
                // ultimate craft: assemble a Mekanism creative energy cube from large amounts of
                // trans-dimensional circuits/alloy/metal. 500,000,000 RF/t = 1,250,000,000 J/t.
                ItemStack creativeCube = chargedCreativeCube();
                if (!creativeCube.isEmpty()) {
                    // With Botania / MEGA Cells installed, the final craft also demands
                    // Gaia spirits and a 256M cell component.
                    ItemStack gaia = loaded("botania") ? modItem("botania", "life_essence", 4) : ItemStack.EMPTY;
                    ItemStack megaTop = loaded("megacells") ? modItem("megacells", "cell_component_256m", 1) : ItemStack.EMPTY;
                    list.add(new ChemRecipe(
                            item(MMMRegistry.TRANSDIMENSIONAL_CIRCUIT.get(), 8), item(MMMRegistry.TRANSDIMENSIONAL_ALLOY.get(), 16),
                            item(MMMRegistry.TRANSDIMENSIONAL_METAL.get(), 16), gaia, megaTop,
                            GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                            creativeCube, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                            GasStack.EMPTY, FluidStack.EMPTY,
                            24_000, 1_250_000_000L, 0));
                }
            }
            case LARGE_INSCRIBER -> {
                // AE2 inscriber, 16x parallel. Printing steps need the matching AE2 press
                // installed in the module slot; assembly steps need no press.
                if (loaded("ae2")) {
                    record Print(String press, ItemStack in, String out) {
                    }
                    for (Print p : new Print[]{
                            new Print("silicon_press", modItem("ae2", "silicon", 16), "printed_silicon"),
                            new Print("logic_processor_press", item(net.minecraft.world.item.Items.GOLD_INGOT, 16), "printed_logic_processor"),
                            new Print("calculation_processor_press", modItem("ae2", "certus_quartz_crystal", 16), "printed_calculation_processor"),
                            new Print("engineering_processor_press", item(net.minecraft.world.item.Items.DIAMOND, 16), "printed_engineering_processor")}) {
                        ItemStack press = modItem("ae2", p.press(), 1);
                        ItemStack out = modItem("ae2", p.out(), 16);
                        if (!press.isEmpty() && !p.in().isEmpty() && !out.isEmpty()) {
                            list.add(withPressNote(new ChemRecipe(
                                    p.in(), GasStack.EMPTY, FluidStack.EMPTY,
                                    out, GasStack.EMPTY, FluidStack.EMPTY,
                                    80, 10_000L).requireUpgrade(press), press));
                        }
                    }
                    // processor assembly (print + redstone + printed silicon), no press
                    for (String kind : new String[]{"logic", "calculation", "engineering"}) {
                        ItemStack print = modItem("ae2", "printed_" + kind + "_processor", 16);
                        ItemStack silicon = modItem("ae2", "printed_silicon", 16);
                        ItemStack out = modItem("ae2", kind + "_processor", 16);
                        if (!print.isEmpty() && !silicon.isEmpty() && !out.isEmpty()) {
                            list.add(new ChemRecipe(
                                    print, item(net.minecraft.world.item.Items.REDSTONE, 16), silicon,
                                    GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                                    out, GasStack.EMPTY, FluidStack.EMPTY,
                                    80, 10_000L, 0));
                        }
                    }
                }
            }
            case LARGE_CHARGER -> {
                // AE2 charger, 16x parallel.
                if (loaded("ae2")) {
                    ItemStack certus = modItem("ae2", "certus_quartz_crystal", 16);
                    ItemStack charged = modItem("ae2", "charged_certus_quartz_crystal", 16);
                    if (!certus.isEmpty() && !charged.isEmpty()) {
                        list.add(new ChemRecipe(
                                certus, GasStack.EMPTY, FluidStack.EMPTY,
                                charged, GasStack.EMPTY, FluidStack.EMPTY,
                                40, 10_000L));
                    }
                }
                // charge this mod's superconductors (the assembler outputs them uncharged)
                list.add(new ChemRecipe(
                        item(MMMRegistry.UNCHARGED_SUPERCONDUCTOR.get(), 16), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.SUPERCONDUCTOR.get(), 16), GasStack.EMPTY, FluidStack.EMPTY,
                        60, 25_000L));
            }
            case RESEARCH_STATION -> {
                // GT-style scanning: a blank data orb + a sample of the finished part
                // -> a research-data module for the assembly line. 20,000 RF/t, 2 min.
                record Research(ItemStack sample, net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> data) {
                }
                // Each sample is a part the player can already build, so the research
                // always precedes the machine it unlocks (never the other way round).
                for (Research research : new Research[]{
                        new Research(item(MMMRegistry.SUPERCONDUCTOR.get(), 1), MMMRegistry.RESEARCH_DATA_SUPERCONDUCTOR),
                        new Research(item(MMMRegistry.FUSION_COIL.get().asItem(), 1), MMMRegistry.RESEARCH_DATA_FUSION),
                        new Research(item(MMMRegistry.VOID_DRILL.get().asItem(), 1), MMMRegistry.RESEARCH_DATA_VOID_MINING),
                        new Research(item(MMMRegistry.TRANSDIMENSIONAL_ALLOY.get(), 1), MMMRegistry.RESEARCH_DATA_TRANSDIMENSIONAL),
                        new Research(item(MMMRegistry.FROST_PROOF_CASING.get().asItem(), 1), MMMRegistry.RESEARCH_DATA_CRYOGENICS),
                        new Research(item(MMMRegistry.ALLOY_BLAST_CASING.get().asItem(), 1), MMMRegistry.RESEARCH_DATA_METALLURGY),
                        new Research(item(MMMRegistry.DRILL_PIPE.get().asItem(), 1), MMMRegistry.RESEARCH_DATA_PETROCHEMISTRY),
                        new Research(item(MMMRegistry.COLLIDER_MAGNET.get().asItem(), 1), MMMRegistry.RESEARCH_DATA_PARTICLE),
                        new Research(item(MMMRegistry.ANNIHILATION_CASING.get().asItem(), 1), MMMRegistry.RESEARCH_DATA_ANTIMATTER),
                        new Research(item(MMMRegistry.REPLICATOR_CASING.get().asItem(), 1), MMMRegistry.RESEARCH_DATA_REPLICATION),
                        new Research(item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 1), MMMRegistry.RESEARCH_DATA_DIGITAL),
                        new Research(item(MMMRegistry.LIVINGROCK_CASING.get().asItem(), 1), MMMRegistry.RESEARCH_DATA_ARCANE)}) {
                    list.add(new ChemRecipe(
                            item(MMMRegistry.DATA_ORB.get(), 1), research.sample(),
                            GasStack.EMPTY, FluidStack.EMPTY,
                            item(research.data().get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                            2_400, 50_000L, 0));
                }

                // (Pattern imprinting lives in the Large Chemical Reactor, together with
                // the dissolution that consumes the artefact — see REACTOR. Splitting it
                // out here made every imprint recipe look identical to the machine.)
            }
            case MATTER_REPLICATOR -> {
                // --- matter replication line, stage 4: growing the copy ---
                // The pattern sits in the module slot and is never consumed; primordial
                // matter is. Every run leaves exotic residue (which the line recycles),
                // and the copy itself is a chance roll — GT style.
                record Replicate(net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> pattern,
                                 ItemStack result, int chance, int mb, int ticks, long energy) {
                }
                java.util.List<Replicate> jobs = new ArrayList<>();
                jobs.add(new Replicate(MMMRegistry.PATTERN_NETHER_STAR,
                        item(net.minecraft.world.item.Items.NETHER_STAR, 1), 40, 1_000, 1_200, 400_000_000L));
                jobs.add(new Replicate(MMMRegistry.PATTERN_DRAGON_EGG,
                        item(net.minecraft.world.item.Items.DRAGON_EGG, 1), 20, 2_000, 2_400, 800_000_000L));
                if (loaded("draconicevolution")) {
                    ItemStack shard = modItem("draconicevolution", "chaos_shard", 1);
                    if (!shard.isEmpty()) {
                        jobs.add(new Replicate(MMMRegistry.PATTERN_CHAOS_SHARD, shard, 25, 1_500, 1_800, 600_000_000L));
                    }
                }
                if (loaded("botania")) {
                    ItemStack gaia = modItem("botania", "life_essence", 1);
                    if (!gaia.isEmpty()) {
                        jobs.add(new Replicate(MMMRegistry.PATTERN_GAIA_SPIRIT, gaia, 40, 1_000, 1_200, 400_000_000L));
                    }
                }
                for (Replicate j : jobs) {
                    ItemStack pattern = item(j.pattern().get(), 1);
                    list.add(new ChemRecipe(
                            ItemStack.EMPTY, GasStack.EMPTY, primordialMatter(j.mb()),
                            item(MMMRegistry.EXOTIC_RESIDUE.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                            j.ticks(), j.energy())
                            .requireUpgrade(pattern)
                            .withChance(j.result(), j.chance())
                            .withNote(net.minecraft.network.chat.Component.translatable(
                                    "gui." + com.falcon2235.moremultiblock.MekanismMoreMultiblock.MODID + ".replicate_req",
                                    pattern.getHoverName(), j.chance() + "%", j.result().getHoverName())));
                }
            }
            case ASSEMBLY_LINE -> {
                // GT assembly line: every recipe demands its research-data module.
                // Bulk superconductors (the assembler still makes singles without research).
                ItemStack elementiumBulk = loaded("botania") ? modItem("botania", "elementium_ingot", 4) : ItemStack.EMPTY;
                ItemStack scBulkOut = loaded("ae2") ? item(MMMRegistry.UNCHARGED_SUPERCONDUCTOR.get(), 8)
                        : item(MMMRegistry.SUPERCONDUCTOR.get(), 8);
                list.add(researchNote(new ChemRecipe(
                        item(MMMRegistry.NAQUADAH_ALLOY_INGOT.get(), 4), item(MMMRegistry.PLATINUM_INGOT.get(), 4),
                        item(MMMRegistry.IRIDIUM_INGOT.get(), 4),
                        elementiumBulk.isEmpty() ? item(MMMRegistry.RHODIUM_INGOT.get(), 4) : elementiumBulk,
                        item(MMMRegistry.NAQUADAH_ENRICHED_INGOT.get(), 4),
                        GasStack.EMPTY, GasStack.EMPTY, moltenSuperAlloy(576),
                        scBulkOut, GasStack.EMPTY, FluidStack.EMPTY,
                        600, 50_000L, 0), MMMRegistry.RESEARCH_DATA_SUPERCONDUCTOR));
                // fusion reactor controller (moved off the crafting grid)
                list.add(researchNote(new ChemRecipe(
                        item(MMMRegistry.SUPERCONDUCTOR.get(), 8), item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 4),
                        item(MMMRegistry.SPECIAL_STEEL_INGOT.get(), 16), item(MMMRegistry.FUSION_GLASS.get().asItem(), 4),
                        item(MMMRegistry.NAQUADAH_ALLOY_INGOT.get(), 8),
                        GasStack.EMPTY, GasStack.EMPTY, moltenSuperAlloy(1_152),
                        new ItemStack(MMMRegistry.CHEM_CONTROLLERS.get(ChemMachineType.FUSION_REACTOR).get()),
                        GasStack.EMPTY, FluidStack.EMPTY,
                        1_200, 100_000L, 0), MMMRegistry.RESEARCH_DATA_FUSION));
                // void ore miner controller (moved off the crafting grid)
                ItemStack voidGem = loaded("botania") ? modItem("botania", "dragonstone", 4) : ItemStack.EMPTY;
                list.add(researchNote(new ChemRecipe(
                        item(MMMRegistry.NAQUADAH_ALLOY_INGOT.get(), 8), item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 4),
                        item(MMMRegistry.VOID_MINER_CASING.get().asItem(), 2),
                        voidGem.isEmpty() ? item(net.minecraft.world.item.Items.DIAMOND, 8) : voidGem,
                        item(MMMRegistry.TITANIUM_INGOT.get(), 16),
                        GasStack.EMPTY, GasStack.EMPTY, moltenNaquadahAlloy(288),
                        new ItemStack(MMMRegistry.CHEM_CONTROLLERS.get(ChemMachineType.VOID_MINER).get()),
                        GasStack.EMPTY, FluidStack.EMPTY,
                        1_200, 100_000L, 0), MMMRegistry.RESEARCH_DATA_VOID_MINING));
                // trans-dimensional circuit (moved from the circuit assembler)
                ItemStack megaComponent = loaded("megacells") ? modItem("megacells", "cell_component_4m", 1) : ItemStack.EMPTY;
                list.add(researchNote(new ChemRecipe(
                        item(MMMRegistry.TRANSDIMENSIONAL_ALLOY.get(), 2), mekItem("ultimate_control_circuit", 2),
                        item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 2), item(MMMRegistry.TRANSDIMENSIONAL_METAL.get(), 1),
                        megaComponent.isEmpty() ? item(net.minecraft.world.item.Items.GOLD_INGOT, 2) : megaComponent,
                        GasStack.EMPTY, GasStack.EMPTY, moltenStellarMatter(144),
                        item(MMMRegistry.TRANSDIMENSIONAL_CIRCUIT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        600, 1_000_000_000L, 0), MMMRegistry.RESEARCH_DATA_TRANSDIMENSIONAL));

                // --- machine controllers ---
                // Most multiblock controllers are built here rather than on a crafting
                // grid. The exceptions are the foundation machines (blast furnace, reactor,
                // distillation, mixer, electrolyzer, centrifuge, ALLOY BLAST FURNACE and
                // VACUUM FREEZER) plus the circuit assembler / research station / assembly
                // line themselves: the alloy blast furnace and freezer produce the molten
                // super alloy every recipe here is soldered with, so gating them behind
                // this machine would close a loop nobody could enter.

                // Petrochemistry: the oil rig and the engine that burns what it pumps.
                list.add(controller(ChemMachineType.OIL_RIG,
                        item(MMMRegistry.OIL_RIG_CASING.get().asItem(), 4), item(MMMRegistry.DRILL_PIPE.get().asItem(), 4),
                        item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 2), item(MMMRegistry.DURALUMIN_INGOT.get(), 8),
                        item(MMMRegistry.SPECIAL_STEEL_INGOT.get(), 8),
                        moltenSuperAlloy(288), 600, 50_000L, MMMRegistry.RESEARCH_DATA_PETROCHEMISTRY));
                list.add(controller(ChemMachineType.COMBUSTION_GENERATOR,
                        item(MMMRegistry.ENGINE_CASING.get().asItem(), 4), item(MMMRegistry.HEAT_VENT.get().asItem(), 4),
                        item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 2), item(MMMRegistry.DURALUMIN_INGOT.get(), 8),
                        mekItem("alloy_atomic", 4),
                        moltenSuperAlloy(288), 600, 50_000L, MMMRegistry.RESEARCH_DATA_PETROCHEMISTRY));

                // Digital logic: the AE2 parallel rigs. Without AE2 the exotic component
                // falls back to plain super alloy so the machines stay buildable.
                ItemStack engProcessor = loaded("ae2") ? modItem("ae2", "engineering_processor", 8) : ItemStack.EMPTY;
                list.add(controller(ChemMachineType.LARGE_INSCRIBER,
                        item(MMMRegistry.INSCRIBER_CASING.get().asItem(), 4), item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 2),
                        engProcessor.isEmpty() ? item(MMMRegistry.SUPER_ALLOY_INGOT.get(), 8) : engProcessor,
                        item(MMMRegistry.TITANIUM_INGOT.get(), 8), item(MMMRegistry.SPECIAL_STEEL_INGOT.get(), 8),
                        moltenSuperAlloy(288), 600, 50_000L, MMMRegistry.RESEARCH_DATA_DIGITAL));
                ItemStack chargedCertus = loaded("ae2") ? modItem("ae2", "charged_certus_quartz_crystal", 16) : ItemStack.EMPTY;
                list.add(controller(ChemMachineType.LARGE_CHARGER,
                        item(MMMRegistry.CHARGER_CASING.get().asItem(), 4), item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 2),
                        chargedCertus.isEmpty() ? mekItem("alloy_atomic", 4) : chargedCertus,
                        item(MMMRegistry.SUPERCONDUCTOR.get(), 4), item(MMMRegistry.TITANIUM_INGOT.get(), 8),
                        moltenSuperAlloy(288), 600, 50_000L, MMMRegistry.RESEARCH_DATA_DIGITAL));

                // Particle physics: the three machines that bend space.
                list.add(controller(ChemMachineType.HADRON_COLLIDER,
                        item(MMMRegistry.ACCELERATOR_CASING.get().asItem(), 8), item(MMMRegistry.COLLIDER_MAGNET.get().asItem(), 4),
                        item(MMMRegistry.SUPERCONDUCTOR.get(), 8), item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 4),
                        item(MMMRegistry.NAQUADAH_ALLOY_INGOT.get(), 8),
                        moltenNaquadahAlloy(576), 1_200, 200_000L, MMMRegistry.RESEARCH_DATA_PARTICLE));
                list.add(controller(ChemMachineType.STAR_GENERATOR,
                        item(MMMRegistry.STAR_CASING.get().asItem(), 8), item(MMMRegistry.FUSION_COIL.get().asItem(), 4),
                        item(MMMRegistry.SUPERCONDUCTOR.get(), 8), item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 4),
                        item(MMMRegistry.NAQUADAH_ALLOY_INGOT.get(), 8),
                        moltenNaquadahAlloy(576), 1_200, 200_000L, MMMRegistry.RESEARCH_DATA_PARTICLE));
                list.add(controller(ChemMachineType.STABILIZER,
                        item(MMMRegistry.NEUTRONIUM_CASING.get().asItem(), 8), item(MMMRegistry.STABILIZER_GLASS.get().asItem(), 4),
                        item(MMMRegistry.SUPERCONDUCTOR.get(), 8), item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 4),
                        item(MMMRegistry.NEUTRONIUM.get(), 4),
                        moltenStellarMatter(288), 1_800, 1_000_000L, MMMRegistry.RESEARCH_DATA_PARTICLE));

                // Antimatter and replication: the two endgame rigs.
                list.add(controller(ChemMachineType.ANNIHILATION_GENERATOR,
                        item(MMMRegistry.ANNIHILATION_CASING.get().asItem(), 8), item(MMMRegistry.SUPERCONDUCTOR.get(), 16),
                        item(MMMRegistry.TRANSDIMENSIONAL_CIRCUIT.get(), 2), item(MMMRegistry.NEUTRONIUM.get(), 8),
                        item(MMMRegistry.STELLAR_CORE.get(), 2),
                        moltenStellarMatter(576), 2_400, 10_000_000L, MMMRegistry.RESEARCH_DATA_ANTIMATTER));
                list.add(controller(ChemMachineType.MATTER_REPLICATOR,
                        item(MMMRegistry.REPLICATOR_CASING.get().asItem(), 8), item(MMMRegistry.TRANSDIMENSIONAL_CIRCUIT.get(), 4),
                        item(MMMRegistry.GRAVITON_ALLOY.get(), 4), item(MMMRegistry.NEUTRONIUM.get(), 8),
                        item(MMMRegistry.SUPERCONDUCTOR.get(), 16),
                        moltenGravitonAlloy(576), 3_600, 50_000_000L, MMMRegistry.RESEARCH_DATA_REPLICATION));

                // Arcane engineering: the Botania / Ars parallel machines. Each also
                // demands mana, drawn from the assembly line's own mana hatches.
                if (loaded("botania")) {
                    ItemStack terrasteel = modItem("botania", "terrasteel_ingot", 4);
                    ItemStack elementium = modItem("botania", "elementium_ingot", 8);
                    ItemStack dragonstone = modItem("botania", "dragonstone", 2);
                    if (!terrasteel.isEmpty()) {
                        list.add(controller(ChemMachineType.GRAND_MANA_POOL,
                                item(MMMRegistry.LIVINGROCK_CASING.get().asItem(), 8), terrasteel.copy(),
                                item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 2), item(MMMRegistry.MANA_HATCH.get().asItem(), 2),
                                item(MMMRegistry.SUPER_ALLOY_INGOT.get(), 4),
                                moltenSuperAlloy(288), 800, 100_000L, MMMRegistry.RESEARCH_DATA_ARCANE));
                        list.add(controller(ChemMachineType.GRAND_TERRA_PLATE,
                                item(MMMRegistry.TERRA_PLATE_CASING.get().asItem(), 8), terrasteel.copyWithCount(8),
                                item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 4), item(MMMRegistry.MANA_HATCH.get().asItem(), 4),
                                item(MMMRegistry.SUPER_ALLOY_INGOT.get(), 8),
                                moltenSuperAlloy(576), 1_200, 200_000L, MMMRegistry.RESEARCH_DATA_ARCANE));
                    }
                    if (!elementium.isEmpty() && !dragonstone.isEmpty()) {
                        list.add(controller(ChemMachineType.GRAND_ELVEN_GATE,
                                item(MMMRegistry.ELVEN_GATE_CASING.get().asItem(), 8), elementium,
                                item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 2), item(MMMRegistry.MANA_HATCH.get().asItem(), 2),
                                dragonstone,
                                moltenSuperAlloy(288), 800, 100_000L, MMMRegistry.RESEARCH_DATA_ARCANE));
                    }
                }
                if (loaded("ars_nouveau")) {
                    ItemStack sourceGem = modItem("ars_nouveau", "source_gem", 16);
                    if (!sourceGem.isEmpty()) {
                        list.add(controller(ChemMachineType.GRAND_IMBUEMENT,
                                item(MMMRegistry.SOURCESTONE_CASING.get().asItem(), 8), sourceGem,
                                item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 2), item(MMMRegistry.SUPER_ALLOY_INGOT.get(), 4),
                                item(MMMRegistry.TITANIUM_INGOT.get(), 8),
                                moltenSuperAlloy(288), 800, 100_000L, MMMRegistry.RESEARCH_DATA_ARCANE));
                    }
                }
            }
            case GRAND_IMBUEMENT -> {
                // Ars Nouveau imbuement, 16x parallel — energy stands in for source.
                if (loaded("ars_nouveau")) {
                    ItemStack sourceGem16 = modItem("ars_nouveau", "source_gem", 16);
                    if (!sourceGem16.isEmpty()) {
                        list.add(new ChemRecipe(
                                item(net.minecraft.world.item.Items.LAPIS_LAZULI, 16), GasStack.EMPTY, FluidStack.EMPTY,
                                sourceGem16.copy(), GasStack.EMPTY, FluidStack.EMPTY,
                                100, 5_000L));
                        list.add(new ChemRecipe(
                                item(net.minecraft.world.item.Items.AMETHYST_SHARD, 16), GasStack.EMPTY, FluidStack.EMPTY,
                                sourceGem16.copy(), GasStack.EMPTY, FluidStack.EMPTY,
                                100, 5_000L));
                    }
                    // source-charging: the magical alternative to the AE2 charging route
                    list.add(new ChemRecipe(
                            item(MMMRegistry.UNCHARGED_SUPERCONDUCTOR.get(), 16), GasStack.EMPTY, FluidStack.EMPTY,
                            item(MMMRegistry.SUPERCONDUCTOR.get(), 16), GasStack.EMPTY, FluidStack.EMPTY,
                            60, 25_000L));
                }
            }
            case GRAND_MANA_POOL -> {
                // Botania mana infusion, 16x parallel. Mana comes from the structure's
                // mana hatches (spark them to pull from mana pools); energy runs the rig.
                if (loaded("botania")) {
                    record Infuse(ItemStack in, String out, int mana) {
                    }
                    for (Infuse i : new Infuse[]{
                            new Infuse(item(net.minecraft.world.item.Items.IRON_INGOT, 16), "manasteel_ingot", 48_000),
                            new Infuse(item(net.minecraft.world.item.Items.ENDER_PEARL, 16), "mana_pearl", 96_000),
                            new Infuse(item(net.minecraft.world.item.Items.DIAMOND, 16), "mana_diamond", 160_000)}) {
                        ItemStack out = modItem("botania", i.out(), 16);
                        if (!out.isEmpty()) {
                            list.add(new ChemRecipe(
                                    i.in(), GasStack.EMPTY, FluidStack.EMPTY,
                                    out, GasStack.EMPTY, FluidStack.EMPTY,
                                    100, 12_500L)
                                    .withMana(i.mana())
                                    .withNote(manaNote(i.mana())));
                        }
                    }
                }
            }
            case GRAND_ELVEN_GATE -> {
                // Botania elven trade, 16x parallel (vanilla ratios: 2 manasteel -> 1
                // elementium, 2 mana diamonds -> 1 dragonstone, pearls/wood 1:1).
                if (loaded("botania")) {
                    record Trade(String in, int inCount, String out) {
                    }
                    for (Trade t : new Trade[]{
                            new Trade("manasteel_ingot", 32, "elementium_ingot"),
                            new Trade("mana_diamond", 32, "dragonstone"),
                            new Trade("mana_pearl", 16, "pixie_dust"),
                            new Trade("livingwood_log", 16, "dreamwood_log")}) {
                        ItemStack in = modItem("botania", t.in(), t.inCount());
                        ItemStack out = modItem("botania", t.out(), 16);
                        if (!in.isEmpty() && !out.isEmpty()) {
                            list.add(new ChemRecipe(
                                    in, GasStack.EMPTY, FluidStack.EMPTY,
                                    out, GasStack.EMPTY, FluidStack.EMPTY,
                                    200, 12_500L)
                                    .withMana(20_000)
                                    .withNote(manaNote(20_000)));
                        }
                    }
                }
            }
            case GRAND_TERRA_PLATE -> {
                // Botania terrasteel infusion, 16x parallel: 500,000 mana per ingot
                // (8,000,000 per batch) plus a terra-plate-scale energy cost.
                if (loaded("botania")) {
                    ItemStack manasteel = modItem("botania", "manasteel_ingot", 16);
                    ItemStack pearl = modItem("botania", "mana_pearl", 16);
                    ItemStack diamond = modItem("botania", "mana_diamond", 16);
                    ItemStack terrasteel = modItem("botania", "terrasteel_ingot", 16);
                    if (!manasteel.isEmpty() && !pearl.isEmpty() && !diamond.isEmpty() && !terrasteel.isEmpty()) {
                        list.add(new ChemRecipe(
                                manasteel, pearl, diamond,
                                GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                                terrasteel, GasStack.EMPTY, FluidStack.EMPTY,
                                400, 250_000L, 0)
                                .withMana(8_000_000)
                                .withNote(manaNote(8_000_000)));
                    }
                }
            }
            case OIL_RIG -> {
                // Pumps crude oil up from bedrock (config: mB per second, RF/t).
                // A normal recipe — no inputs, runs forever.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY,
                        crudeOil(com.falcon2235.moremultiblock.MMMConfig.oilRigCrudeMbPerSecond()),
                        20, com.falcon2235.moremultiblock.MMMConfig.oilRigJPerTick()));
            }
            case COMBUSTION_GENERATOR -> {
                // JEI display only — the generator's real logic burns diesel every tick in
                // ChemMachineBlockEntity.combustionTick() and PRODUCES the shown energy.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY,
                        diesel(com.falcon2235.moremultiblock.MMMConfig.combustionDieselMbPerTick() * 20),
                        ItemStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        20, com.falcon2235.moremultiblock.MMMConfig.combustionJPerTick())
                        .withNote(net.minecraft.network.chat.Component.translatable(
                                "gui." + com.falcon2235.moremultiblock.MekanismMoreMultiblock.MODID + ".generates",
                                String.format(java.util.Locale.ROOT, "%,d",
                                        com.falcon2235.moremultiblock.MMMConfig.combustionRfPerTick()))));
            }
            case ANNIHILATION_GENERATOR -> {
                // JEI display only — the generator's real logic annihilates every tick in
                // ChemMachineBlockEntity.annihilationTick() and PRODUCES the shown energy.
                // Slot amounts show one second's worth of each input.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(MekanismGases.HYDROGEN,
                                com.falcon2235.moremultiblock.MMMConfig.annihilationHydrogenMbPerTick() * 20L),
                        new GasStack(MekanismGases.ANTIMATTER,
                                com.falcon2235.moremultiblock.MMMConfig.annihilationAntimatterMbPerTick() * 20L),
                        liquidHelium(com.falcon2235.moremultiblock.MMMConfig.annihilationHeliumMbPerTick() * 20),
                        ItemStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        20, com.falcon2235.moremultiblock.MMMConfig.annihilationJPerTick(), 0)
                        .withNote(net.minecraft.network.chat.Component.translatable(
                                "gui." + com.falcon2235.moremultiblock.MekanismMoreMultiblock.MODID + ".generates_annihilation",
                                String.format(java.util.Locale.ROOT, "%,d",
                                        com.falcon2235.moremultiblock.MMMConfig.annihilationRfPerTick()))));
            }
            case VOID_MINER -> {
                // JEI display only — the miner's real logic rolls the weighted table every
                // tick in ChemMachineBlockEntity.voidMinerTick(). One entry per ore, with
                // its roll chance drawn as a note.
                int total = VoidOreTable.totalWeight();
                for (VoidOreTable.Entry entry : VoidOreTable.entries()) {
                    String pct = String.format(java.util.Locale.ROOT, "%.1f%%", 100.0 * entry.weight() / total);
                    list.add(new ChemRecipe(
                            ItemStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                            entry.stack().copyWithCount(VoidOreTable.oresPerRoll()), GasStack.EMPTY, FluidStack.EMPTY,
                            VoidOreTable.rollIntervalTicks(), VoidOreTable.energyPerTick(), 0)
                            .withNote(net.minecraft.network.chat.Component.translatable(
                                    "gui." + com.falcon2235.moremultiblock.MekanismMoreMultiblock.MODID + ".void_chance",
                                    pct, VoidOreTable.oresPerRoll())));
                }
            }
            case STAR_GENERATOR -> {
                // Compress a stellar core + a full charge of hydrogen into a black hole seed.
                // 10 minutes (12000 ticks) at 250,000,000 J/t = 100,000,000 RF/t.
                // With Draconic Evolution the collapse also needs an awakened core.
                ItemStack awakenedCore = loaded("draconicevolution")
                        ? modItem("draconicevolution", "awakened_core", 1) : ItemStack.EMPTY;
                list.add(new ChemRecipe(
                        item(MMMRegistry.STELLAR_CORE.get(), 1), awakenedCore,
                        new GasStack(MekanismGases.HYDROGEN, 64_000L), FluidStack.EMPTY,
                        item(MMMRegistry.BLACK_HOLE_SEED.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        12_000, 250_000_000L, 0));
            }
            case CIRCUIT_ASSEMBLER -> {
                // supreme control circuit: 5 components + molten super alloy solder.
                // With Botania installed the gold is replaced by mana-infused manasteel.
                ItemStack manasteel = loaded("botania") ? modItem("botania", "manasteel_ingot", 2) : ItemStack.EMPTY;
                list.add(new ChemRecipe(
                        mekItem("ultimate_control_circuit", 2), item(MMMRegistry.SUPER_ALLOY_INGOT.get(), 1),
                        mekItem("alloy_atomic", 2),
                        manasteel.isEmpty() ? item(net.minecraft.world.item.Items.GOLD_INGOT, 2) : manasteel,
                        item(net.minecraft.world.item.Items.REDSTONE, 4),
                        GasStack.EMPTY, GasStack.EMPTY, moltenSuperAlloy(144),
                        item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        400, 10_000L, 0));
                // superconductor (GregTech-style): exotic-metal windings assembled with molten
                // super-alloy solder. With Botania the rhodium becomes elven elementium; with
                // AE2 the assembler outputs UNCHARGED coils that must be charged (AE2 charger
                // or the large charger) before use.
                ItemStack elementium = loaded("botania") ? modItem("botania", "elementium_ingot", 1) : ItemStack.EMPTY;
                ItemStack scOutput = loaded("ae2") && !item(MMMRegistry.UNCHARGED_SUPERCONDUCTOR.get(), 1).isEmpty()
                        ? item(MMMRegistry.UNCHARGED_SUPERCONDUCTOR.get(), 2)
                        : item(MMMRegistry.SUPERCONDUCTOR.get(), 2);
                list.add(new ChemRecipe(
                        item(MMMRegistry.NAQUADAH_ALLOY_INGOT.get(), 1), item(MMMRegistry.PLATINUM_INGOT.get(), 1),
                        item(MMMRegistry.IRIDIUM_INGOT.get(), 1),
                        elementium.isEmpty() ? item(MMMRegistry.RHODIUM_INGOT.get(), 1) : elementium,
                        item(MMMRegistry.NAQUADAH_ENRICHED_INGOT.get(), 1),
                        GasStack.EMPTY, GasStack.EMPTY, moltenSuperAlloy(144),
                        scOutput, GasStack.EMPTY, FluidStack.EMPTY,
                        400, 25_000L, 0));
                // (the trans-dimensional circuit moved to the ASSEMBLY_LINE — it now
                // needs trans-dimensional research data installed there)
            }
            case ELECTROLYZER -> {
                // GTCEu: platinum raw powder -> platinum + chlorine
                list.add(new ChemRecipe(
                        item(MMMRegistry.PLATINUM_RAW.get(), 3), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.PLATINUM_DUST.get(), 1), new GasStack(MekanismGases.CHLORINE, 800L), FluidStack.EMPTY,
                        100, 300L));
                // GTCEu: rhodium sulfate solution -> rhodium + sulfur trioxide
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, rhodiumSulfate(1_000),
                        item(MMMRegistry.RHODIUM_DUST.get(), 2), new GasStack(MekanismGases.SULFUR_TRIOXIDE, 3_000L), FluidStack.EMPTY,
                        100, 300L));
            }
            case CENTRIFUGE -> {
                // Neutronium chain, step 2 of 4: spin the degenerate gas until the
                // neutron-rich fraction settles out. The rest burns off as stellar ash,
                // which the stellar loop reclaims.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, new GasStack(ChemRegistry.DEGENERATE_MATTER, 1_000L), FluidStack.EMPTY,
                        item(MMMRegistry.NEUTRON_RICH_MASS.get(), 2), GasStack.EMPTY, FluidStack.EMPTY,
                        400, 40_000_000L)
                        .withChance(item(MMMRegistry.STELLAR_ASH.get(), 1), 35)
                        .withNote(stepNote("neutronium", 2, 4, "alloy_blast")));
                // GTCEu: platinum group sludge + aqua regia -> the four raw metal fractions
                list.add(new ChemRecipe(
                        item(MMMRegistry.PLATINUM_GROUP_SLUDGE.get(), 6), ItemStack.EMPTY, ItemStack.EMPTY,
                        ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.AQUA_REGIA, 1_200L), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.PLATINUM_RAW.get(), 3), item(MMMRegistry.PALLADIUM_RAW.get(), 3),
                        item(MMMRegistry.INERT_METAL_MIXTURE.get(), 2), item(MMMRegistry.RAREST_METAL_MIXTURE.get(), 1),
                        GasStack.EMPTY, FluidStack.EMPTY,
                        500, 600L, 0));
                // GTCEu: iridium metal residue -> iridium chloride
                list.add(new ChemRecipe(
                        item(MMMRegistry.IRIDIUM_METAL_RESIDUE.get(), 5), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.IRIDIUM_CHLORIDE.get(), 4), GasStack.EMPTY, FluidStack.EMPTY,
                        200, 300L));

                // --- naquadah line ---
                // impure enriched naquadah solution -> trinium sulfide + antimony trifluoride + enriched solution
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        GasStack.EMPTY, GasStack.EMPTY, impureEnrichedNaquadah(2_000),
                        item(MMMRegistry.TRINIUM_SULFIDE.get(), 1), item(MMMRegistry.ANTIMONY_TRIFLUORIDE.get(), 2), ItemStack.EMPTY, ItemStack.EMPTY,
                        GasStack.EMPTY, enrichedNaquadah(1_000),
                        400, 800L, 0));
                // impure naquadria solution (gas) -> antimony trifluoride + naquadria solution (gas)
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.IMPURE_NAQUADRIA_SOLUTION, 2_000L), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.ANTIMONY_TRIFLUORIDE.get(), 2), new GasStack(ChemRegistry.NAQUADRIA_SOLUTION, 1_000L), FluidStack.EMPTY,
                        400, 800L, 0));
                // acidic enriched naquadah solution -> fluorine + enriched naquadah sulfate
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        GasStack.EMPTY, GasStack.EMPTY, acidicEnrichedNaquadah(3_000),
                        item(MMMRegistry.ENRICHED_NAQUADAH_SULFATE.get(), 6), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.FLUORINE, 500L), FluidStack.EMPTY,
                        100, 600L, 0));
                // acidic naquadria solution (gas) -> fluorine + naquadria sulfate
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.ACIDIC_NAQUADRIA_SOLUTION, 3_000L), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.NAQUADRIA_SULFATE.get(), 6), new GasStack(ChemRegistry.FLUORINE, 500L), FluidStack.EMPTY,
                        100, 600L, 0));
            }
        }
        return List.copyOf(list);
    }

    private static ItemStack item(net.minecraft.world.item.Item item, int count) {
        return new ItemStack(item, count);
    }

    /** Looks up a Mekanism Generators gas by name at runtime, or null when the mod is absent. */
    private static Gas generatorsGas(String name) {
        Gas gas = MekanismAPI.gasRegistry().getValue(new ResourceLocation("mekanismgenerators", name));
        return gas == null || gas.isEmptyType() ? null : gas;
    }

    /**
     * A Mekanism creative energy cube filled to maximum energy, so the produced cube
     * supplies power immediately (a fresh one is empty and outputs nothing).
     */
    private static ItemStack chargedCreativeCube() {
        ItemStack cube = mekItem("creative_energy_cube", 1);
        if (!cube.isEmpty()) {
            cube.getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY).ifPresent(handler -> {
                for (int i = 0; i < handler.getEnergyContainerCount(); i++) {
                    handler.setEnergy(i, handler.getMaxEnergy(i));
                }
            });
        }
        return cube;
    }

    private static ItemStack mekItem(String path, int count) {
        return modItem("mekanism", path, count);
    }

    /** Runtime item lookup for optional/soft-referenced mods (AE2, Botania, MEGA Cells). */
    private static ItemStack modItem(String namespace, String path, int count) {
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new net.minecraft.resources.ResourceLocation(namespace, path));
        return item == null || item == net.minecraft.world.item.Items.AIR
                ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static boolean loaded(String modid) {
        return net.minecraftforge.fml.ModList.get().isLoaded(modid);
    }

    /** JEI note naming the press module a large-inscriber recipe needs. */
    private static ChemRecipe withPressNote(ChemRecipe recipe, ItemStack press) {
        return recipe.withNote(net.minecraft.network.chat.Component.translatable(
                "gui." + com.falcon2235.moremultiblock.MekanismMoreMultiblock.MODID + ".press_req",
                press.getHoverName()));
    }

    /**
     * An assembly-line recipe that builds one multiblock controller: five component
     * inputs soldered with a molten alloy, gated behind its research-data module.
     */
    private static ChemRecipe controller(ChemMachineType machine,
            ItemStack a, ItemStack b, ItemStack c, ItemStack d, ItemStack e,
            FluidStack solder, int ticks, long energyPerTick,
            net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> data) {
        return researchNote(new ChemRecipe(
                a, b, c, d, e,
                GasStack.EMPTY, GasStack.EMPTY, solder,
                new ItemStack(MMMRegistry.CHEM_CONTROLLERS.get(machine).get()), GasStack.EMPTY, FluidStack.EMPTY,
                ticks, energyPerTick, 0), data);
    }

    /** Marks an assembly-line recipe as requiring the given research-data module. */
    private static ChemRecipe researchNote(ChemRecipe recipe,
            net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> data) {
        ItemStack module = item(data.get(), 1);
        return recipe.requireUpgrade(module)
                .withNote(net.minecraft.network.chat.Component.translatable(
                        "gui." + com.falcon2235.moremultiblock.MekanismMoreMultiblock.MODID + ".research_req",
                        module.getHoverName()));
    }

    /**
     * JEI note marking a recipe's place in one of the deep end-game chains, so a player
     * looking at an unfamiliar intermediate can see what it belongs to and what comes next.
     */
    private static net.minecraft.network.chat.Component stepNote(String chainKey, int step, int total, String nextKey) {
        String base = "gui." + com.falcon2235.moremultiblock.MekanismMoreMultiblock.MODID + ".";
        return net.minecraft.network.chat.Component.translatable(base + "chain_step",
                net.minecraft.network.chat.Component.translatable(base + "chain." + chainKey),
                step, total,
                net.minecraft.network.chat.Component.translatable(base + "chain_next." + nextKey));
    }

    /** JEI note showing a recipe's Botania mana cost. */
    private static net.minecraft.network.chat.Component manaNote(int mana) {
        return net.minecraft.network.chat.Component.translatable(
                "gui." + com.falcon2235.moremultiblock.MekanismMoreMultiblock.MODID + ".mana_req",
                String.format(java.util.Locale.ROOT, "%,d", mana));
    }

    private static FluidStack water(int amount) {
        return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, amount);
    }

    private static FluidStack liquidMagnesium(int amount) {
        return new FluidStack(ChemRegistry.LIQUID_MAGNESIUM.getStillFluid(), amount);
    }

    private static FluidStack crudeOil(int amount) {
        return new FluidStack(ChemRegistry.CRUDE_OIL.getStillFluid(), amount);
    }

    private static FluidStack sulfuricFuel(int amount) {
        return new FluidStack(ChemRegistry.SULFURIC_FUEL.getStillFluid(), amount);
    }

    /** Public: the combustion generator's block entity checks its fuel tank against this. */
    public static FluidStack diesel(int amount) {
        return new FluidStack(ChemRegistry.DIESEL.getStillFluid(), amount);
    }

    /** Public: the annihilation generator's block entity checks its coolant tank against this. */
    public static FluidStack liquidHelium(int amount) {
        return new FluidStack(ChemRegistry.LIQUID_HELIUM.getStillFluid(), amount);
    }

    private static FluidStack moltenSuperAlloy(int amount) {
        return new FluidStack(ChemRegistry.MOLTEN_SUPER_ALLOY.getStillFluid(), amount);
    }

    private static FluidStack rhodiumSulfate(int amount) {
        return new FluidStack(ChemRegistry.RHODIUM_SULFATE.getStillFluid(), amount);
    }

    private static FluidStack acidicOsmiumSolution(int amount) {
        return new FluidStack(ChemRegistry.ACIDIC_OSMIUM_SOLUTION.getStillFluid(), amount);
    }

    private static FluidStack fluoroantimonicAcid(int amount) {
        return new FluidStack(ChemRegistry.FLUOROANTIMONIC_ACID.getStillFluid(), amount);
    }

    private static FluidStack impureEnrichedNaquadah(int amount) {
        return new FluidStack(ChemRegistry.IMPURE_ENRICHED_NAQUADAH_SOLUTION.getStillFluid(), amount);
    }

    private static FluidStack enrichedNaquadah(int amount) {
        return new FluidStack(ChemRegistry.ENRICHED_NAQUADAH_SOLUTION.getStillFluid(), amount);
    }

    private static FluidStack acidicEnrichedNaquadah(int amount) {
        return new FluidStack(ChemRegistry.ACIDIC_ENRICHED_NAQUADAH_SOLUTION.getStillFluid(), amount);
    }

    private static FluidStack moltenNaquadahAlloy(int amount) {
        return new FluidStack(ChemRegistry.MOLTEN_NAQUADAH_ALLOY.getStillFluid(), amount);
    }

    private static FluidStack moltenGravitonAlloy(int amount) {
        return new FluidStack(ChemRegistry.MOLTEN_GRAVITON_ALLOY.getStillFluid(), amount);
    }

    private static FluidStack primordialMatter(int amount) {
        return new FluidStack(ChemRegistry.PRIMORDIAL_MATTER.getStillFluid(), amount);
    }

    private static FluidStack moltenNeutronium(int amount) {
        return new FluidStack(ChemRegistry.MOLTEN_NEUTRONIUM.getStillFluid(), amount);
    }

    private static FluidStack moltenStellarMatter(int amount) {
        return new FluidStack(ChemRegistry.MOLTEN_STELLAR_MATTER.getStillFluid(), amount);
    }

    private static FluidStack moltenTransAlloy(int amount) {
        return new FluidStack(ChemRegistry.MOLTEN_TRANSDIMENSIONAL_ALLOY.getStillFluid(), amount);
    }
}
