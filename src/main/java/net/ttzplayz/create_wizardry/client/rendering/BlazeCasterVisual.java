package net.ttzplayz.create_wizardry.client.rendering;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.Translate;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.ttzplayz.create_wizardry.block.blaze_caster.BlazeCasterBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class BlazeCasterVisual extends AbstractBlockEntityVisual<BlazeCasterBlockEntity>
        implements SimpleDynamicVisual, SimpleTickableVisual {

    private BlazeBurnerBlock.HeatLevel heatLevel;
    private final TransformedInstance head;
    @Nullable private TransformedInstance hat;
    @Nullable private TransformedInstance eyes;
    @Nullable private TransformedInstance smallRods;
    @Nullable private TransformedInstance largeRods;
    @Nullable private ScrollInstance flame;
    @Nullable private String currentElement;

    public BlazeCasterVisual(VisualizationContext ctx, BlazeCasterBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        heatLevel = blockEntity.getHeatLevelFromBlock();
        boolean active = blockEntity.headAnimation.getValue(partialTick) * .175f > 0.125f;

        head = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(blockEntity.getBlazeModel(heatLevel, active)))
                .createInstance();
        head.light(LightTexture.FULL_BRIGHT);

        PartialModel hatModel = blockEntity.getHatModel(heatLevel);
        if (hatModel != null) {
            hat = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(hatModel))
                    .createInstance();
            hat.light(LightTexture.FULL_BRIGHT);
        }

        PartialModel eyesModel = blockEntity.getEyesModel(heatLevel);
        if (eyesModel != null) {
            eyes = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(eyesModel))
                    .createInstance();
            eyes.light(LightTexture.FULL_BRIGHT);
        }

        if (heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) {
            smallRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.BLAZE_BURNER_RODS))
                    .createInstance();
            smallRods.light(LightTexture.FULL_BRIGHT);
            largeRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.BLAZE_BURNER_RODS_2))
                    .createInstance();
            largeRods.light(LightTexture.FULL_BRIGHT);
        }

        currentElement = blockEntity.getElementId();
        animate(partialTick);
    }

    @Override
    public void tick(TickableVisual.Context context) {
        blockEntity.tickAnimation();
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        if (!isVisible(ctx.frustum()) || doDistanceLimitThisFrame(ctx))
            return;

        BlazeBurnerBlock.HeatLevel newHeatLevel = blockEntity.getHeatLevelFromBlock();
        float animation = blockEntity.headAnimation.getValue(ctx.partialTick()) * .175f;
        boolean active = animation > 0.125f;

        String newElement = blockEntity.getElementId();
        boolean heatOrElementChanged = newHeatLevel != heatLevel
                || !Objects.equals(newElement, currentElement);

        if (heatOrElementChanged) {
            heatLevel = newHeatLevel;
            currentElement = newElement;

            instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(blockEntity.getBlazeModel(heatLevel, active)))
                    .stealInstance(head);

            PartialModel hatModel = blockEntity.getHatModel(heatLevel);
            if (hatModel != null) {
                if (hat == null) {
                    hat = instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, Models.partial(hatModel))
                            .createInstance();
                    hat.light(LightTexture.FULL_BRIGHT);
                } else {
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, Models.partial(hatModel))
                            .stealInstance(hat);
                }
            } else if (hat != null) {
                hat.delete();
                hat = null;
            }

            if (eyes != null) {
                eyes.delete();
                eyes = null;
            }
        }

        // Eyes lifecycle
        PartialModel eyesModel = blockEntity.getEyesModel(newHeatLevel);
        if (eyesModel != null && eyes == null) {
            eyes = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(eyesModel))
                    .createInstance();
            eyes.light(LightTexture.FULL_BRIGHT);
        } else if (eyesModel == null && eyes != null) {
            eyes.delete();
            eyes = null;
        }

        // Rod lifecycle — created at FADING+, removed below FADING
        if (newHeatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && smallRods == null) {
            smallRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.BLAZE_BURNER_RODS))
                    .createInstance();
            smallRods.light(LightTexture.FULL_BRIGHT);
            largeRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.BLAZE_BURNER_RODS_2))
                    .createInstance();
            largeRods.light(LightTexture.FULL_BRIGHT);
        } else if (!newHeatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && smallRods != null) {
            smallRods.delete(); smallRods = null;
            if (largeRods != null) { largeRods.delete(); largeRods = null; }
        }

        animate(ctx.partialTick());
    }

    private void animate(float partialTicks) {
        float animation = blockEntity.headAnimation.getValue(partialTicks) * .175f;
        boolean active = animation > 0.125f;
        float renderTick = AnimationTickHolder.getRenderTime(level) + (blockEntity.hashCode() % 13) * 16f;
        float offsetMult = heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) ? 64 : 16;
        float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMult;
        float headY = offset - (animation * .75f);
        float horizontalAngle = AngleHelper.rad(blockEntity.headAngle.getValue(partialTicks));

        head.setIdentityTransform()
                .translate(getVisualPosition())
                .translateY(headY)
                .translate(Translate.CENTER)
                .rotateY(horizontalAngle)
                .translateBack(Translate.CENTER)
                .setChanged();

        if (hat != null) {
            hat.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translateY(headY)
                    .translateY(.75f);
            hat.rotateCentered(horizontalAngle + Mth.PI, Direction.UP)
                    .translate(0.5f, 0, 0.5f)
                    .light(LightTexture.FULL_BRIGHT);
            hat.setChanged();
        }

        if (eyes != null) {
            eyes.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translateY(headY)
                    .translate(Translate.CENTER)
                    .rotateY(horizontalAngle)
                    .translateBack(Translate.CENTER)
                    .setChanged();
        }

        if (smallRods != null) {
            float offset1 = Mth.sin((float) ((renderTick / 16f + Math.PI) % (2 * Math.PI))) / offsetMult;
            smallRods.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translateY(offset1 + animation + .125f)
                    .setChanged();
        }

        if (largeRods != null) {
            float offset2 = Mth.sin((float) ((renderTick / 16f + Math.PI / 2) % (2 * Math.PI))) / offsetMult;
            largeRods.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translateY(offset2 + animation - 3 / 16f)
                    .setChanged();
        }

        // Flame lifecycle — shown only when animation threshold is met (head animation active)
        if (active && flame == null) {
            setupFlame();
        } else if (!active && flame != null) {
            flame.delete();
            flame = null;
        }
    }

    private void setupFlame() {
        flame = instancerProvider()
                .instancer(AllInstanceTypes.SCROLLING, Models.partial(AllPartialModels.BLAZE_BURNER_FLAME))
                .createInstance();
        flame.position(getVisualPosition()).light(LightTexture.FULL_BRIGHT);

        SpriteShiftEntry spriteShift = AllSpriteShifts.BURNER_FLAME;
        float spriteWidth  = spriteShift.getTarget().getU1() - spriteShift.getTarget().getU0();
        float spriteHeight = spriteShift.getTarget().getV1() - spriteShift.getTarget().getV0();
        float speed = 1 / 32f + 1 / 64f * BlazeBurnerBlock.HeatLevel.FADING.ordinal();
        flame.speedU = speed / 2;
        flame.speedV = speed;
        flame.scaleU = spriteWidth  / 2;
        flame.scaleV = spriteHeight / 2;
        flame.diffU = spriteShift.getTarget().getU0() - spriteShift.getOriginal().getU0();
        flame.diffV = spriteShift.getTarget().getV0() - spriteShift.getOriginal().getV0();
    }

    @Override
    public void updateLight(float partialTick) {}

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {}

    @Override
    protected void _delete() {
        head.delete();
        if (hat != null) hat.delete();
        if (eyes != null) eyes.delete();
        if (smallRods != null) smallRods.delete();
        if (largeRods != null) largeRods.delete();
        if (flame != null) flame.delete();
    }
}
