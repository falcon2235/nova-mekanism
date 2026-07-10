package com.falcon2235.moremultiblock.machine;

/**
 * The GregTech-inspired chemical multiblocks. Each is either a hollow casing box or a
 * vertical coil tower of the given (odd width, odd height, depth); the controller is
 * the centre block of the front face.
 */
public enum ChemMachineType {
    /**
     * Electric blast furnace: ore→oxide, sponge→ingot, magnesium dust→liquid magnesium.
     * GT-style vertical structure: solid 3x3 base (controller front-centre) and top,
     * two coil rings between them with a hollow centre column.
     */
    BLAST_FURNACE("blast_furnace", 3, 4, 3, true),
    /** Large chemical reactor: oxide+chlorine→TiCl4, purified TiCl4+liquid Mg→sponge, CO chemistry. */
    REACTOR("reactor", 5, 3, 5, false),
    /** Distillation tower: TiCl4→purified TiCl4. */
    DISTILLATION("distillation", 3, 5, 3, false),
    /** Mixer: blends dusts into alloy dusts (copper + nickel → cupronickel). */
    MIXER("mixer", 3, 3, 3, false),
    /**
     * Alloy blast furnace, a faithful copy of GTCEu's Alloy Blast Smelter structure:
     * a 5x5x5 barrel — 5x5 casing plates top and bottom (corners cut, controller at
     * the bottom-front centre), two 12-coil rings with a 12-heat-vent ring between
     * them, all around a hollow 3x3x3 interior. Melts alloy components directly into
     * a molten super-alloy fluid (no mixer pre-step needed, like GT). Higher coil
     * tiers speed it up. For end-game alloys beyond atomic alloy.
     */
    ALLOY_BLAST_FURNACE("alloy_blast_furnace", 5, 5, 5, true),
    /**
     * Vacuum freezer (GT style): a special-steel frost-proof 3x3x3 box that freezes the
     * molten super-alloy fluid back into a solid ingot. No coils.
     */
    VACUUM_FREEZER("vacuum_freezer", 3, 3, 3, false),
    /**
     * Circuit assembler, copied from GTNH/BartWorks' Circuit Assembly Line slice
     * layout: a long 3x3x5 line — solid casing top (controller front-centre on the top
     * layer, like GT) and bottom, glass side walls with a solid casing spine between
     * them on the middle layer. Takes 5 item inputs plus 1 fluid to build circuits
     * above the tier of any single machine. Not a coil tower.
     */
    CIRCUIT_ASSEMBLER("circuit_assembler", 3, 3, 5, false),
    /**
     * Large electrolyzer (platinum-group line): splits raw metal powders and metal
     * sulfate solutions into pure metals plus their gases (GTCEu electrolyzer recipes).
     */
    ELECTROLYZER("electrolyzer", 3, 3, 3, false),
    /**
     * Large centrifuge (platinum-group line): separates dissolved platinum-group
     * sludge into the raw metal fractions (GTCEu centrifuge recipes).
     */
    CENTRIFUGE("centrifuge", 3, 3, 3, false),
    /**
     * Fusion reactor: a verbatim copy of GregTech's fusion reactor structure — a flat
     * 15x3x15 octagonal ring of casing with fusion-glass windows, four superconducting
     * fusion coils, and a large hollow plasma chamber (see MultiblockValidator.FUSION_PATTERN).
     * Fuses deuterium + tritium into helium plasma, then helium plasma + antimatter into
     * molten stellar matter. Endgame.
     */
    FUSION_REACTOR("fusion_reactor", 15, 3, 15, false),
    /**
     * Artificial star generator: a giant 9x9x9 rounded containment sphere of star casing
     * with a 3x3 fusion-glass window at the centre of each face and a hollow 7x7x7 core.
     * Compresses a stellar core and a huge charge of hydrogen (100M RF/t for 10 minutes)
     * into a black hole seed. The final pinnacle machine.
     */
    STAR_GENERATOR("star_generator", 9, 9, 9, false),
    /**
     * Black hole stabilizer: the true final machine — a chunk-sized 16x16x16 neutronium
     * wireframe cage with a glass window at the centre of each face and a hollow core.
     * Stabilises a black hole seed (1,000,000,000 RF/t for 30 minutes) into 10
     * trans-dimensional metal, the ultimate creative-tier material.
     */
    STABILIZER("black_hole_stabilizer", 16, 16, 16, false),
    /**
     * Large Hadron Collider: a big flat octagonal accelerator ring built like GregTech's
     * fusion reactor — a 33x33 tube loop lying flat, three blocks tall, with an
     * accelerator-casing shell, glass windows, glowing collider magnets and a hollow beam
     * channel, around a huge empty centre. Collides hydrogen (protons) directly into
     * antimatter. Craftable just before the end-game; ~100M RF/t.
     */
    HADRON_COLLIDER("hadron_collider", 33, 3, 33, false),
    /**
     * Void ore miner: a GTNH-style drill rig — a solid 7x7 casing base plate with four
     * corner legs, a glowing void-drill mast rising through the centre and a 3x3 crown
     * platform on top. Mines ore from nothing: every tick it consumes 1,000,000 RF
     * (2.5M J) and produces 10 of one randomly chosen ore, weighted by rarity.
     */
    VOID_MINER("void_ore_miner", 7, 9, 7, false),
    /**
     * Oil drilling rig, modelled on GregTech's Fluid Drilling Rig: a 5x5 casing base
     * plate with four corner legs, a drill-pipe string down the centre and a 3x3 crown.
     * Pumps crude oil up from bedrock (10 mB/t at 4,000 RF/t) — the head of the GT-style
     * petroleum chain (crude oil → distillation → desulfurization → diesel).
     */
    OIL_RIG("oil_drilling_rig", 5, 7, 5, false),
    /**
     * Large combustion generator, modelled on GregTech's Large Combustion Engine:
     * a 3x3x4 engine block — engine casing shell with two full gearbox rings in the
     * middle slices. Burns diesel (20 mB/t) into 500,000 RF/t, far beyond an
     * ethylene-fed gas-burning generator (~70k RF/t). Energy flows OUT through its
     * energy ports.
     */
    COMBUSTION_GENERATOR("combustion_generator", 3, 3, 4, false),
    /**
     * Annihilation generator: a 7x7x7 rounded containment sphere of annihilation casing
     * with a fusion-glass window on each face (same family as the artificial star
     * generator). Annihilates hydrogen (50 mB/t) against antimatter (1 mB/t) under a
     * liquid-helium coolant bath (10 mB/t), producing 800,000,000 RF/t. Built after
     * obtaining a stellar core; extremely expensive.
     */
    ANNIHILATION_GENERATOR("annihilation_generator", 7, 7, 7, false);

    public final String id;
    public final int width;
    public final int height;
    public final int depth;
    /** Whether this machine is a vertical coil tower (EBF-shaped) rather than a hollow box. */
    public final boolean coilTower;

    ChemMachineType(String id, int width, int height, int depth, boolean coilTower) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.coilTower = coilTower;
    }
}
