package net.ttzplayz.create_wizardry.client.rendering;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.ttzplayz.create_wizardry.client.CWPartialModels;
import net.ttzplayz.create_wizardry.client.CWSpriteShifts;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
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

    // re-bake single-sided hat obj with culling off; cached per partial
    private static final RendererReloadCache<PartialModel, Model> NO_CULL_MODELS =
            new RendererReloadCache<>(partial -> new BakedModelBuilder(partial.get())
                    .materialFunc((renderType, shaded) -> {
                        Material base = ModelUtil.getMaterial(renderType, shaded);
                        return base == null ? null
                                : new SimpleMaterial.Builder().copyFrom(base).backfaceCulling(false).build();
                    })
                    .build());

    private static Model noCullPartial(PartialModel partial) {
        return NO_CULL_MODELS.get(partial);
    }

    private BlazeBurnerBlock.HeatLevel heatLevel;
    private final TransformedInstance head;
    @Nullable private TransformedInstance hat;
    @Nullable private TransformedInstance hatBase;
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
                    .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatModel))
                    .createInstance();
            hat.light(LightTexture.FULL_BRIGHT);
        }

        PartialModel hatBaseModel = blockEntity.getHatBaseModel(heatLevel);
        if (hatBaseModel != null) {
            hatBase = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatBaseModel))
                    .createInstance();
            hatBase.light(LightTexture.FULL_BRIGHT);
        }

        PartialModel eyesModel = blockEntity.getEyesModel(heatLevel);
        if (eyesModel != null) {
            eyes = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(eyesModel))
                    .createInstance();
            eyes.light(LightTexture.FULL_BRIGHT);
        }

        currentElement = blockEntity.getElementId();

        if (heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) {
            smallRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(
                        CWPartialModels.ROD_SMALL_BY_ELEMENT.getOrDefault(currentElement, AllPartialModels.BLAZE_BURNER_RODS)))
                    .createInstance();
            smallRods.light(LightTexture.FULL_BRIGHT);
            largeRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(
                        CWPartialModels.ROD_LARGE_BY_ELEMENT.getOrDefault(currentElement, AllPartialModels.BLAZE_BURNER_RODS_2)))
                    .createInstance();
            largeRods.light(LightTexture.FULL_BRIGHT);
        }
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
                            .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatModel))
                            .createInstance();
                    hat.light(LightTexture.FULL_BRIGHT);
                } else {
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatModel))
                            .stealInstance(hat);
                }
            } else if (hat != null) {
                hat.delete();
                hat = null;
            }

            PartialModel hatBaseModel = blockEntity.getHatBaseModel(heatLevel);
            if (hatBaseModel != null) {
                if (hatBase == null) {
                    hatBase = instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatBaseModel))
                            .createInstance();
                    hatBase.light(LightTexture.FULL_BRIGHT);
                } else {
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatBaseModel))
                            .stealInstance(hatBase);
                }
            } else if (hatBase != null) {
                hatBase.delete();
                hatBase = null;
            }

            if (eyes != null) {
                eyes.delete();
                eyes = null;
            }

            // Rods must be recreated with the new element's model
            if (smallRods != null) { smallRods.delete(); smallRods = null; }
            if (largeRods != null) { largeRods.delete(); largeRods = null; }

            // Flame must be recreated with the new element's sprite shift
            if (flame != null) {
                flame.delete();
                flame = null;
            }
        }

        // hat lifecycle
        PartialModel currentHatModel = blockEntity.getHatModel(newHeatLevel);
        if (currentHatModel != null && hat == null) {
            hat = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, noCullPartial(currentHatModel))
                    .createInstance();
            hat.light(LightTexture.FULL_BRIGHT);
        } else if (currentHatModel == null && hat != null) {
            hat.delete();
            hat = null;
        }

        // Hat base lifecycle (non-dyeable metal parts)
        PartialModel currentHatBaseModel = blockEntity.getHatBaseModel(newHeatLevel);
        if (currentHatBaseModel != null && hatBase == null) {
            hatBase = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, noCullPartial(currentHatBaseModel))
                    .createInstance();
            hatBase.light(LightTexture.FULL_BRIGHT);
        } else if (currentHatBaseModel == null && hatBase != null) {
            hatBase.delete();
            hatBase = null;
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

        // rod lifecycle, created at FADING+
        if (newHeatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && smallRods == null) {
            smallRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(
                        CWPartialModels.ROD_SMALL_BY_ELEMENT.getOrDefault(newElement, AllPartialModels.BLAZE_BURNER_RODS)))
                    .createInstance();
            smallRods.light(LightTexture.FULL_BRIGHT);
            largeRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(
                        CWPartialModels.ROD_LARGE_BY_ELEMENT.getOrDefault(newElement, AllPartialModels.BLAZE_BURNER_RODS_2)))
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
        float headY = offset + (animation * 1.5f);
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
                    .translateY(headY + 7 / 16f);
            hat.rotateCentered(horizontalAngle, Direction.UP)
                    .translate(0.5f, 0, 0.5f)
                    .light(LightTexture.FULL_BRIGHT)
                    .colorRgb(blockEntity.getHatDyeColor());
            hat.setChanged();
        }

        if (hatBase != null) {
            hatBase.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translateY(headY + 7 / 16f);
            hatBase.rotateCentered(horizontalAngle, Direction.UP)
                    .translate(0.5f, 0, 0.5f)
                    .light(LightTexture.FULL_BRIGHT)
                    .colorRgb(0xFFFFFF);
            hatBase.setChanged();
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

        // flame lifecycle, shown above anim threshold
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

        SpriteShiftEntry spriteShift = CWSpriteShifts.BY_ELEMENT.getOrDefault(
                blockEntity.getElementId(), CWSpriteShifts.NONE);
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
        if (hatBase != null) hatBase.delete();
        if (eyes != null) eyes.delete();
        if (smallRods != null) smallRods.delete();
        if (largeRods != null) largeRods.delete();
        if (flame != null) flame.delete();
    }
}
