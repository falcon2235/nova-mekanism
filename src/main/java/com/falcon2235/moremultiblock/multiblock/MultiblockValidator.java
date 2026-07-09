package com.falcon2235.moremultiblock.multiblock;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.MekanismMoreMultiblock;
import com.falcon2235.moremultiblock.block.ControllerBlock;
import com.falcon2235.moremultiblock.block.ParallelProcessorBlock;
import com.falcon2235.moremultiblock.block.PortBlock;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Validates the fixed 3(width) x 3(height) x 4(depth) hollow box anchored to the
 * controller. The controller is the centre block of the front face; the box
 * extends four blocks backwards (opposite the controller facing).
 *
 * <p>Because every cell is addressed by a fixed offset from the controller,
 * validation touches exactly 36 block states with no allocation-heavy flood fill
 * and never wanders into a neighbouring structure. That makes casing/port sharing
 * between overlapping multiblocks safe and keeps the per-check cost tiny.
 */
public final class MultiblockValidator {

    public static final int WIDTH = 3;   // along the horizontal axis perpendicular to facing
    public static final int HEIGHT = 3;  // vertical
    public static final int DEPTH = 4;   // into the structure, opposite the controller facing

    private static final String LANG = "multiblock." + MekanismMoreMultiblock.MODID + ".";

    /**
     * The GregTech fusion reactor structure, copied verbatim from GTCEu's
     * {@code fusion_reactor} pattern: a flat 15(wide) x 3(tall) x 15(deep) octagonal
     * ring. Indexed {@code [depth][layer][width]} from the controller outward; the
     * controller {@code 'S'} sits at the front-centre of the middle layer.
     * <pre>
     * S = controller   C/E/O/I = casing (E/O/I also accept a port)
     * G = fusion glass  K = superconducting fusion coil   A = air (hollow)   # = any
     * </pre>
     */
    public static final String[][] FUSION_PATTERN = {
            {"###############", "######OSO######", "###############"},
            {"######ICI######", "####GGAAAGG####", "######ICI######"},
            {"####CC###CC####", "###EAAOGOAAE###", "####CC###CC####"},
            {"###C#######C###", "##EKEG###GEKE##", "###C#######C###"},
            {"##C#########C##", "#GAE#######EAG#", "##C#########C##"},
            {"##C#########C##", "#GAG#######GAG#", "##C#########C##"},
            {"#I###########I#", "OAO#########OAO", "#I###########I#"},
            {"#C###########C#", "GAG#########GAG", "#C###########C#"},
            {"#I###########I#", "OAO#########OAO", "#I###########I#"},
            {"##C#########C##", "#GAG#######GAG#", "##C#########C##"},
            {"##C#########C##", "#GAE#######EAG#", "##C#########C##"},
            {"###C#######C###", "##EKEG###GEKE##", "###C#######C###"},
            {"####CC###CC####", "###EAAOGOAAE###", "####CC###CC####"},
            {"######ICI######", "####GGAAAGG####", "######ICI######"},
            {"###############", "######OGO######", "###############"},
    };
    public static final int FUSION_DEPTH = FUSION_PATTERN.length;      // 15
    public static final int FUSION_WIDTH = FUSION_PATTERN[0][0].length(); // 15
    public static final int FUSION_HEIGHT = 3;
    /** Column index of the controller within a pattern row (centre). */
    public static final int FUSION_HALF_W = FUSION_WIDTH / 2;          // 7

    public record ParallelUnit(BlockPos pos, int tier) {
    }

    public record Result(boolean valid, Component error, List<BlockPos> ports, List<ParallelUnit> parallelUnits) {

        static Result fail(String key, Object... args) {
            return new Result(false, Component.translatable(LANG + key, args), List.of(), List.of());
        }
    }

    private MultiblockValidator() {
    }

    public static Result validate(Level level, BlockPos controllerPos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();

        List<BlockPos> ports = new ArrayList<>();
        List<ParallelUnit> units = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int d = 0; d < DEPTH; d++) {
            for (int u = -1; u <= 1; u++) {
                for (int r = -1; r <= 1; r++) {
                    cursor.set(controllerPos).move(back, d).move(Direction.UP, u).move(right, r);

                    boolean anchor = d == 0 && u == 0 && r == 0;
                    boolean interior = r == 0 && u == 0 && (d == 1 || d == 2);
                    BlockState state = level.getBlockState(cursor);

                    if (interior) {
                        if (!state.isAir()) {
                            return Result.fail("not_hollow", posString(cursor));
                        }
                        continue;
                    }
                    if (anchor) {
                        continue; // the controller itself
                    }

                    Block block = state.getBlock();
                    if (block instanceof ParallelProcessorBlock processor) {
                        units.add(new ParallelUnit(cursor.immutable(), processor.parallel));
                    } else if (block instanceof PortBlock) {
                        ports.add(cursor.immutable());
                    } else if (block instanceof ControllerBlock) {
                        return Result.fail("multiple_controllers");
                    } else if (!state.is(MMMRegistry.CASING.get())) {
                        return Result.fail("invalid_wall", posString(cursor));
                    }
                }
            }
        }
        return new Result(true, null, List.copyOf(ports), List.copyOf(units));
    }

    private static String posString(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    /**
     * Validates a GT-style vertical coil tower: a solid 3x3 casing base with the
     * controller at the front-centre, {@code height - 2} 3x3 coil ring layers above it
     * with a hollow centre column, and a solid 3x3 casing top. The electric blast
     * furnace is height 4 (two coil layers); the alloy blast smelter is height 5 (three
     * coil layers). Ports may replace casing in the base and top layers only. Any coil
     * tier is accepted but all coils must be the same tier; the detected tier is written
     * to {@code coilTierOut[0]}.
     */
    public static Component validateEbf(Level level, BlockPos controllerPos, Direction facing,
                                        Block casing, int height, List<BlockPos> portsOut, int[] coilTierOut) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Block expectedCoil = null;

        for (int dy = 0; dy <= height - 1; dy++) {
            for (int dz = 0; dz <= 2; dz++) {
                for (int dr = -1; dr <= 1; dr++) {
                    cursor.set(controllerPos).move(back, dz).move(Direction.UP, dy).move(right, dr);

                    boolean anchor = dy == 0 && dz == 0 && dr == 0;
                    boolean ringLayer = dy >= 1 && dy <= height - 2;
                    BlockState state = level.getBlockState(cursor);

                    if (anchor) {
                        continue;
                    }
                    if (ringLayer) {
                        if (dz == 1 && dr == 0) {
                            if (!state.isAir()) {
                                return Component.translatable(LANG + "not_hollow", posString(cursor));
                            }
                        } else {
                            int tier = com.falcon2235.moremultiblock.MMMRegistry.coilTierOf(state);
                            if (tier < 0) {
                                return Component.translatable(LANG + "invalid_coil", posString(cursor));
                            }
                            if (expectedCoil == null) {
                                expectedCoil = state.getBlock();
                                if (coilTierOut != null) {
                                    coilTierOut[0] = tier;
                                }
                            } else if (!state.is(expectedCoil)) {
                                return Component.translatable(LANG + "mixed_coils", posString(cursor));
                            }
                        }
                    } else if (state.getBlock() instanceof PortBlock) {
                        if (portsOut != null) {
                            portsOut.add(cursor.immutable());
                        }
                    } else if (!state.is(casing)) {
                        return Component.translatable(LANG + "invalid_wall", posString(cursor));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Validates the GT Alloy Blast Smelter barrel (copied from GTCEu's
     * {@code alloy_blast_smelter} pattern):
     * <pre>
     * y=4  5x5 casing plate, corners cut
     * y=3  ring of 12 coils around a 3x3 hollow
     * y=2  ring of 12 heat vents around a 3x3 hollow
     * y=1  ring of 12 coils around a 3x3 hollow
     * y=0  5x5 casing plate, corners cut, controller at the front centre
     * </pre>
     * The four vertical corner columns are ignored entirely (GT treats them as "any").
     * Ports may replace casing in the top and bottom plates only. All 24 coils must be
     * the same tier; the detected tier is written to {@code coilTierOut[0]}.
     */
    public static Component validateAbs(Level level, BlockPos controllerPos, Direction facing,
                                        Block casing, Block vent, List<BlockPos> portsOut, int[] coilTierOut) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Block expectedCoil = null;

        for (int dy = 0; dy <= 4; dy++) {
            for (int dz = 0; dz <= 4; dz++) {
                for (int dr = -2; dr <= 2; dr++) {
                    if (Math.abs(dr) == 2 && (dz == 0 || dz == 4)) {
                        continue; // corner columns are "any" in GT's pattern
                    }
                    boolean anchor = dy == 0 && dz == 0 && dr == 0;
                    if (anchor) {
                        continue;
                    }
                    cursor.set(controllerPos).move(back, dz).move(Direction.UP, dy).move(right, dr);
                    BlockState state = level.getBlockState(cursor);

                    boolean plate = dy == 0 || dy == 4;
                    if (plate) {
                        if (state.getBlock() instanceof PortBlock) {
                            if (portsOut != null) {
                                portsOut.add(cursor.immutable());
                            }
                        } else if (!state.is(casing)) {
                            return Component.translatable(LANG + "invalid_wall", posString(cursor));
                        }
                        continue;
                    }
                    // ring layers: 3x3 hollow interior, coils (y1/y3) or vents (y2) on the ring
                    boolean interior = Math.abs(dr) <= 1 && dz >= 1 && dz <= 3;
                    if (interior) {
                        if (!state.isAir()) {
                            return Component.translatable(LANG + "not_hollow", posString(cursor));
                        }
                    } else if (dy == 2) {
                        if (!state.is(vent)) {
                            return Component.translatable(LANG + "invalid_vent", posString(cursor));
                        }
                    } else {
                        int tier = MMMRegistry.coilTierOf(state);
                        if (tier < 0) {
                            return Component.translatable(LANG + "invalid_coil", posString(cursor));
                        }
                        if (expectedCoil == null) {
                            expectedCoil = state.getBlock();
                            if (coilTierOut != null) {
                                coilTierOut[0] = tier;
                            }
                        } else if (!state.is(expectedCoil)) {
                            return Component.translatable(LANG + "mixed_coils", posString(cursor));
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Validates the artificial star generator: a giant hollow {@code size} cube (odd),
     * with the eight corners cut off (rounded), a hollow interior, and a 3x3 glass window
     * at the centre of each of the six faces. The controller sits at the centre of the
     * front face (where the front window's middle would be). Casing cells accept a port.
     */
    public static Component validateStar(Level level, BlockPos controllerPos, Direction facing,
                                         Block casing, Block glass, int size, List<BlockPos> portsOut) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        int half = size / 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int d = 0; d < size; d++) {
            for (int u = -half; u <= half; u++) {
                for (int r = -half; r <= half; r++) {
                    int cd = Math.abs(d - half);
                    int cu = Math.abs(u);
                    int cr = Math.abs(r);
                    int maxDist = Math.max(cd, Math.max(cu, cr));
                    if (cd == half && cu == half && cr == half) {
                        continue; // cut the 8 corners for a rounded look
                    }
                    boolean anchor = d == 0 && u == 0 && r == 0;
                    if (anchor) {
                        continue; // controller at the front-face centre
                    }
                    cursor.set(controllerPos).move(back, d).move(Direction.UP, u).move(right, r);
                    BlockState state = level.getBlockState(cursor);

                    if (maxDist < half) {
                        if (!state.isAir()) {
                            return Component.translatable(LANG + "not_hollow", posString(cursor));
                        }
                        continue;
                    }
                    if (isStarWindow(cd, cu, cr, half)) {
                        if (!state.is(glass)) {
                            return Component.translatable(LANG + "invalid_glass", posString(cursor));
                        }
                    } else if (state.getBlock() instanceof PortBlock) {
                        if (portsOut != null) {
                            portsOut.add(cursor.immutable());
                        }
                    } else if (!state.is(casing)) {
                        return Component.translatable(LANG + "invalid_wall", posString(cursor));
                    }
                }
            }
        }
        return null;
    }

    public static final int STABILIZER_SIZE = 16;

    public static final int COLLIDER_SIZE = 33;             // width & depth of the flat octagonal ring
    public static final int COLLIDER_HEIGHT = 3;            // tube is three tall (bottom/mid/top)
    private static final int COLLIDER_CORNER_FLATTEN = 8;   // how far the octagon corners are flattened

    /**
     * Classifies a Large Hadron Collider cell like a scaled-up GregTech fusion-reactor tube.
     * Uses an "octagon distance" — {@code max(chebyshev, manhattan - flatten)} — so the straight
     * edges and the diagonal corners form ONE continuous octagon ring (no gaps at the corners).
     * The 3-wide band's cross-section matches the fusion reactor's: casing on the top &amp; bottom
     * of the centre column, glass on the outer wall and a glowing magnet on the inner wall (both
     * at mid height), a hollow beam down the centre, open corners.
     * {@code layer} 0/1/2 = bottom/middle/top. Returns 0 = skip, 1 = casing, 2 = magnet, 3 = glass.
     */
    public static int colliderKind(int d, int r, int layer) {
        int half = COLLIDER_SIZE / 2;
        int cheby = Math.max(Math.abs(r), Math.abs(d - half));
        int man = Math.abs(r) + Math.abs(d - half);
        int oct = Math.max(cheby, man - COLLIDER_CORNER_FLATTEN); // octagon "radius" of this cell
        if (oct < half - 2 || oct > half) {
            return 0; // huge empty centre, or outside the ring
        }
        if (layer == 1) { // middle layer
            if (oct == half) {
                return 3; // outer glass window
            }
            if (oct == half - 2) {
                return 2; // inner magnet wall
            }
            return 0; // oct == half-1: hollow beam channel
        }
        // top & bottom: casing only on the tube's centre column (the beam-channel spine)
        return oct == half - 1 ? 1 : 0;
    }

    /**
     * Validates the Large Hadron Collider: a flat 33x33 octagonal tube ring, three tall,
     * built like a giant fusion reactor. The controller sits at the front centre of the tube's
     * middle layer; casing cells accept a port. The vast interior is free.
     */
    public static Component validateCollider(Level level, BlockPos controllerPos, Direction facing,
                                             Block casing, Block magnet, Block glass, List<BlockPos> portsOut) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        int half = COLLIDER_SIZE / 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int d = 0; d < COLLIDER_SIZE; d++) {
            for (int r = -half; r <= half; r++) {
                for (int layer = 0; layer < COLLIDER_HEIGHT; layer++) {
                    int kind = colliderKind(d, r, layer);
                    if (kind == 0) {
                        continue;
                    }
                    if (d == 0 && r == 0 && layer == 1) {
                        continue; // controller at the front centre of the middle layer
                    }
                    cursor.set(controllerPos).move(back, d).move(Direction.UP, layer - 1).move(right, r);
                    BlockState state = level.getBlockState(cursor);
                    if (kind == 2) {
                        if (!state.is(magnet)) {
                            return Component.translatable(LANG + "invalid_coil", posString(cursor));
                        }
                    } else if (kind == 3) {
                        if (!state.is(glass)) {
                            return Component.translatable(LANG + "invalid_glass", posString(cursor));
                        }
                    } else if (state.getBlock() instanceof PortBlock) { // casing tube (ports allowed)
                        if (portsOut != null) {
                            portsOut.add(cursor.immutable());
                        }
                    } else if (!state.is(casing)) {
                        return Component.translatable(LANG + "invalid_wall", posString(cursor));
                    }
                }
            }
        }
        return null;
    }

    /** Width offset of grid column {@code rr}: the controller column ({@code rr = 7}) maps to r = 0. */
    private static int stabilizerR(int rr) {
        return rr - (STABILIZER_SIZE / 2 - 1);
    }

    /**
     * Validates the black hole stabilizer: a chunk-sized {@value #STABILIZER_SIZE} cube — the
     * twelve cube edges are neutronium casing and every one of the six faces is fully glazed
     * with stabilizer glass (a sealed glass box in a neutronium frame). Only the frame and
     * face cells are checked; the interior is left free, so the cube can be built anywhere.
     * The controller sits on the front-bottom edge and edge cells accept a port.
     */
    public static Component validateStabilizer(Level level, BlockPos controllerPos, Direction facing,
                                               Block casing, Block glass, List<BlockPos> portsOut) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int d = 0; d < STABILIZER_SIZE; d++) {
            for (int u = 0; u < STABILIZER_SIZE; u++) {
                for (int rr = 0; rr < STABILIZER_SIZE; rr++) {
                    int kind = stabilizerCell(d, u, rr);
                    if (kind == 0) {
                        continue; // open face / interior — not part of the cage, ignored
                    }
                    int r = stabilizerR(rr);
                    if (d == 0 && u == 0 && r == 0) {
                        continue; // controller on the front-bottom edge
                    }
                    cursor.set(controllerPos).move(back, d).move(Direction.UP, u).move(right, r);
                    BlockState state = level.getBlockState(cursor);
                    if (kind == 2) { // face-centre window
                        if (!state.is(glass)) {
                            return Component.translatable(LANG + "invalid_glass", posString(cursor));
                        }
                    } else if (state.getBlock() instanceof PortBlock) { // cube edge → neutronium or port
                        if (portsOut != null) {
                            portsOut.add(cursor.immutable());
                        }
                    } else if (!state.is(casing)) {
                        return Component.translatable(LANG + "invalid_wall", posString(cursor));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Classifies a stabilizer cell (grid coords 0..15): 0 = open interior (not checked),
     * 1 = neutronium edge (2+ axes at an extreme), 2 = glass face (exactly one axis at an extreme).
     */
    public static int stabilizerCell(int d, int u, int rr) {
        int max = STABILIZER_SIZE - 1;
        int ext = ((d == 0 || d == max) ? 1 : 0) + ((u == 0 || u == max) ? 1 : 0) + ((rr == 0 || rr == max) ? 1 : 0);
        if (ext >= 2) {
            return 1;
        }
        return ext == 1 ? 2 : 0;
    }

    /** A 3x3 window at a face centre: exactly one axis is maxed and the other two are within 1. */
    public static boolean isStarWindow(int cd, int cu, int cr, int half) {
        int maxedAxes = (cd == half ? 1 : 0) + (cu == half ? 1 : 0) + (cr == half ? 1 : 0);
        if (maxedAxes != 1) {
            return false;
        }
        if (cd == half) {
            return cu <= 1 && cr <= 1;
        }
        if (cu == half) {
            return cd <= 1 && cr <= 1;
        }
        return cd <= 1 && cu <= 1; // cr == half
    }

    /**
     * Validates the GT-style primitive blast furnace: same vertical shape as the EBF
     * (solid 3x3 base with the controller front-centre, two 3x3 rings with a hollow
     * centre column, solid 3x3 top) but built entirely from the given brick block.
     * Ports may replace bricks in the base and top layers only.
     */
    public static Component validatePbf(Level level, BlockPos controllerPos, Direction facing,
                                        Block brick, List<BlockPos> portsOut) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dy = 0; dy <= 3; dy++) {
            for (int dz = 0; dz <= 2; dz++) {
                for (int dr = -1; dr <= 1; dr++) {
                    cursor.set(controllerPos).move(back, dz).move(Direction.UP, dy).move(right, dr);

                    boolean anchor = dy == 0 && dz == 0 && dr == 0;
                    boolean ringLayer = dy == 1 || dy == 2;
                    BlockState state = level.getBlockState(cursor);

                    if (anchor) {
                        continue;
                    }
                    if (ringLayer) {
                        if (dz == 1 && dr == 0) {
                            if (!state.isAir()) {
                                return Component.translatable(LANG + "not_hollow", posString(cursor));
                            }
                        } else if (!state.is(brick)) {
                            return Component.translatable(LANG + "invalid_wall", posString(cursor));
                        }
                    } else if (state.getBlock() instanceof PortBlock) {
                        if (portsOut != null) {
                            portsOut.add(cursor.immutable());
                        }
                    } else if (!state.is(brick)) {
                        return Component.translatable(LANG + "invalid_wall", posString(cursor));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Validates the Circuit Assembly Line, copied from GTNH/BartWorks'
     * {@code MTECircuitAssemblyLine} slice layout (rotated so the line runs backwards
     * from the controller):
     * <pre>
     * top layer     casing  casing  casing   (grate roof; controller front-centre)
     * middle layer  glass   casing  glass    (glass side walls, solid casing spine)
     * bottom layer  casing  casing  casing   (floor / bus line)
     * </pre>
     * The controller sits on the TOP layer at the front centre, like the GT original.
     * Ports may replace casing on the top and bottom layers; the glass walls and the
     * middle spine must be exact.
     */
    public static Component validateAssemblyLine(Level level, BlockPos controllerPos, Direction facing,
                                                 Block casing, Block glass, int width, int height, int depth,
                                                 List<BlockPos> portsOut) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        int halfW = (width - 1) / 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int k = 0; k < height; k++) { // k = layers counted downward from the controller
            for (int d = 0; d < depth; d++) {
                for (int r = -halfW; r <= halfW; r++) {
                    cursor.set(controllerPos).move(back, d).move(Direction.DOWN, k).move(right, r);

                    boolean anchor = k == 0 && d == 0 && r == 0;
                    if (anchor) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    boolean middle = k >= 1 && k <= height - 2;

                    if (middle) {
                        if (Math.abs(r) == halfW) {
                            // glass side walls
                            if (!state.is(glass)) {
                                return Component.translatable(LANG + "invalid_glass", posString(cursor));
                            }
                        } else if (!state.is(casing)) {
                            // solid assembly-casing spine (no ports here)
                            return Component.translatable(LANG + "invalid_wall", posString(cursor));
                        }
                    } else if (state.getBlock() instanceof PortBlock) {
                        if (portsOut != null) {
                            portsOut.add(cursor.immutable());
                        }
                    } else if (!state.is(casing)) {
                        return Component.translatable(LANG + "invalid_wall", posString(cursor));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Validates the GregTech fusion reactor against {@link #FUSION_PATTERN}: a flat
     * 15x3x15 octagonal ring. The controller sits at the front-centre of the middle
     * layer; the ring extends backwards (depth) and ±7 sideways (width), three blocks
     * tall. Casing/energy/import/export cells accept casing or a port; glass and coil
     * cells are exact; air cells must be empty. Returns {@code null} when valid.
     */
    public static Component validateFusion(Level level, BlockPos controllerPos, Direction facing,
                                           Block casing, Block coil, Block glass, List<BlockPos> portsOut) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int d = 0; d < FUSION_DEPTH; d++) {
            for (int layer = 0; layer < FUSION_HEIGHT; layer++) {
                String row = FUSION_PATTERN[d][layer];
                int dy = 1 - layer; // layer 0 = top (+1), 1 = middle (0), 2 = bottom (-1)
                for (int x = 0; x < FUSION_WIDTH; x++) {
                    char c = row.charAt(x);
                    if (c == '#' || c == 'S') {
                        continue; // "any" cells and the controller anchor itself
                    }
                    int r = x - FUSION_HALF_W;
                    cursor.set(controllerPos).move(back, d).move(Direction.UP, dy).move(right, r);
                    BlockState state = level.getBlockState(cursor);
                    switch (c) {
                        case 'A' -> {
                            if (!state.isAir()) {
                                return Component.translatable(LANG + "not_hollow", posString(cursor));
                            }
                        }
                        case 'G' -> {
                            if (!state.is(glass)) {
                                return Component.translatable(LANG + "invalid_glass", posString(cursor));
                            }
                        }
                        case 'K' -> {
                            if (!state.is(coil)) {
                                return Component.translatable(LANG + "invalid_coil", posString(cursor));
                            }
                        }
                        default -> { // C, E, O, I: casing frame or a port
                            if (state.getBlock() instanceof PortBlock) {
                                if (portsOut != null) {
                                    portsOut.add(cursor.immutable());
                                }
                            } else if (!state.is(casing)) {
                                return Component.translatable(LANG + "invalid_wall", posString(cursor));
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Validates a hollow casing box of arbitrary (odd width, odd height, any depth)
     * anchored to a controller at the centre of the front face, used by the chemical
     * machines. Returns {@code null} when valid, otherwise a translated error.
     * Width and height must be odd so the controller can sit centred on the front face.
     */
    public static Component validateBox(Level level, BlockPos controllerPos, Direction facing,
                                        int width, int height, int depth, Block casing, @Nullable Block coil,
                                        List<BlockPos> portsOut) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        int halfW = (width - 1) / 2;
        int halfH = (height - 1) / 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int d = 0; d < depth; d++) {
            for (int u = -halfH; u <= halfH; u++) {
                for (int r = -halfW; r <= halfW; r++) {
                    cursor.set(controllerPos).move(back, d).move(Direction.UP, u).move(right, r);

                    boolean anchor = d == 0 && u == 0 && r == 0;
                    boolean interior = Math.abs(r) < halfW && Math.abs(u) < halfH && d > 0 && d < depth - 1;
                    // Middle slices of a coiled machine (the EBF) must be coil rings.
                    boolean coilRing = coil != null && d > 0 && d < depth - 1;
                    BlockState state = level.getBlockState(cursor);

                    if (interior) {
                        if (!state.isAir()) {
                            return Component.translatable(LANG + "not_hollow", posString(cursor));
                        }
                    } else if (!anchor) {
                        if (coilRing) {
                            if (!state.is(coil)) {
                                return Component.translatable(LANG + "invalid_coil", posString(cursor));
                            }
                        } else if (state.getBlock() instanceof PortBlock) {
                            if (portsOut != null) {
                                portsOut.add(cursor.immutable());
                            }
                        } else if (!state.is(casing)) {
                            return Component.translatable(LANG + "invalid_wall", posString(cursor));
                        }
                    }
                }
            }
        }
        return null;
    }
}
