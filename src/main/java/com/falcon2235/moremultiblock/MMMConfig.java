package com.falcon2235.moremultiblock;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * All balance knobs of Nova Mekanism, exposed as a standard Forge COMMON config
 * (config/mekanism_more_multiblock-common.toml). Values are read lazily: the custom
 * machine loops read them every tick, and the hardcoded recipe set is rebuilt when
 * the config (re)loads. Energy values are configured in RF/t (the mod's machines
 * internally run on Joules; 1 RF = 2.5 J).
 *
 * <p>Every getter falls back to its default when called before the config file has
 * loaded, so early recipe/JEI access can never crash.
 */
public final class MMMConfig {

    public static final ForgeConfigSpec SPEC;

    // --- void ore miner ---
    private static final ForgeConfigSpec.LongValue VOID_MINER_RF_PER_TICK;
    private static final ForgeConfigSpec.IntValue VOID_MINER_ROLL_INTERVAL_TICKS;
    private static final ForgeConfigSpec.IntValue VOID_MINER_ORES_PER_ROLL;

    // --- oil drilling rig ---
    private static final ForgeConfigSpec.LongValue OIL_RIG_RF_PER_TICK;
    private static final ForgeConfigSpec.IntValue OIL_RIG_CRUDE_MB_PER_SECOND;

    // --- large combustion generator ---
    private static final ForgeConfigSpec.LongValue COMBUSTION_RF_PER_TICK;
    private static final ForgeConfigSpec.IntValue COMBUSTION_DIESEL_MB_PER_TICK;

    // --- annihilation generator ---
    private static final ForgeConfigSpec.LongValue ANNIHILATION_RF_PER_TICK;
    private static final ForgeConfigSpec.IntValue ANNIHILATION_HYDROGEN_MB_PER_TICK;
    private static final ForgeConfigSpec.IntValue ANNIHILATION_ANTIMATTER_MB_PER_TICK;
    private static final ForgeConfigSpec.IntValue ANNIHILATION_HELIUM_MB_PER_TICK;

    // --- large hadron collider ---
    private static final ForgeConfigSpec.LongValue COLLIDER_RF_PER_TICK;
    private static final ForgeConfigSpec.IntValue COLLIDER_HYDROGEN_MB_PER_OP;
    private static final ForgeConfigSpec.IntValue COLLIDER_ANTIMATTER_MB_PER_OP;

    // --- quantum conduits ---
    private static final ForgeConfigSpec.LongValue CONDUIT_ENERGY_RF_PER_TICK;
    private static final ForgeConfigSpec.IntValue CONDUIT_FLUID_MB_PER_TICK;
    private static final ForgeConfigSpec.LongValue CONDUIT_GAS_MB_PER_TICK;
    private static final ForgeConfigSpec.IntValue CONDUIT_ITEMS_PER_PULL;

    // --- modpack integration switches ---
    private static final ForgeConfigSpec.BooleanValue HARDEN_MEKANISM_RECIPES;
    private static final java.util.Map<String, ForgeConfigSpec.BooleanValue> ORE_GENERATION =
            new java.util.LinkedHashMap<>();

    /** Ores this mod can generate; pack authors disable the ones their pack already covers. */
    public static final java.util.List<String> ORE_NAMES = java.util.List.of(
            "titanium", "magnesium", "nickel", "chromium", "bauxite", "zinc",
            "cooperite", "saltpeter", "antimony", "naquadah");

    // --- global balance multipliers ---
    private static final ForgeConfigSpec.DoubleValue RECIPE_ENERGY_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue RECIPE_TIME_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Void Ore Miner (7x9x7 drill rig)").push("void_ore_miner");
        VOID_MINER_RF_PER_TICK = b
                .comment("Energy drawn per working tick, in RF/t.")
                .defineInRange("energyRfPerTick", 1_000_000L, 1L, 1_000_000_000_000L);
        VOID_MINER_ROLL_INTERVAL_TICKS = b
                .comment("Base ticks between mining rolls (20 = one roll per second).",
                        "Mekanism speed upgrades shorten this further.")
                .defineInRange("rollIntervalTicks", 20, 1, 1_200);
        VOID_MINER_ORES_PER_ROLL = b
                .comment("How many raw ores of the rolled type each roll produces.")
                .defineInRange("oresPerRoll", 10, 1, 64);
        b.pop();

        b.comment("Oil Drilling Rig (5x7x5 rig)").push("oil_drilling_rig");
        OIL_RIG_RF_PER_TICK = b
                .comment("Energy drawn per working tick, in RF/t.")
                .defineInRange("energyRfPerTick", 4_000L, 1L, 1_000_000_000L);
        OIL_RIG_CRUDE_MB_PER_SECOND = b
                .comment("Crude oil pumped per second (mB). 200 = 10 mB/t.")
                .defineInRange("crudeOilMbPerSecond", 200, 1, 64_000);
        b.pop();

        b.comment("Large Combustion Generator (3x3x4 engine)").push("combustion_generator");
        COMBUSTION_RF_PER_TICK = b
                .comment("Energy generated per tick while burning, in RF/t.")
                .defineInRange("outputRfPerTick", 500_000L, 1L, 1_000_000_000_000L);
        COMBUSTION_DIESEL_MB_PER_TICK = b
                .comment("Diesel burned per tick (mB).")
                .defineInRange("dieselMbPerTick", 20, 1, 64_000);
        b.pop();

        b.comment("Annihilation Generator (7x7x7 containment sphere)").push("annihilation_generator");
        ANNIHILATION_RF_PER_TICK = b
                .comment("Energy generated per tick while running, in RF/t.")
                .defineInRange("outputRfPerTick", 800_000_000L, 1L, 1_000_000_000_000L);
        ANNIHILATION_HYDROGEN_MB_PER_TICK = b
                .comment("Hydrogen annihilated per tick (mB). 0 disables this input.")
                .defineInRange("hydrogenMbPerTick", 50, 0, 64_000);
        ANNIHILATION_ANTIMATTER_MB_PER_TICK = b
                .comment("Antimatter annihilated per tick (mB). 0 disables this input.")
                .defineInRange("antimatterMbPerTick", 1, 0, 64_000);
        ANNIHILATION_HELIUM_MB_PER_TICK = b
                .comment("Liquid-helium coolant consumed per tick (mB). 0 disables this input.")
                .defineInRange("liquidHeliumMbPerTick", 10, 0, 64_000);
        b.pop();

        b.comment("Large Hadron Collider (33x3x33 accelerator ring)").push("hadron_collider");
        COLLIDER_RF_PER_TICK = b
                .comment("Energy drawn per working tick, in RF/t.")
                .defineInRange("energyRfPerTick", 100_000_000L, 1L, 1_000_000_000_000L);
        COLLIDER_HYDROGEN_MB_PER_OP = b
                .comment("Hydrogen consumed per 5-second operation (mB).")
                .defineInRange("hydrogenMbPerOperation", 8_000, 1, 64_000);
        COLLIDER_ANTIMATTER_MB_PER_OP = b
                .comment("Antimatter produced per 5-second operation (mB). 500 = 5 mB/t.")
                .defineInRange("antimatterMbPerOperation", 500, 1, 64_000);
        b.pop();

        b.comment("Switches for modpack authors: world generation and the changes this",
                        "mod makes to other mods' recipes.")
                .push("integration");
        HARDEN_MEKANISM_RECIPES = b
                .comment("Apply this mod's high-priority data pack, which hardens some Mekanism",
                        "recipes (SPS, fission reactor casing, enriched iron, gas-burning",
                        "generator, digital miner). Set false to leave Mekanism untouched.",
                        "Takes effect on game restart.")
                .define("hardenMekanismRecipes", true);
        b.comment("Per-ore world generation. Disable the ores your pack already covers",
                        "with another mod. Takes effect for newly generated chunks.")
                .push("ore_generation");
        for (String ore : ORE_NAMES) {
            ORE_GENERATION.put(ore, b.define(ore, true));
        }
        b.pop();
        b.pop();

        b.comment("Quantum conduits (the transmitter tier above Mekanism's ultimate).",
                        "These caps apply to extraction-mode pulls per face per tick;",
                        "resources pushed INTO a conduit are always routed uncapped.")
                .push("quantum_conduit");
        CONDUIT_ENERGY_RF_PER_TICK = b
                .comment("Max RF pulled per extract face per tick.")
                .defineInRange("energyRfPerTick", 2_147_483_647L, 1L, 2_147_483_647L);
        CONDUIT_FLUID_MB_PER_TICK = b
                .comment("Max fluid pulled per extract face per tick (mB).")
                .defineInRange("fluidMbPerTick", 1_024_000, 1, 2_000_000_000);
        CONDUIT_GAS_MB_PER_TICK = b
                .comment("Max gas pulled per extract face per tick (mB).")
                .defineInRange("gasMbPerTick", 1_024_000L, 1L, 1_000_000_000_000L);
        CONDUIT_ITEMS_PER_PULL = b
                .comment("Max items pulled per extract face per tick.")
                .defineInRange("itemsPerPull", 64, 1, 64);
        b.pop();

        b.comment("Global balance multipliers for every hardcoded multiblock recipe",
                        "(does not affect the generator machines or the void miner, whose",
                        "rates are set in their own sections above).")
                .push("balance");
        RECIPE_ENERGY_MULTIPLIER = b
                .comment("Multiplies the energy cost of every multiblock recipe.")
                .defineInRange("recipeEnergyMultiplier", 1.0D, 0.05D, 100.0D);
        RECIPE_TIME_MULTIPLIER = b
                .comment("Multiplies the duration of every multiblock recipe.")
                .defineInRange("recipeTimeMultiplier", 1.0D, 0.05D, 100.0D);
        b.pop();

        SPEC = b.build();
    }

    private MMMConfig() {
    }

    // --- RF-facing getters (for display) ---

    public static long voidMinerRfPerTick() {
        return get(VOID_MINER_RF_PER_TICK, 1_000_000L);
    }

    public static long combustionRfPerTick() {
        return get(COMBUSTION_RF_PER_TICK, 500_000L);
    }

    public static long annihilationRfPerTick() {
        return get(ANNIHILATION_RF_PER_TICK, 800_000_000L);
    }

    // --- Joule-facing getters (machines run on J; 1 RF = 2.5 J) ---

    public static long voidMinerJPerTick() {
        return rfToJoules(voidMinerRfPerTick());
    }

    public static long oilRigJPerTick() {
        return rfToJoules(get(OIL_RIG_RF_PER_TICK, 4_000L));
    }

    public static long combustionJPerTick() {
        return rfToJoules(combustionRfPerTick());
    }

    public static long annihilationJPerTick() {
        return rfToJoules(annihilationRfPerTick());
    }

    public static long colliderJPerTick() {
        return rfToJoules(get(COLLIDER_RF_PER_TICK, 100_000_000L));
    }

    // --- amounts ---

    public static int voidMinerRollIntervalTicks() {
        return get(VOID_MINER_ROLL_INTERVAL_TICKS, 20);
    }

    public static int voidMinerOresPerRoll() {
        return get(VOID_MINER_ORES_PER_ROLL, 10);
    }

    public static int oilRigCrudeMbPerSecond() {
        return get(OIL_RIG_CRUDE_MB_PER_SECOND, 200);
    }

    public static int combustionDieselMbPerTick() {
        return get(COMBUSTION_DIESEL_MB_PER_TICK, 20);
    }

    public static int annihilationHydrogenMbPerTick() {
        return get(ANNIHILATION_HYDROGEN_MB_PER_TICK, 50);
    }

    public static int annihilationAntimatterMbPerTick() {
        return get(ANNIHILATION_ANTIMATTER_MB_PER_TICK, 1);
    }

    public static int annihilationHeliumMbPerTick() {
        return get(ANNIHILATION_HELIUM_MB_PER_TICK, 10);
    }

    public static int colliderHydrogenMbPerOp() {
        return get(COLLIDER_HYDROGEN_MB_PER_OP, 8_000);
    }

    public static int colliderAntimatterMbPerOp() {
        return get(COLLIDER_ANTIMATTER_MB_PER_OP, 500);
    }

    public static int conduitEnergyRfPerTick() {
        return (int) Math.min(Integer.MAX_VALUE, get(CONDUIT_ENERGY_RF_PER_TICK, 2_147_483_647L));
    }

    public static int conduitFluidMbPerTick() {
        return get(CONDUIT_FLUID_MB_PER_TICK, 1_024_000);
    }

    public static long conduitGasMbPerTick() {
        return get(CONDUIT_GAS_MB_PER_TICK, 1_024_000L);
    }

    public static int conduitItemsPerPull() {
        return get(CONDUIT_ITEMS_PER_PULL, 64);
    }

    /** Whether the bundled override pack (which nerfs some Mekanism recipes) is applied. */
    public static boolean hardenMekanismRecipes() {
        return get(HARDEN_MEKANISM_RECIPES, Boolean.TRUE);
    }

    /** Whether the named ore should be placed by world generation. */
    public static boolean generateOre(String ore) {
        ForgeConfigSpec.BooleanValue value = ORE_GENERATION.get(ore);
        return value == null || get(value, Boolean.TRUE);
    }

    public static double recipeEnergyMultiplier() {
        return get(RECIPE_ENERGY_MULTIPLIER, 1.0D);
    }

    public static double recipeTimeMultiplier() {
        return get(RECIPE_TIME_MULTIPLIER, 1.0D);
    }

    private static long rfToJoules(long rf) {
        return rf * 5L / 2L;
    }

    /** Reads a config value, falling back to the default before the config has loaded. */
    private static <T> T get(ForgeConfigSpec.ConfigValue<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException | NullPointerException e) {
            return fallback;
        }
    }
}
