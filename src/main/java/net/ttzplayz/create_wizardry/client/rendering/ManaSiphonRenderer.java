package net.ttzplayz.create_wizardry.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ttzplayz.create_wizardry.block.mana_siphon.ManaSiphonBlockEntity;
import net.ttzplayz.create_wizardry.client.CWPartialModels;

// fallback renderer (used when Flywheel's instancing backend is off)
public class ManaSiphonRenderer extends SafeBlockEntityRenderer<ManaSiphonBlockEntity>
        implements PartialModelBlockEntityRenderer {

    // kept in sync with ManaSiphonVisual
    private static final float OUTWARD_DEGREES = -22.5f;
    private static final float HINGE_X = 0.5f;
    private static final float HINGE_Y = 14f / 16f;
    private static final float HINGE_Z = 6f / 16f;

    public ManaSiphonRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(ManaSiphonBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                              MultiBufferSource bufferSource, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();

        PartialModel wheelModel = CWPartialModels.MANA_SIPHON_WHEEL;
        float angle = KineticBlockEntityRenderer.getAngleForBe(blockEntity, blockEntity.getBlockPos(), Direction.Axis.Y);
        SuperByteBuffer wheel = CachedBuffers.partial(wheelModel, state);
        RenderType wheelType = getRenderType(state, wheelModel);
        wheel.rotateCentered(angle, Direction.UP)
                .light(light)
                .renderInto(poseStack, bufferSource.getBuffer(wheelType));

        PartialModel prongModel = CWPartialModels.MANA_SIPHON_PRONG;
        RenderType prongType = getRenderType(state, prongModel);
        float tilt = OUTWARD_DEGREES * blockEntity.prongAnimation.getValue(partialTicks);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(90f * i));
            poseStack.translate(-0.5, -0.5, -0.5);
            poseStack.translate(HINGE_X, HINGE_Y, HINGE_Z);
            poseStack.mulPose(Axis.XP.rotationDegrees(tilt));
            poseStack.translate(-HINGE_X, -HINGE_Y, -HINGE_Z);
            CachedBuffers.partial(prongModel, state)
                    .light(light)
                    .renderInto(poseStack, bufferSource.getBuffer(prongType));
            poseStack.popPose();
        }
    }
}
