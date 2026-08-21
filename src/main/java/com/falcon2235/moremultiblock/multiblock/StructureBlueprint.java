package com.falcon2235.moremultiblock.multiblock;

import com.falcon2235.moremultiblock.MMMRegistry;
import com.falcon2235.moremultiblock.block.ChemMachineBlock;
import com.falcon2235.moremultiblock.block.ControllerBlock;
import com.falcon2235.moremultiblock.block.PbfBlock;
import com.falcon2235.moremultiblock.machine.ChemMachineType;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Produces the list of solid blocks a multiblock needs at each offset from its
 * controller — the "blueprint" the construction terminal fills in. Mirrors the offset
 * walks in {@link MultiblockValidator}, but emits the expected block for every cell
 * instead of checking one. Hollow/air cells and the controller itself are omitted;
 * port-eligible cells get the wall casing (existing ports are left alone by the caller).
 */
public final class StructureBlueprint {

    /** One block the terminal should place at a position. */
    public record Cell(BlockPos pos, Block block) {
    }

    private StructureBlueprint() {
    }

    /** Blueprint for the controller at {@code pos}, or {@code null} if it is not one of ours. */
    @Nullable
    public static List<Cell> forController(BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof ControllerBlock) {
            return parallelBox(pos, state.getValue(ControllerBlock.FACING));
        }
        if (block instanceof PbfBlock) {
            return brickTower(pos, state.getValue(PbfBlock.FACING));
        }
        if (block instanceof ChemMachineBlock chem) {
            return chemMachine(pos, chem.machineType, state.getValue(ChemMachineBlock.FACING));
        }
        return null;
    }

    private static List<Cell> chemMachine(BlockPos pos, ChemMachineType type, Direction facing) {
        return switch (type) {
            case BLAST_FURNACE -> coilTower(pos, facing, MMMRegistry.chemCasing(type), MMMRegistry.chemCoil(type), type.height);
            case ALLOY_BLAST_FURNACE -> alloyBarrel(pos, facing);
            case CIRCUIT_ASSEMBLER -> assemblyLine(pos, facing);
            case FUSION_REACTOR -> fusionRing(pos, facing);
            case STAR_GENERATOR, ANNIHILATION_GENERATOR, MATTER_REPLICATOR -> starSphere(pos, facing, type);
            case STABILIZER -> stabilizerCage(pos, facing);
            case HADRON_COLLIDER -> colliderLoop(pos, facing);
            case VOID_MINER -> voidMinerRig(pos, facing);
            case OIL_RIG -> oilRig(pos, facing);
            case ASSEMBLY_LINE -> asslineLane(pos, facing);
            case COMBUSTION_GENERATOR -> coilBox(pos, facing, MMMRegistry.chemCasing(type),
                    MMMRegistry.chemCoil(type), type.width, type.height, type.depth);
            default -> box(pos, facing, MMMRegistry.chemCasing(type), type.width, type.height, type.depth);
        };
    }

    // --- parallel processing box (3x3x4 casing shell) ---
    private static List<Cell> parallelBox(BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Block casing = MMMRegistry.CASING.get();
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int d = 0; d < MultiblockValidator.DEPTH; d++) {
            for (int u = -1; u <= 1; u++) {
                for (int r = -1; r <= 1; r++) {
                    boolean anchor = d == 0 && u == 0 && r == 0;
                    boolean interior = r == 0 && u == 0 && (d == 1 || d == 2);
                    if (anchor || interior) {
                        continue;
                    }
                    c.set(pos).move(back, d).move(Direction.UP, u).move(right, r);
                    cells.add(new Cell(c.immutable(), casing));
                }
            }
        }
        return cells;
    }

    // --- generic hollow casing box ---
    private static List<Cell> box(BlockPos pos, Direction facing, Block casing, int width, int height, int depth) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        int halfW = (width - 1) / 2;
        int halfH = (height - 1) / 2;
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int d = 0; d < depth; d++) {
            for (int u = -halfH; u <= halfH; u++) {
                for (int r = -halfW; r <= halfW; r++) {
                    boolean anchor = d == 0 && u == 0 && r == 0;
                    boolean interior = Math.abs(r) < halfW && Math.abs(u) < halfH && d > 0 && d < depth - 1;
                    if (anchor || interior) {
                        continue;
                    }
                    c.set(pos).move(back, d).move(Direction.UP, u).move(right, r);
                    cells.add(new Cell(c.immutable(), casing));
                }
            }
        }
        return cells;
    }

    // --- GT vertical coil tower (electric blast furnace) ---
    private static List<Cell> coilTower(BlockPos pos, Direction facing, Block casing, Block coil, int height) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= height - 1; dy++) {
            for (int dz = 0; dz <= 2; dz++) {
                for (int dr = -1; dr <= 1; dr++) {
                    boolean anchor = dy == 0 && dz == 0 && dr == 0;
                    if (anchor) {
                        continue;
                    }
                    boolean ringLayer = dy >= 1 && dy <= height - 2;
                    Block block;
                    if (ringLayer) {
                        if (dz == 1 && dr == 0) {
                            continue; // hollow centre column
                        }
                        block = coil;
                    } else {
                        block = casing;
                    }
                    c.set(pos).move(back, dz).move(Direction.UP, dy).move(right, dr);
                    cells.add(new Cell(c.immutable(), block));
                }
            }
        }
        return cells;
    }

    // --- GT alloy blast smelter barrel (5x5x5) ---
    private static List<Cell> alloyBarrel(BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Block casing = MMMRegistry.chemCasing(ChemMachineType.ALLOY_BLAST_FURNACE);
        Block vent = MMMRegistry.HEAT_VENT.get();
        Block coil = MMMRegistry.chemCoil(ChemMachineType.ALLOY_BLAST_FURNACE);
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 4; dy++) {
            for (int dz = 0; dz <= 4; dz++) {
                for (int dr = -2; dr <= 2; dr++) {
                    if (Math.abs(dr) == 2 && (dz == 0 || dz == 4)) {
                        continue; // corner column
                    }
                    boolean anchor = dy == 0 && dz == 0 && dr == 0;
                    if (anchor) {
                        continue;
                    }
                    Block block;
                    if (dy == 0 || dy == 4) {
                        block = casing;
                    } else {
                        boolean interior = Math.abs(dr) <= 1 && dz >= 1 && dz <= 3;
                        if (interior) {
                            continue; // hollow
                        }
                        block = dy == 2 ? vent : coil;
                    }
                    c.set(pos).move(back, dz).move(Direction.UP, dy).move(right, dr);
                    cells.add(new Cell(c.immutable(), block));
                }
            }
        }
        return cells;
    }

    // --- GTNH circuit assembly line (grate roof, glass+arm middle, casing floor) ---
    private static List<Cell> assemblyLine(BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        ChemMachineType type = ChemMachineType.CIRCUIT_ASSEMBLER;
        Block casing = MMMRegistry.chemCasing(type);
        Block glass = MMMRegistry.ASSEMBLY_GLASS.get();
        Block conveyor = MMMRegistry.ASSLINE_CONVEYOR.get();
        Block grate = MMMRegistry.ASSLINE_GRATE.get();
        int halfW = (type.width - 1) / 2;
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int k = 0; k < type.height; k++) {
            for (int d = 0; d < type.depth; d++) {
                for (int r = -halfW; r <= halfW; r++) {
                    boolean anchor = k == 0 && d == 0 && r == 0;
                    if (anchor) {
                        continue;
                    }
                    Block block;
                    if (k == 0) {
                        block = grate;
                    } else if (k <= type.height - 2) {
                        block = Math.abs(r) == halfW ? glass : conveyor;
                    } else {
                        block = casing;
                    }
                    c.set(pos).move(back, d).move(Direction.DOWN, k).move(right, r);
                    cells.add(new Cell(c.immutable(), block));
                }
            }
        }
        return cells;
    }

    // --- GregTech fusion reactor (15x3x15 ring, from FUSION_PATTERN) ---
    private static List<Cell> fusionRing(BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Block casing = MMMRegistry.chemCasing(ChemMachineType.FUSION_REACTOR);
        Block coil = MMMRegistry.FUSION_COIL.get();
        Block glass = MMMRegistry.FUSION_GLASS.get();
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int d = 0; d < MultiblockValidator.FUSION_DEPTH; d++) {
            for (int layer = 0; layer < MultiblockValidator.FUSION_HEIGHT; layer++) {
                String row = MultiblockValidator.FUSION_PATTERN[d][layer];
                int dy = 1 - layer;
                for (int x = 0; x < MultiblockValidator.FUSION_WIDTH; x++) {
                    char ch = row.charAt(x);
                    Block block = switch (ch) {
                        case 'G' -> glass;
                        case 'K' -> coil;
                        case 'C', 'E', 'O', 'I' -> casing;
                        default -> null; // '#', 'A', 'S'
                    };
                    if (block == null) {
                        continue;
                    }
                    int r = x - MultiblockValidator.FUSION_HALF_W;
                    c.set(pos).move(back, d).move(Direction.UP, dy).move(right, r);
                    cells.add(new Cell(c.immutable(), block));
                }
            }
        }
        return cells;
    }

    // --- rounded containment sphere with face windows (star generator, annihilation generator) ---
    private static List<Cell> starSphere(BlockPos pos, Direction facing, ChemMachineType type) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Block casing = MMMRegistry.chemCasing(type);
        Block glass = MMMRegistry.FUSION_GLASS.get();
        int size = type.width;
        int half = size / 2;
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int d = 0; d < size; d++) {
            for (int u = -half; u <= half; u++) {
                for (int r = -half; r <= half; r++) {
                    int cd = Math.abs(d - half);
                    int cu = Math.abs(u);
                    int cr = Math.abs(r);
                    int maxDist = Math.max(cd, Math.max(cu, cr));
                    if (cd == half && cu == half && cr == half) {
                        continue; // corner
                    }
                    boolean anchor = d == 0 && u == 0 && r == 0;
                    if (anchor || maxDist < half) {
                        continue; // controller or hollow interior
                    }
                    Block block = MultiblockValidator.isStarWindow(cd, cu, cr, half) ? glass : casing;
                    c.set(pos).move(back, d).move(Direction.UP, u).move(right, r);
                    cells.add(new Cell(c.immutable(), block));
                }
            }
        }
        return cells;
    }

    // --- black hole stabilizer (chunk-sized neutronium wireframe cage) ---
    private static List<Cell> stabilizerCage(BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Block casing = MMMRegistry.chemCasing(ChemMachineType.STABILIZER);
        Block glass = MMMRegistry.STABILIZER_GLASS.get();
        int size = MultiblockValidator.STABILIZER_SIZE;
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int d = 0; d < size; d++) {
            for (int u = 0; u < size; u++) {
                for (int rr = 0; rr < size; rr++) {
                    int r = rr - (size / 2 - 1);
                    boolean anchor = d == 0 && u == 0 && r == 0;
                    if (anchor) {
                        continue;
                    }
                    int kind = MultiblockValidator.stabilizerCell(d, u, rr);
                    if (kind == 0) {
                        continue; // open air
                    }
                    c.set(pos).move(back, d).move(Direction.UP, u).move(right, r);
                    cells.add(new Cell(c.immutable(), kind == 2 ? glass : casing));
                }
            }
        }
        return cells;
    }

    // --- large hadron collider (flat octagonal tube ring, fusion-reactor style) ---
    private static List<Cell> colliderLoop(BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Block casing = MMMRegistry.chemCasing(ChemMachineType.HADRON_COLLIDER);
        Block magnet = MMMRegistry.COLLIDER_MAGNET.get();
        Block glass = MMMRegistry.FUSION_GLASS.get();
        int half = MultiblockValidator.COLLIDER_SIZE / 2;
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int d = 0; d < MultiblockValidator.COLLIDER_SIZE; d++) {
            for (int r = -half; r <= half; r++) {
                for (int layer = 0; layer < MultiblockValidator.COLLIDER_HEIGHT; layer++) {
                    int kind = MultiblockValidator.colliderKind(d, r, layer);
                    if (kind == 0) {
                        continue;
                    }
                    Block block = kind == 2 ? magnet : kind == 3 ? glass : casing;
                    c.set(pos).move(back, d).move(Direction.UP, layer - 1).move(right, r);
                    cells.add(new Cell(c.immutable(), block));
                }
            }
        }
        return cells;
    }

    // --- void ore miner (GTNH-style drill rig: base plate, corner legs, drill mast, crown) ---
    private static List<Cell> voidMinerRig(BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Block casing = MMMRegistry.chemCasing(ChemMachineType.VOID_MINER);
        Block drill = MMMRegistry.VOID_DRILL.get();
        int mid = (MultiblockValidator.VOID_MINER_SIZE - 1) / 2;
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int d = 0; d < MultiblockValidator.VOID_MINER_SIZE; d++) {
            for (int u = 0; u < MultiblockValidator.VOID_MINER_HEIGHT; u++) {
                for (int r = -mid; r <= mid; r++) {
                    int kind = MultiblockValidator.voidMinerKind(d, r, u);
                    if (kind == 0 || (d == 0 && u == 0 && r == 0)) {
                        continue; // open cell or the controller itself
                    }
                    c.set(pos).move(back, d).move(Direction.UP, u).move(right, r);
                    cells.add(new Cell(c.immutable(), kind == 2 ? drill : casing));
                }
            }
        }
        return cells;
    }

    // --- oil drilling rig (GT fluid drill: base plate, corner legs, drill pipe, crown) ---
    private static List<Cell> oilRig(BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Block casing = MMMRegistry.chemCasing(ChemMachineType.OIL_RIG);
        Block pipe = MMMRegistry.DRILL_PIPE.get();
        int mid = (MultiblockValidator.OIL_RIG_SIZE - 1) / 2;
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int d = 0; d < MultiblockValidator.OIL_RIG_SIZE; d++) {
            for (int u = 0; u < MultiblockValidator.OIL_RIG_HEIGHT; u++) {
                for (int r = -mid; r <= mid; r++) {
                    int kind = MultiblockValidator.oilRigKind(d, r, u);
                    if (kind == 0 || (d == 0 && u == 0 && r == 0)) {
                        continue;
                    }
                    c.set(pos).move(back, d).move(Direction.UP, u).move(right, r);
                    cells.add(new Cell(c.immutable(), kind == 2 ? pipe : casing));
                }
            }
        }
        return cells;
    }

    // --- hollow casing box with full coil/gearbox rings in the middle slices ---
    private static List<Cell> coilBox(BlockPos pos, Direction facing, Block casing, Block coil,
                                      int width, int height, int depth) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        int halfW = (width - 1) / 2;
        int halfH = (height - 1) / 2;
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int d = 0; d < depth; d++) {
            for (int u = -halfH; u <= halfH; u++) {
                for (int r = -halfW; r <= halfW; r++) {
                    boolean anchor = d == 0 && u == 0 && r == 0;
                    boolean interior = Math.abs(r) < halfW && Math.abs(u) < halfH && d > 0 && d < depth - 1;
                    if (anchor || interior) {
                        continue;
                    }
                    boolean coilRing = d > 0 && d < depth - 1;
                    c.set(pos).move(back, d).move(Direction.UP, u).move(right, r);
                    cells.add(new Cell(c.immutable(), coilRing ? coil : casing));
                }
            }
        }
        return cells;
    }

    // --- assembly line (GTCEu pattern FIF/RTR/SAG/#Y#; controller at u=2, left column) ---
    private static List<Cell> asslineLane(BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Block casing = MMMRegistry.chemCasing(ChemMachineType.ASSEMBLY_LINE);
        Block conveyor = MMMRegistry.ASSLINE_CONVEYOR.get();
        Block glass = MMMRegistry.ASSEMBLY_GLASS.get();
        Block grate = MMMRegistry.ASSLINE_GRATE.get();
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int d = 0; d < MultiblockValidator.ASSLINE_LENGTH; d++) {
            for (int u = 0; u < MultiblockValidator.ASSLINE_HEIGHT; u++) {
                for (int r = 0; r <= 2; r++) {
                    if (d == 0 && u == 2 && r == 0) {
                        continue; // controller
                    }
                    int kind = MultiblockValidator.asslineKind(d, r, u);
                    if (kind == 0) {
                        continue; // "any" cell
                    }
                    Block block = switch (kind) {
                        case 2 -> conveyor;
                        case 3 -> glass;
                        case 4 -> grate;
                        default -> casing;
                    };
                    c.set(pos).move(back, d).move(Direction.UP, u - 2).move(right, r);
                    cells.add(new Cell(c.immutable(), block));
                }
            }
        }
        return cells;
    }

    // --- primitive blast furnace (all-brick vertical tower) ---
    private static List<Cell> brickTower(BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Block brick = Blocks.BRICKS;
        List<Cell> cells = new ArrayList<>();
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 3; dy++) {
            for (int dz = 0; dz <= 2; dz++) {
                for (int dr = -1; dr <= 1; dr++) {
                    boolean anchor = dy == 0 && dz == 0 && dr == 0;
                    if (anchor) {
                        continue;
                    }
                    boolean ringLayer = dy == 1 || dy == 2;
                    if (ringLayer && dz == 1 && dr == 0) {
                        continue; // hollow centre column
                    }
                    c.set(pos).move(back, dz).move(Direction.UP, dy).move(right, dr);
                    cells.add(new Cell(c.immutable(), brick));
                }
            }
        }
        return cells;
    }
}
