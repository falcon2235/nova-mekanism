package com.chihaya.moremultiblock.machine;

import com.chihaya.moremultiblock.MMMRegistry;
import com.chihaya.moremultiblock.content.ChemRegistry;

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

    private static List<ChemRecipe> build(ChemMachineType type) {
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

                // special steel chain smelting (tier 1)
                list.add(new ChemRecipe(
                        item(MMMRegistry.ALUMINA.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.ALUMINUM_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        240, 250L, 1));
                list.add(new ChemRecipe(
                        item(MMMRegistry.SPECIAL_STEEL_DUST.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.SPECIAL_STEEL_INGOT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        280, 400L, 2));

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
            }
            case MIXER -> {
                // copper dust + nickel dust -> 2 cupronickel dust
                list.add(new ChemRecipe(
                        mekItem("dust_copper", 1), item(MMMRegistry.NICKEL_DUST.get(), 1),
                        GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.CUPRONICKEL_DUST.get(), 2), GasStack.EMPTY, FluidStack.EMPTY,
                        100, 200L, 0));
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
                // trans-dimensional alloy: alloy trans-dim metal + neutronium + naquadah alloy.
                // 300,000,000 RF/t = 750,000,000 J/t; antimatter coil.
                list.add(new ChemRecipe(
                        item(MMMRegistry.TRANSDIMENSIONAL_METAL.get(), 2), item(MMMRegistry.NEUTRONIUM.get(), 1),
                        item(MMMRegistry.NAQUADAH_ALLOY_INGOT.get(), 1), ItemStack.EMPTY,
                        GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, moltenTransAlloy(144),
                        800, 750_000_000L, 4));
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
                // molten stellar matter (fusion output) -> solid stellar core
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, moltenStellarMatter(100),
                        item(MMMRegistry.STELLAR_CORE.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        600, 2_000L, 0));
                // molten trans-dimensional alloy -> solid alloy ingot (200,000,000 RF/t = 500,000,000 J/t)
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, GasStack.EMPTY, moltenTransAlloy(144),
                        item(MMMRegistry.TRANSDIMENSIONAL_ALLOY.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        800, 500_000_000L, 0));
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
                            200, 20_000L, 0));
                }
                // stellar fusion: helium plasma + antimatter -> molten stellar matter.
                // Endgame: draws 1,000,000,000 J/t = 400,000,000 RF/t.
                list.add(new ChemRecipe(
                        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.HELIUM_PLASMA, 1_000L), new GasStack(MekanismGases.ANTIMATTER, 10L), FluidStack.EMPTY,
                        ItemStack.EMPTY, GasStack.EMPTY, moltenStellarMatter(100),
                        400, 1_000_000_000L, 0));
                // neutronium (GT: naquadria fusion): fuse naquadria with helium plasma + antimatter
                list.add(new ChemRecipe(
                        item(MMMRegistry.NAQUADRIA_INGOT.get(), 1), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        new GasStack(ChemRegistry.HELIUM_PLASMA, 1_000L), new GasStack(MekanismGases.ANTIMATTER, 50L), FluidStack.EMPTY,
                        item(MMMRegistry.NEUTRONIUM.get(), 4), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                        GasStack.EMPTY, FluidStack.EMPTY,
                        600, 800_000_000L, 0));
            }
            case STABILIZER -> {
                // stabilize a black hole seed into 10 trans-dimensional metal.
                // 30 minutes (36000 ticks) at 2,500,000,000 J/t = 1,000,000,000 RF/t.
                list.add(new ChemRecipe(
                        item(MMMRegistry.BLACK_HOLE_SEED.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        item(MMMRegistry.TRANSDIMENSIONAL_METAL.get(), 10), GasStack.EMPTY, FluidStack.EMPTY,
                        36_000, 2_500_000_000L, 0));
                // ultimate craft: assemble a Mekanism creative energy cube from large amounts of
                // trans-dimensional circuits/alloy/metal. 500,000,000 RF/t = 1,250,000,000 J/t.
                ItemStack creativeCube = chargedCreativeCube();
                if (!creativeCube.isEmpty()) {
                    list.add(new ChemRecipe(
                            item(MMMRegistry.TRANSDIMENSIONAL_CIRCUIT.get(), 8), item(MMMRegistry.TRANSDIMENSIONAL_ALLOY.get(), 16),
                            item(MMMRegistry.TRANSDIMENSIONAL_METAL.get(), 16), ItemStack.EMPTY, ItemStack.EMPTY,
                            GasStack.EMPTY, GasStack.EMPTY, FluidStack.EMPTY,
                            creativeCube, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                            GasStack.EMPTY, FluidStack.EMPTY,
                            24_000, 1_250_000_000L, 0));
                }
            }
            case STAR_GENERATOR -> {
                // Compress a stellar core + a full charge of hydrogen into a black hole seed.
                // 10 minutes (12000 ticks) at 250,000,000 J/t = 100,000,000 RF/t.
                list.add(new ChemRecipe(
                        item(MMMRegistry.STELLAR_CORE.get(), 1), new GasStack(MekanismGases.HYDROGEN, 64_000L), FluidStack.EMPTY,
                        item(MMMRegistry.BLACK_HOLE_SEED.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        12_000, 250_000_000L, 0));
            }
            case CIRCUIT_ASSEMBLER -> {
                // supreme control circuit: 5 components + molten super alloy solder
                // (above Mekanism's ultimate control circuit)
                list.add(new ChemRecipe(
                        mekItem("ultimate_control_circuit", 2), item(MMMRegistry.SUPER_ALLOY_INGOT.get(), 1),
                        mekItem("alloy_atomic", 2), item(net.minecraft.world.item.Items.GOLD_INGOT, 2),
                        item(net.minecraft.world.item.Items.REDSTONE, 4),
                        GasStack.EMPTY, GasStack.EMPTY, moltenSuperAlloy(144),
                        item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        400, 1_500L, 0));
                // superconductor (GregTech-style): exotic-metal windings assembled with molten
                // super-alloy solder, used to build the fusion coils and fusion casing.
                list.add(new ChemRecipe(
                        item(MMMRegistry.NAQUADAH_ALLOY_INGOT.get(), 1), item(MMMRegistry.PLATINUM_INGOT.get(), 1),
                        item(MMMRegistry.IRIDIUM_INGOT.get(), 1), item(MMMRegistry.RHODIUM_INGOT.get(), 1),
                        item(MMMRegistry.NAQUADAH_ENRICHED_INGOT.get(), 1),
                        GasStack.EMPTY, GasStack.EMPTY, moltenSuperAlloy(144),
                        item(MMMRegistry.SUPERCONDUCTOR.get(), 2), GasStack.EMPTY, FluidStack.EMPTY,
                        400, 2_000L, 0));
                // trans-dimensional circuit: alloy + supreme circuit + metal, soldered with
                // molten stellar matter. 400,000,000 RF/t = 1,000,000,000 J/t.
                list.add(new ChemRecipe(
                        item(MMMRegistry.TRANSDIMENSIONAL_ALLOY.get(), 2), mekItem("ultimate_control_circuit", 2),
                        item(MMMRegistry.SUPREME_CONTROL_CIRCUIT.get(), 2), item(MMMRegistry.TRANSDIMENSIONAL_METAL.get(), 1),
                        item(net.minecraft.world.item.Items.GOLD_INGOT, 2),
                        GasStack.EMPTY, GasStack.EMPTY, moltenStellarMatter(144),
                        item(MMMRegistry.TRANSDIMENSIONAL_CIRCUIT.get(), 1), GasStack.EMPTY, FluidStack.EMPTY,
                        600, 1_000_000_000L, 0));
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
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new net.minecraft.resources.ResourceLocation("mekanism", path));
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static FluidStack water(int amount) {
        return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, amount);
    }

    private static FluidStack liquidMagnesium(int amount) {
        return new FluidStack(ChemRegistry.LIQUID_MAGNESIUM.getStillFluid(), amount);
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

    private static FluidStack moltenStellarMatter(int amount) {
        return new FluidStack(ChemRegistry.MOLTEN_STELLAR_MATTER.getStillFluid(), amount);
    }

    private static FluidStack moltenTransAlloy(int amount) {
        return new FluidStack(ChemRegistry.MOLTEN_TRANSDIMENSIONAL_ALLOY.getStillFluid(), amount);
    }
}
