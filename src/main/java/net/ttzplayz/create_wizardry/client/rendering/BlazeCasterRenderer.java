package net.ttzplayz.create_wizardry.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.ttzplayz.create_wizardry.client.CWPartialModels;
import net.ttzplayz.create_wizardry.client.CWSpriteShifts;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ttzplayz.create_wizardry.block.blaze_caster.BlazeCasterBlockEntity;

import javax.annotation.Nullable;


public class BlazeCasterRenderer<T extends BlazeCasterBlockEntity> extends SafeBlockEntityRenderer<T> implements PartialModelBlockEntityRenderer {
    public BlazeCasterRenderer(BlockEntityRendererProvider.Context context) {}


    @Override
    protected void renderSafe(T blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        BlazeBurnerBlock.HeatLevel heatLevel = blockEntity.getHeatLevelFromBlock();
        if (heatLevel == BlazeBurnerBlock.HeatLevel.NONE)
            return;
        Level level = blockEntity.getLevel();
        assert level != null;
        float renderTime = AnimationTickHolder.getRenderTime(level);
        BlockState blockState = blockEntity.getBlockState();
        float animation = blockEntity.headAnimation.getValue(partialTicks) * .175f;
        float horizontalAngle = AngleHelper.rad(blockEntity.headAngle.getValue(partialTicks));
        boolean active = blockEntity.isActive();
        int seed = blockEntity.hashCode();
        PartialModel blazeModel = blockEntity.getBlazeModel(heatLevel, active);
        PartialModel hatModel = blockEntity.getHatModel(heatLevel);
        PartialModel gogglesModel = blockEntity.getGogglesModel(heatLevel);
        PartialModel eyesModel = blockEntity.getEyesModel(heatLevel);
        String elementId = blockEntity.getElementId();
        int hatDyeColor = blockEntity.getHatDyeColor();
        PartialModel hatBaseModel = blockEntity.getHatBaseModel(heatLevel);
        PartialModel glowModel = blockEntity.getSuperheatGlowModel(heatLevel,
                active && heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING));
        SpriteShiftEntry elementFlame = CWSpriteShifts.BY_ELEMENT.getOrDefault(elementId, CWSpriteShifts.NONE);
        renderBlaze(
                blockState, heatLevel, renderTime,
                poseStack, null, bufferSource,
                light, overlay, seed,
                animation, horizontalAngle,
                active && heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING),
                blazeModel, hatModel, gogglesModel, eyesModel, elementFlame, elementId, hatDyeColor, hatBaseModel,
                glowModel);
    }

    protected void renderGoggles(
            BlockState blockState, BlazeBurnerBlock.HeatLevel heatLevel, float renderTime,
            PoseStack poseStack, @Nullable PoseStack transformStack, MultiBufferSource bufferSource,
            int light, int overlay, int seed,
            float animation, float horizontalAngle, float headY,
            PartialModel blazeModel, PartialModel gogglesModel) {
        SuperByteBuffer gogglesBuffer = CachedBuffers.partial(gogglesModel, blockState);
        if (transformStack != null)
            gogglesBuffer.transform(transformStack);
        gogglesBuffer.translate(0, headY + .5f, 0);
        RenderType renderType = getRenderType(blockState, gogglesModel);
        draw(gogglesBuffer, horizontalAngle, poseStack, bufferSource.getBuffer(renderType));
    }

    protected void renderHat(
            BlockState blockState, BlazeBurnerBlock.HeatLevel heatLevel, float renderTime,
            PoseStack poseStack, @Nullable PoseStack transformStack, MultiBufferSource bufferSource,
            int light, int overlay, int seed,
            float animation, float horizontalAngle, float headY,
            PartialModel blazeModel, PartialModel hatModel, int dyeColor) {
        SuperByteBuffer hatBuffer = CachedBuffers.partial(hatModel, blockState);
        if (transformStack != null)
            hatBuffer.transform(transformStack);
        hatBuffer.translate(0, headY + 7 / 16f, 0);
        int r = (dyeColor >> 16) & 0xFF;
        int g = (dyeColor >> 8)  & 0xFF;
        int b =  dyeColor        & 0xFF;
        hatBuffer.color(r, g, b, 255);
        RenderType renderType = getRenderType(blockState, hatModel);
        hatBuffer.rotateCentered(horizontalAngle, Direction.UP)
                 .translate(0.5f, 0, 0.5f)
                 .light(LightTexture.FULL_BRIGHT)
                 .renderInto(poseStack, bufferSource.getBuffer(renderType));
    }

    protected void renderEyes(
            BlockState blockState, PoseStack poseStack, @Nullable PoseStack transformStack,
            MultiBufferSource bufferSource, float horizontalAngle, float headY, PartialModel eyesModel) {
        SuperByteBuffer eyesBuffer = CachedBuffers.partial(eyesModel, blockState);
        if (transformStack != null) eyesBuffer.transform(transformStack);
        eyesBuffer.translate(0, headY, 0);
        draw(eyesBuffer, horizontalAngle, poseStack, bufferSource.getBuffer(RenderType.cutoutMipped()));
    }

    public void renderBlaze(
            BlockState blockState, BlazeBurnerBlock.HeatLevel heatLevel, float renderTime,
            PoseStack poseStack, @Nullable PoseStack transformStack, MultiBufferSource bufferSource,
            int light, int overlay, int seed,
            float animation, float horizontalAngle, boolean active,
            PartialModel blazeModel, @Nullable PartialModel hatModel, @Nullable PartialModel gogglesModel,
            @Nullable PartialModel eyesModel,
            @Nullable SpriteShiftEntry elementFlame) {
        renderBlaze(blockState, heatLevel, renderTime, poseStack, transformStack, bufferSource,
                light, overlay, seed, animation, horizontalAngle, active,
                blazeModel, hatModel, gogglesModel, eyesModel, elementFlame, null, 0xFFFFFF);
    }

    public void renderBlaze(
            BlockState blockState, BlazeBurnerBlock.HeatLevel heatLevel, float renderTime,
            PoseStack poseStack, @Nullable PoseStack transformStack, MultiBufferSource bufferSource,
            int light, int overlay, int seed,
            float animation, float horizontalAngle, boolean active,
            PartialModel blazeModel, @Nullable PartialModel hatModel, @Nullable PartialModel gogglesModel,
            @Nullable PartialModel eyesModel,
            @Nullable SpriteShiftEntry elementFlame, @Nullable String elementId) {
        renderBlaze(blockState, heatLevel, renderTime, poseStack, transformStack, bufferSource,
                light, overlay, seed, animation, horizontalAngle, active,
                blazeModel, hatModel, gogglesModel, eyesModel, elementFlame, elementId, 0xFFFFFF, null, null);
    }

    public void renderBlaze(
            BlockState blockState, BlazeBurnerBlock.HeatLevel heatLevel, float renderTime,
            PoseStack poseStack, @Nullable PoseStack transformStack, MultiBufferSource bufferSource,
            int light, int overlay, int seed,
            float animation, float horizontalAngle, boolean active,
            PartialModel blazeModel, @Nullable PartialModel hatModel, @Nullable PartialModel gogglesModel,
            @Nullable PartialModel eyesModel,
            @Nullable SpriteShiftEntry elementFlame, @Nullable String elementId, int hatDyeColor) {
        renderBlaze(blockState, heatLevel, renderTime, poseStack, transformStack, bufferSource,
                light, overlay, seed, animation, horizontalAngle, active,
                blazeModel, hatModel, gogglesModel, eyesModel, elementFlame, elementId, hatDyeColor, null, null);
    }

    public void renderBlaze(
            BlockState blockState, BlazeBurnerBlock.HeatLevel heatLevel, float renderTime,
            PoseStack poseStack, @Nullable PoseStack transformStack, MultiBufferSource bufferSource,
            int light, int overlay, int seed,
            float animation, float horizontalAngle, boolean active,
            PartialModel blazeModel, @Nullable PartialModel hatModel, @Nullable PartialModel gogglesModel,
            @Nullable PartialModel eyesModel,
            @Nullable SpriteShiftEntry elementFlame, @Nullable String elementId, int hatDyeColor,
            @Nullable PartialModel hatBaseModel, @Nullable PartialModel glowModel) {
        float seededRenderTime = renderTime + (seed % 13) * 16f;
        float offsetScale = heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) ? 64 : 16;
        float offset = Mth.sin((seededRenderTime / 16f) % (2 * Mth.PI)) / offsetScale;
        float rodsOffset1 = Mth.sin((seededRenderTime / 16f + Mth.PI) % (2 * Mth.PI)) / offsetScale;
        float rodsOffset2 = Mth.sin((seededRenderTime / 16f + Mth.PI / 2) % (2 * Mth.PI)) / offsetScale;
        float headY = offset + (animation * 1.5f);

        poseStack.pushPose();
        // Blaze Head
        SuperByteBuffer blazeBuffer = CachedBuffers.partial(blazeModel, blockState);
        if (transformStack != null)
            blazeBuffer.transform(transformStack);
        blazeBuffer.translate(0, headY, 0);
        draw(blazeBuffer, horizontalAngle, poseStack, bufferSource.getBuffer(RenderType.solid()));
        // Eyes overlay
        if (eyesModel != null)
            renderEyes(blockState, poseStack, transformStack, bufferSource, horizontalAngle, headY, eyesModel);
        // Superheat "black hole" glow overlay (emissive, translucent)
        if (glowModel != null) {
            SuperByteBuffer glowBuffer = CachedBuffers.partial(glowModel, blockState);
            if (transformStack != null) glowBuffer.transform(transformStack);
            glowBuffer.translate(0, headY, 0);
            draw(glowBuffer, horizontalAngle, poseStack, bufferSource.getBuffer(RenderType.translucent()));
        }
        // Goggles
        if (gogglesModel != null)
            renderGoggles(
                    blockState, heatLevel, renderTime,
                    poseStack, transformStack, bufferSource,
                    light, overlay, seed,
                    animation, horizontalAngle, headY,
                    blazeModel, gogglesModel);
        // Hat (dyeable cloth parts, tinted with the dye color)
        if (hatModel != null)
            renderHat(
                    blockState, heatLevel, renderTime,
                    poseStack, transformStack, bufferSource,
                    light, overlay, seed,
                    animation, horizontalAngle, headY,
                    blazeModel, hatModel, hatDyeColor);
        // hat base, untinted metal parts
        if (hatBaseModel != null)
            renderHat(
                    blockState, heatLevel, renderTime,
                    poseStack, transformStack, bufferSource,
                    light, overlay, seed,
                    animation, horizontalAngle, headY,
                    blazeModel, hatBaseModel, 0xFFFFFF);
        // Blaze Rods
        if (heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) {
            PartialModel rodsModel = heatLevel == BlazeBurnerBlock.HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS
                    : CWPartialModels.ROD_SMALL_BY_ELEMENT.getOrDefault(elementId, AllPartialModels.BLAZE_BURNER_RODS);
            PartialModel rodsModel2 = heatLevel == BlazeBurnerBlock.HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS_2
                    : CWPartialModels.ROD_LARGE_BY_ELEMENT.getOrDefault(elementId, AllPartialModels.BLAZE_BURNER_RODS_2);

            SuperByteBuffer rodsBuffer = CachedBuffers.partial(rodsModel, blockState);
            if (transformStack != null)
                rodsBuffer.transform(transformStack);
            rodsBuffer.translate(0, rodsOffset1 + animation + .125f, 0)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));

            SuperByteBuffer rodsBuffer2 = CachedBuffers.partial(rodsModel2, blockState);
            if (transformStack != null)
                rodsBuffer2.transform(transformStack);
            rodsBuffer2.translate(0, rodsOffset2 + animation - 3 / 16f, 0)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));
        }
        // Blaze Flame
        if (active) {
            SpriteShiftEntry spriteShift = elementFlame != null ? elementFlame :
                    (heatLevel == BlazeBurnerBlock.HeatLevel.SEETHING
                            ? AllSpriteShifts.SUPER_BURNER_FLAME
                            : AllSpriteShifts.BURNER_FLAME);

            float spriteWidth = spriteShift.getTarget().getU1() - spriteShift.getTarget().getU0();

            float spriteHeight = spriteShift.getTarget().getV1() - spriteShift.getTarget().getV0();

            float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();

            float uScroll = speed * renderTime / 2;
            uScroll -= Mth.floor(uScroll);
            uScroll *= spriteWidth / 2;

            float vScroll = speed * renderTime;
            vScroll -= Mth.floor(vScroll);
            vScroll *= spriteHeight / 2;

            SuperByteBuffer flameBuffer = CachedBuffers.partial(AllPartialModels.BLAZE_BURNER_FLAME, blockState);
            if (transformStack != null)
                flameBuffer.transform(transformStack);
            flameBuffer.shiftUVScrolling(spriteShift, uScroll, vScroll);

            VertexConsumer cutout = bufferSource.getBuffer(RenderType.cutoutMipped());
            draw(flameBuffer, horizontalAngle, poseStack, cutout);
        }

        poseStack.popPose();
    }

    protected static void draw(SuperByteBuffer buffer, float horizontalAngle, PoseStack poseStack, VertexConsumer vertexConsumer) {
        buffer.rotateCentered(horizontalAngle, Direction.UP)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(poseStack, vertexConsumer);
    }

    protected static void drawCentered(SuperByteBuffer buffer, float horizontalAngle, PoseStack poseStack, VertexConsumer vertexConsumer) {
        buffer.rotateCentered(horizontalAngle, Direction.UP)
                .translate(0.5f, 0, 0.5f)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(poseStack, vertexConsumer);
    }
}