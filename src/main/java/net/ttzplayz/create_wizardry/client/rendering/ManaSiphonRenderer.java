package net.ttzplayz.create_wizardry.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
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

// fallback renderer
public class ManaSiphonRenderer extends SafeBlockEntityRenderer<ManaSiphonBlockEntity>
        implements PartialModelBlockEntityRenderer {

    public ManaSiphonRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(ManaSiphonBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                              MultiBufferSource bufferSource, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        PartialModel model = CWPartialModels.MANA_SIPHON_WHEEL;
        float angle = KineticBlockEntityRenderer.getAngleForBe(blockEntity, blockEntity.getBlockPos(), Direction.Axis.Y);
        SuperByteBuffer wheel = CachedBuffers.partial(model, state);
        RenderType renderType = getRenderType(state, model);
        wheel.rotateCentered(angle, Direction.UP)
                .light(light)
                .renderInto(poseStack, bufferSource.getBuffer(renderType));
    }
}
