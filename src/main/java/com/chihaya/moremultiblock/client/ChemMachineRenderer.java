package com.chihaya.moremultiblock.client;

import com.chihaya.moremultiblock.MekanismMoreMultiblock;
import com.chihaya.moremultiblock.block.ChemMachineBlock;
import com.chihaya.moremultiblock.blockentity.ChemMachineBlockEntity;
import com.chihaya.moremultiblock.machine.ChemMachineType;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders a non-colliding visual inside the two star-scale machines while they run:
 * a glowing star at the centre of the Artificial Star Generator, and a spinning black
 * hole (dark core + accretion disk) at the centre of the Black Hole Stabilizer. Both are
 * camera-facing billboards drawn at full brightness; they have no hitbox or block state.
 */
public class ChemMachineRenderer implements BlockEntityRenderer<ChemMachineBlockEntity> {

    private static final ResourceLocation STAR =
            new ResourceLocation(MekanismMoreMultiblock.MODID, "textures/effect/star.png");
    private static final ResourceLocation BLACK_HOLE =
            new ResourceLocation(MekanismMoreMultiblock.MODID, "textures/effect/black_hole.png");

    public ChemMachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ChemMachineBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (be.getLevel() == null || !be.isEffectActive()) {
            return;
        }
        ChemMachineType type = be.machineType();
        boolean star = type == ChemMachineType.STAR_GENERATOR;
        if (!star && type != ChemMachineType.STABILIZER) {
            return;
        }

        Direction facing = be.getBlockState().getValue(ChemMachineBlock.FACING);
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Vec3 centre = star
                // 9x9x9: centre is 4 blocks back, at the controller's height
                ? new Vec3(back.getStepX() * 4 + 0.5, 0.5, back.getStepZ() * 4 + 0.5)
                // 16x16x16: centre is ~8 back, ~8 up, half a block right of the controller
                : new Vec3(back.getStepX() * 8 + right.getStepX() * 0.5 + 0.5, 8.0,
                        back.getStepZ() * 8 + right.getStepZ() * 0.5 + 0.5);

        float time = (float) (be.getLevel().getGameTime() % 720720L) + partialTick;

        pose.pushPose();
        pose.translate(centre.x, centre.y, centre.z);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        if (star) {
            float pulse = 1.0F + 0.06F * (float) Math.sin(time * 0.12F);
            // faint corona, then bright core
            drawBillboard(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(STAR)),
                    3.6F * pulse, time * 0.3F, 255, 210, 120, 90);
            drawBillboard(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(STAR)),
                    2.3F * pulse, -time * 0.5F, 255, 246, 214, 255);
        } else {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(BLACK_HOLE));
            // outer accretion glow + the disk itself, counter-rotating for a swirl
            drawBillboard(pose, vc, 5.2F, time * 1.1F, 150, 90, 200, 120);
            drawBillboard(pose, vc, 4.0F, -time * 2.2F, 255, 255, 255, 255);
        }

        pose.popPose();
    }

    /** Draws a camera-facing quad of half-size {@code s}, spun {@code roll} degrees, full-bright. */
    private static void drawBillboard(PoseStack pose, VertexConsumer vc, float s, float roll,
                                      int r, int g, int b, int a) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        Matrix4f m = pose.last().pose();
        int light = LightTexture.FULL_BRIGHT;
        quad(vc, m, -s, -s, 0.0F, 1.0F, r, g, b, a, light);
        quad(vc, m, s, -s, 1.0F, 1.0F, r, g, b, a, light);
        quad(vc, m, s, s, 1.0F, 0.0F, r, g, b, a, light);
        quad(vc, m, -s, s, 0.0F, 0.0F, r, g, b, a, light);
        pose.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x, float y, float u, float v,
                             int r, int g, int b, int a, int light) {
        vc.vertex(m, x, y, 0.0F).color(r, g, b, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0.0F, 0.0F, 1.0F).endVertex();
    }
}
