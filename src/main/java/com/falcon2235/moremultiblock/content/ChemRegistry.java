package com.falcon2235.moremultiblock.content;

import com.falcon2235.moremultiblock.MekanismMoreMultiblock;

import mekanism.api.chemical.gas.Gas;
import mekanism.common.registration.impl.FluidDeferredRegister;
import mekanism.common.registration.impl.FluidRegistryObject;
import mekanism.common.registration.impl.GasDeferredRegister;
import mekanism.common.registration.impl.GasRegistryObject;

import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Custom Mekanism chemicals and fluids used by the titanium production chain:
 * titanium tetrachloride gas (before/after distillation) and molten (liquid)
 * magnesium. Chlorine reuses Mekanism's own {@code mekanism:chlorine} gas.
 */
public final class ChemRegistry {

    public static final GasDeferredRegister GASES = new GasDeferredRegister(MekanismMoreMultiblock.MODID);
    public static final FluidDeferredRegister FLUIDS = new FluidDeferredRegister(MekanismMoreMultiblock.MODID);

    /** TiCl4 produced by chlorinating titanium oxide (stage 2). */
    public static final GasRegistryObject<Gas> TITANIUM_TETRACHLORIDE =
            GASES.register("titanium_tetrachloride", 0xFFB8A66A);

    /** Distilled/purified TiCl4 (stage 3), fed into the magnesium reduction. */
    public static final GasRegistryObject<Gas> PURIFIED_TITANIUM_TETRACHLORIDE =
            GASES.register("purified_titanium_tetrachloride", 0xFFEADFA6);

    /** CO from coal + water in the reactor; refines fine nickel powder (Mond process). */
    public static final GasRegistryObject<Gas> CARBON_MONOXIDE =
            GASES.register("carbon_monoxide", 0xFF9AA3AB);

    /** NaOH from sodium (brine electrolysis) + water; Bayer process + soda ash feedstock. */
    public static final GasRegistryObject<Gas> SODIUM_HYDROXIDE =
            GASES.register("sodium_hydroxide", 0xFFE8E4D4);

    /** CO2 from coal/charcoal + oxygen; reacts with NaOH into sodium carbonate. */
    public static final GasRegistryObject<Gas> CARBON_DIOXIDE =
            GASES.register("carbon_dioxide", 0xFFBFC7CC);

    /** Molten magnesium, made by melting magnesium dust in the blast furnace (stage 4 reductant). */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> LIQUID_MAGNESIUM =
            FLUIDS.register("liquid_magnesium", props -> props.tint(0xFFCBCBD6));

    /**
     * Molten super alloy: the hot output of the alloy blast furnace. The vacuum freezer
     * chills it back into a solid super-alloy ingot (GT alloy-smelter → freezer flow).
     */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> MOLTEN_SUPER_ALLOY =
            FLUIDS.register("molten_super_alloy", props -> props.tint(0xFFFF9A3C));

    // --- platinum-group line (GTCEu platline) ---

    /** HNO3 from saltpeter + sulfuric acid; dissolves PGM-bearing ores into sludge. */
    public static final GasRegistryObject<Gas> NITRIC_ACID =
            GASES.register("nitric_acid", 0xFFE8D26A);

    /** Aqua regia (HNO3 + 2 HCl in the mixer); leaches the platinum group sludge. */
    public static final GasRegistryObject<Gas> AQUA_REGIA =
            GASES.register("aqua_regia", 0xFFE07A2C);

    /** Rhodium sulfate solution from the inert metal mixture; electrolyzed into rhodium. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> RHODIUM_SULFATE =
            FLUIDS.register("rhodium_sulfate", props -> props.tint(0xFFC44536));

    /** Acidic osmium solution from the rarest metal mixture; distilled into osmium tetroxide. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> ACIDIC_OSMIUM_SOLUTION =
            FLUIDS.register("acidic_osmium_solution", props -> props.tint(0xFF4A6A8A));

    // --- naquadah line (GTCEu naquadah processing) ---
    // Gases: fluorine chemistry + the naquadria-side solutions.
    public static final GasRegistryObject<Gas> HYDROFLUORIC_ACID =
            GASES.register("hydrofluoric_acid", 0xFFCFE060);
    public static final GasRegistryObject<Gas> FLUORINE =
            GASES.register("fluorine", 0xFFE8E86A);
    public static final GasRegistryObject<Gas> IMPURE_NAQUADRIA_SOLUTION =
            GASES.register("impure_naquadria_solution", 0xFF4A2E5A);
    public static final GasRegistryObject<Gas> NAQUADRIA_SOLUTION =
            GASES.register("naquadria_solution", 0xFF6A3A8A);
    public static final GasRegistryObject<Gas> ACIDIC_NAQUADRIA_SOLUTION =
            GASES.register("acidic_naquadria_solution", 0xFF8A4EAE);

    // Fluids: fluoroantimonic solvent + the enriched-naquadah-side solutions + molten alloy.
    public static final FluidRegistryObject<?, ?, ?, ?, ?> FLUOROANTIMONIC_ACID =
            FLUIDS.register("fluoroantimonic_acid", props -> props.tint(0xFFB8C24A));
    public static final FluidRegistryObject<?, ?, ?, ?, ?> IMPURE_ENRICHED_NAQUADAH_SOLUTION =
            FLUIDS.register("impure_enriched_naquadah_solution", props -> props.tint(0xFF2E5A50));
    public static final FluidRegistryObject<?, ?, ?, ?, ?> ENRICHED_NAQUADAH_SOLUTION =
            FLUIDS.register("enriched_naquadah_solution", props -> props.tint(0xFF3A7A5E));
    public static final FluidRegistryObject<?, ?, ?, ?, ?> ACIDIC_ENRICHED_NAQUADAH_SOLUTION =
            FLUIDS.register("acidic_enriched_naquadah_solution", props -> props.tint(0xFF4E9E6A));

    /** Molten naquadah alloy: alloy blast furnace output, frozen into the solid ingot. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> MOLTEN_NAQUADAH_ALLOY =
            FLUIDS.register("molten_naquadah_alloy", props -> props.tint(0xFF4C7A50));

    // --- petroleum line (GT-style oil processing) ---
    /** Crude oil pumped up by the oil drilling rig; distilled into sulfuric fuel. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> CRUDE_OIL =
            FLUIDS.register("crude_oil", props -> props.tint(0xFF17130E));

    /** Sulfur-laden fuel fraction from crude-oil distillation; desulfurized into diesel. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> SULFURIC_FUEL =
            FLUIDS.register("sulfuric_fuel", props -> props.tint(0xFF8A7A26));

    /** Diesel: desulfurized fuel, burned by the large combustion generator. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> DIESEL =
            FLUIDS.register("diesel", props -> props.tint(0xFFD8B04A));

    // --- fusion reactor ---
    /** Helium plasma: first fusion product (hydrogen + lithium), fuel for stellar fusion. */
    public static final GasRegistryObject<Gas> HELIUM_PLASMA =
            GASES.register("helium_plasma", 0xFFFFD27A);

    /** Molten stellar matter: the fusion reactor's pinnacle output, frozen into a stellar core. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> MOLTEN_STELLAR_MATTER =
            FLUIDS.register("molten_stellar_matter", props -> props.tint(0xFFB0E8FF));

    /** Liquid helium: helium plasma condensed in the vacuum freezer; annihilation coolant. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> LIQUID_HELIUM =
            FLUIDS.register("liquid_helium", props -> props.tint(0xFFC8ECF8));

    // --- deep end-game chains ---
    /**
     * Degenerate matter: the fusion reactor's raw naquadria product, matter crushed
     * past the electron shell. Centrifuged into neutron-rich mass.
     */
    public static final GasRegistryObject<Gas> DEGENERATE_MATTER =
            GASES.register("degenerate_matter", 0xFF7A88A8);

    /** Stellar plasma: molten stellar matter distilled down before it can be frozen into a core. */
    public static final GasRegistryObject<Gas> STELLAR_PLASMA =
            GASES.register("stellar_plasma", 0xFFFFE0A0);

    /** Molten neutronium: neutron-rich mass pressed together; freezes into the solid metal. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> MOLTEN_NEUTRONIUM =
            FLUIDS.register("molten_neutronium", props -> props.tint(0xFFDCDCE4));

    // --- matter replication line ---
    /**
     * Exotic plasma: what is left when a rare artefact is torn apart in the chemical
     * reactor. It still carries the item's structure, which the research station
     * imprints onto a blank pattern.
     */
    public static final GasRegistryObject<Gas> EXOTIC_PLASMA =
            GASES.register("exotic_plasma", 0xFFE85AF0);

    /**
     * Primordial matter: undifferentiated mass-energy, the replicator's feedstock.
     * Condensed from antimatter and stellar matter, or recycled from exotic residue.
     */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> PRIMORDIAL_MATTER =
            FLUIDS.register("primordial_matter", props -> props.tint(0xFFF0E8B0));

    /** Molten graviton alloy: neutronium + stellar matter, frozen into the tier-5 coil material. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> MOLTEN_GRAVITON_ALLOY =
            FLUIDS.register("molten_graviton_alloy", props -> props.tint(0xFF6EE0FF));

    /** Molten trans-dimensional alloy: alloy blast furnace output, frozen into the alloy ingot. */
    public static final FluidRegistryObject<?, ?, ?, ?, ?> MOLTEN_TRANSDIMENSIONAL_ALLOY =
            FLUIDS.register("molten_transdimensional_alloy", props -> props.tint(0xFFC060E0));

    private ChemRegistry() {
    }

    public static void register(IEventBus bus) {
        GASES.register(bus);
        FLUIDS.register(bus);
    }
}
