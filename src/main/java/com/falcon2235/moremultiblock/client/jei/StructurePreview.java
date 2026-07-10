package com.falcon2235.moremultiblock.client.jei;

import com.falcon2235.moremultiblock.MMMRegistry;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a static isometric 3D preview of a machine's structure (GregTech style)
 * inside a JEI category, using the vanilla block renderer. The controller sits on
 * the front face and is drawn last, facing the viewer and protruding slightly so
 * its position in the structure is obvious.
 *
 * <p>The box front face is the -Z side (cell z == 0). The view is yawed 225° so that
 * front face points toward the camera (otherwise the controller ends up on the far
 * side, hidden behind the casing).
 */
public final class StructurePreview {

    /** How far (in blocks) the controller pokes out of the front face for visibility. */
    private static final float CONTROLLER_POP = 0.34F;

    private StructurePreview() {
    }

    public static void render(GuiGraphics graphics, int centerX, int centerY,
                              int w, int h, int d, BlockState controller, float scale) {
        render(graphics, centerX, centerY, w, h, d, controller,
                MMMRegistry.CASING.get().defaultBlockState(), null, StructureEntry.Mode.BOX, null, scale);
    }

    public static void render(GuiGraphics graphics, int centerX, int centerY,
                              int w, int h, int d, BlockState controller,
                              BlockState casing, @Nullable BlockState coil, float scale) {
        render(graphics, centerX, centerY, w, h, d, controller, casing, coil, StructureEntry.Mode.BOX, null, scale);
    }

    public static void render(GuiGraphics graphics, int centerX, int centerY,
                              int w, int h, int d, BlockState controller,
                              BlockState casing, @Nullable BlockState coil,
                              boolean ebf, float scale) {
        render(graphics, centerX, centerY, w, h, d, controller, casing, coil,
                ebf ? StructureEntry.Mode.TOWER : StructureEntry.Mode.BOX, null, scale);
    }

    public static void render(GuiGraphics graphics, int centerX, int centerY,
                              int w, int h, int d, BlockState controller,
                              BlockState casing, @Nullable BlockState coil,
                              StructureEntry.Mode mode, @Nullable BlockState vent, float scale) {
        int halfW = (w - 1) / 2;
        int halfH = (h - 1) / 2;

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 200);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(30));
        pose.mulPose(Axis.YP.rotationDegrees(225));
        // centre the box on the origin
        pose.translate(-w / 2.0F, -h / 2.0F, -d / 2.0F);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // shell first (skip the controller cell and hollow cells)
        // Anchor (controller) height: bottom layer for the vertical towers/barrel, the
        // TOP layer for the assembly line (like GT's CAL), face centre for plain boxes.
        int anchorY = switch (mode) {
            case TOWER, BARREL, FRAME, DRILL, RIG -> 0;
            case ASSEMBLY -> h - 1;
            default -> halfH; // LOOP (mid layer), BOX, RING, SPHERE
        };
        for (int z = 0; z < d; z++) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean anchor = x == halfW && y == anchorY && z == 0;
                    BlockState cell;
                    switch (mode) {
                        case TOWER -> {
                            // GT vertical EBF: solid base and top layers, coil rings between
                            // them with a hollow centre column.
                            boolean ringLayer = y > 0 && y < h - 1;
                            boolean hollow = ringLayer && x == halfW && z == (d - 1) / 2;
                            cell = hollow ? null : (ringLayer && coil != null ? coil : casing);
                        }
                        case BARREL -> {
                            // GT alloy blast smelter: 5x5 plates top/bottom with corners cut,
                            // coil rings (y1/y3) and a vent ring (y2) around a hollow 3x3x3.
                            boolean corner = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);
                            if (corner) {
                                cell = null;
                            } else if (y == 0 || y == h - 1) {
                                cell = casing;
                            } else {
                                boolean interior = x > 0 && x < w - 1 && z > 0 && z < d - 1;
                                if (interior) {
                                    cell = null;
                                } else if (y == h / 2) {
                                    cell = vent != null ? vent : casing;
                                } else {
                                    cell = coil != null ? coil : casing;
                                }
                            }
                        }
                        case RING -> {
                            // GregTech fusion reactor: render the exact FUSION_PATTERN
                            // (15x3x15 ring). The glass window uses the "vent" accent block.
                            int patternLayer = (y == 1) ? 1 : (y == 0 ? 2 : 0);
                            char c = com.falcon2235.moremultiblock.multiblock.MultiblockValidator
                                    .FUSION_PATTERN[z][patternLayer].charAt(x);
                            cell = switch (c) {
                                case 'G' -> vent != null ? vent : casing;
                                case 'K' -> coil != null ? coil : casing;
                                case 'C', 'E', 'O', 'I' -> casing;
                                default -> null; // '#', 'A', 'S'
                            };
                        }
                        case FRAME -> {
                            // Black hole stabilizer: chunk-sized wireframe cube — neutronium
                            // edges, a glass window at each face centre (vent param), open faces.
                            int kind = com.falcon2235.moremultiblock.multiblock.MultiblockValidator
                                    .stabilizerCell(z, y, x);
                            cell = switch (kind) {
                                case 1 -> casing;
                                case 2 -> vent != null ? vent : casing;
                                default -> null;
                            };
                        }
                        case LOOP -> {
                            // Large hadron collider: flat octagonal tube ring (fusion-style).
                            // kind 1 casing, 2 magnet (coil), 3 glass window (vent).
                            int kind = com.falcon2235.moremultiblock.multiblock.MultiblockValidator
                                    .colliderKind(z, x - w / 2, y);
                            cell = switch (kind) {
                                case 2 -> coil != null ? coil : casing;
                                case 3 -> vent != null ? vent : casing;
                                case 1 -> casing;
                                default -> null;
                            };
                        }
                        case DRILL -> {
                            // Void ore miner: drill rig — base plate, corner legs, glowing
                            // drill mast (coil param), 3x3 crown platform.
                            int kind = com.falcon2235.moremultiblock.multiblock.MultiblockValidator
                                    .voidMinerKind(z, x - w / 2, y);
                            cell = switch (kind) {
                                case 2 -> coil != null ? coil : casing;
                                case 1 -> casing;
                                default -> null;
                            };
                        }
                        case RIG -> {
                            // Oil drilling rig: base plate, corner legs, drill-pipe string
                            // (coil param), 3x3 crown platform.
                            int kind = com.falcon2235.moremultiblock.multiblock.MultiblockValidator
                                    .oilRigKind(z, x - w / 2, y);
                            cell = switch (kind) {
                                case 2 -> coil != null ? coil : casing;
                                case 1 -> casing;
                                default -> null;
                            };
                        }
                        case SPHERE -> {
                            // Artificial star generator: giant rounded cube, corners cut,
                            // hollow core, a glass window (vent param) at each face centre.
                            int half = w / 2;
                            int cd = Math.abs(z - half);
                            int cu = Math.abs(y - half);
                            int cr = Math.abs(x - half);
                            int maxDist = Math.max(cd, Math.max(cu, cr));
                            boolean corner = cd == half && cu == half && cr == half;
                            if (corner || maxDist < half) {
                                cell = null;
                            } else if (com.falcon2235.moremultiblock.multiblock.MultiblockValidator
                                    .isStarWindow(cd, cu, cr, half)) {
                                cell = vent != null ? vent : casing;
                            } else {
                                cell = casing;
                            }
                        }
                        case ASSEMBLY -> {
                            // GTNH circuit assembly line: solid casing top + bottom, glass
                            // side walls (vent param) with a casing spine on the middle layer.
                            boolean middleLayer = y > 0 && y < h - 1;
                            if (middleLayer && (x == 0 || x == w - 1)) {
                                cell = vent != null ? vent : casing;
                            } else {
                                cell = casing;
                            }
                        }
                        default -> {
                            boolean hollow = x > 0 && x < w - 1 && y > 0 && y < h - 1 && z > 0 && z < d - 1;
                            boolean coilCell = coil != null && z > 0 && z < d - 1 && !hollow;
                            cell = hollow ? null : (coilCell ? coil : casing);
                        }
                    }
                    if (anchor || cell == null) {
                        continue;
                    }
                    pose.pushPose();
                    pose.translate(x, y, z);
                    dispatcher.renderSingleBlock(cell, pose, buffers,
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
            }
        }

        // controller last, popped out of the front face (-Z) toward the viewer
        pose.pushPose();
        pose.translate(halfW, anchorY, -CONTROLLER_POP);
        dispatcher.renderSingleBlock(controller, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();

        buffers.endBatch();
        pose.popPose();
    }
}
