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
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
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

    // Inverted-hull outline: the glow cube model is inverted (from/to swapped) so its near faces are
    // culled and only the far faces render. Drawn OPAQUE, the front-facing head occludes the centre
    // and the magenta far faces peek out around the silhouette as a solid, gap-free colour outline.
    // (Translucent blending washed the flat faces out to thin edges — that was the old bug.)
    // diffuse(false): the base render type applies directional diffuse shading, which darkens the
    // inverted faces unevenly (a "shadow"); a flat glow outline must be uniformly lit on every face.
    private static final RendererReloadCache<PartialModel, Model> GLOW_MODELS =
            new RendererReloadCache<>(partial -> new BakedModelBuilder(partial.get())
                    .materialFunc((renderType, shaded) -> {
                        Material base = ModelUtil.getMaterial(renderType, shaded);
                        return base == null ? null
                                : new SimpleMaterial.Builder().copyFrom(base)
                                        .transparency(Transparency.OPAQUE)
                                        .writeMask(WriteMask.COLOR_DEPTH)
                                        .backfaceCulling(true)
                                        .diffuse(false)
                                        .build();
                    })
                    .build());

    private static Model glowPartial(PartialModel partial) {
        return GLOW_MODELS.get(partial);
    }

    private BlazeBurnerBlock.HeatLevel heatLevel;
    private final TransformedInstance head;
    @Nullable private TransformedInstance hat;
    @Nullable private TransformedInstance hatBase;
    @Nullable private TransformedInstance eyes;
    @Nullable private TransformedInstance glow;
    @Nullable private TransformedInstance smallRods;
    @Nullable private TransformedInstance largeRods;
    @Nullable private TransformedInstance glowSmallRods;
    @Nullable private TransformedInstance glowLargeRods;
    @Nullable private ScrollInstance flame;
    @Nullable private String currentElement;
    @Nullable private PartialModel currentHeadModel;
    @Nullable private PartialModel currentHatModel;
    @Nullable private PartialModel currentHatBaseModel;
    @Nullable private PartialModel currentGlowModel;
    @Nullable private PartialModel currentRodSmallGlowModel;
    @Nullable private PartialModel currentRodLargeGlowModel;
    private boolean superheatedState;

    public BlazeCasterVisual(VisualizationContext ctx, BlazeCasterBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        heatLevel = blockEntity.getHeatLevelFromBlock();
        boolean active = blockEntity.headAnimation.getValue(partialTick) * .175f > 0.125f;

        currentHeadModel = blockEntity.getBlazeModel(heatLevel, active);
        head = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(currentHeadModel))
                .createInstance();
        head.light(LightTexture.FULL_BRIGHT);

        PartialModel hatModel = blockEntity.getHatModel(heatLevel);
        currentHatModel = hatModel;
        if (hatModel != null) {
            hat = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatModel))
                    .createInstance();
            hat.light(LightTexture.FULL_BRIGHT);
        }

        PartialModel hatBaseModel = blockEntity.getHatBaseModel(heatLevel);
        currentHatBaseModel = hatBaseModel;
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
        superheatedState = blockEntity.isSuperheated();

        PartialModel glowModel = blockEntity.getSuperheatGlowModel(heatLevel, active);
        currentGlowModel = glowModel;
        if (glowModel != null) {
            glow = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, glowPartial(glowModel))
                    .createInstance();
            glow.light(LightTexture.FULL_BRIGHT);
        }

        if (heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) {
            smallRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(
                        blockEntity.getRodSmallModel()))
                    .createInstance();
            smallRods.light(LightTexture.FULL_BRIGHT);
            largeRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(
                        blockEntity.getRodLargeModel()))
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
        boolean newSuperheated = blockEntity.isSuperheated();
        boolean heatOrElementChanged = newHeatLevel != heatLevel
                || !Objects.equals(newElement, currentElement)
                || newSuperheated != superheatedState;

        if (heatOrElementChanged) {
            heatLevel = newHeatLevel;
            currentElement = newElement;
            superheatedState = newSuperheated;

            // Hat/hatBase are not driven by heat/element/superheat — their models are handled by the
            // model-aware lifecycle below, which also catches a swap between two different hats.

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

        // Hat lifecycle: compare the model every frame so swapping one hat for a *different* hat
        // (same heat/element) is detected, not just none<->hat. stealInstance swaps the model in
        // place without dropping the instance's transform.
        PartialModel hatModel = blockEntity.getHatModel(newHeatLevel);
        if (!Objects.equals(hatModel, currentHatModel)) {
            currentHatModel = hatModel;
            if (hatModel == null) {
                if (hat != null) { hat.delete(); hat = null; }
            } else if (hat == null) {
                hat = instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatModel))
                        .createInstance();
                hat.light(LightTexture.FULL_BRIGHT);
            } else {
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatModel))
                        .stealInstance(hat);
            }
        }

        // Hat base lifecycle (non-dyeable metal parts) — same model-aware handling
        PartialModel hatBaseModel = blockEntity.getHatBaseModel(newHeatLevel);
        if (!Objects.equals(hatBaseModel, currentHatBaseModel)) {
            currentHatBaseModel = hatBaseModel;
            if (hatBaseModel == null) {
                if (hatBase != null) { hatBase.delete(); hatBase = null; }
            } else if (hatBase == null) {
                hatBase = instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatBaseModel))
                        .createInstance();
                hatBase.light(LightTexture.FULL_BRIGHT);
            } else {
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, noCullPartial(hatBaseModel))
                        .stealInstance(hatBase);
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

        // Head lifecycle: re-fetch every frame so the head swaps on idle<->active (e.g. the
        // superheated ender/blood casting textures), not just on heat/element/superheat changes.
        PartialModel headModel = blockEntity.getBlazeModel(newHeatLevel, active);
        if (!Objects.equals(headModel, currentHeadModel)) {
            currentHeadModel = headModel;
            instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(headModel))
                    .stealInstance(head);
        }

        // Superheat glow lifecycle (recreate when the model changes: on/off or idle<->active)
        PartialModel glowModel = blockEntity.getSuperheatGlowModel(newHeatLevel, active);
        if (!Objects.equals(glowModel, currentGlowModel)) {
            if (glow != null) { glow.delete(); glow = null; }
            currentGlowModel = glowModel;
            if (glowModel != null) {
                glow = instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, glowPartial(glowModel))
                        .createInstance();
                glow.light(LightTexture.FULL_BRIGHT);
            }
        }

        // rod lifecycle, created at FADING+
        if (newHeatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && smallRods == null) {
            smallRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(
                        blockEntity.getRodSmallModel()))
                    .createInstance();
            smallRods.light(LightTexture.FULL_BRIGHT);
            largeRods = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(
                        blockEntity.getRodLargeModel()))
                    .createInstance();
            largeRods.light(LightTexture.FULL_BRIGHT);
        } else if (!newHeatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && smallRods != null) {
            smallRods.delete(); smallRods = null;
            if (largeRods != null) { largeRods.delete(); largeRods = null; }
        }

        // Rod glow-outline lifecycle (ender superheat only), tied to the rods existing. Recreated on
        // model change (on/off or idle<->active), mirroring the head glow lifecycle.
        PartialModel rodSmallGlow = smallRods != null ? blockEntity.getRodSmallGlowModel(newHeatLevel, active) : null;
        if (!Objects.equals(rodSmallGlow, currentRodSmallGlowModel)) {
            if (glowSmallRods != null) { glowSmallRods.delete(); glowSmallRods = null; }
            currentRodSmallGlowModel = rodSmallGlow;
            if (rodSmallGlow != null) {
                glowSmallRods = instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, glowPartial(rodSmallGlow))
                        .createInstance();
                glowSmallRods.light(LightTexture.FULL_BRIGHT);
            }
        }

        PartialModel rodLargeGlow = largeRods != null ? blockEntity.getRodLargeGlowModel(newHeatLevel, active) : null;
        if (!Objects.equals(rodLargeGlow, currentRodLargeGlowModel)) {
            if (glowLargeRods != null) { glowLargeRods.delete(); glowLargeRods = null; }
            currentRodLargeGlowModel = rodLargeGlow;
            if (rodLargeGlow != null) {
                glowLargeRods = instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, glowPartial(rodLargeGlow))
                        .createInstance();
                glowLargeRods.light(LightTexture.FULL_BRIGHT);
            }
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

        if (glow != null) {
            glow.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translateY(headY)
                    .translate(Translate.CENTER)
                    .rotateY(horizontalAngle)
                    .translateBack(Translate.CENTER)
                    .setChanged();
        }

        if (smallRods != null) {
            float offset1 = Mth.sin((float) ((renderTick / 16f + Math.PI) % (2 * Math.PI))) / offsetMult;
            float smallY = offset1 + animation + .125f;
            smallRods.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translateY(smallY)
                    .setChanged();
            if (glowSmallRods != null) {
                // Glow geometry is already inverted+inflated per rod (like the head glow), so it
                // uses the exact same transform as the rods — no scaling, so it stays aligned.
                glowSmallRods.setIdentityTransform()
                        .translate(getVisualPosition())
                        .translateY(smallY)
                        .light(LightTexture.FULL_BRIGHT)
                        .setChanged();
            }
        }

        if (largeRods != null) {
            float offset2 = Mth.sin((float) ((renderTick / 16f + Math.PI / 2) % (2 * Math.PI))) / offsetMult;
            float largeY = offset2 + animation - 3 / 16f;
            largeRods.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translateY(largeY)
                    .setChanged();
            if (glowLargeRods != null) {
                glowLargeRods.setIdentityTransform()
                        .translate(getVisualPosition())
                        .translateY(largeY)
                        .light(LightTexture.FULL_BRIGHT)
                        .setChanged();
            }
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
        if (glow != null) glow.delete();
        if (smallRods != null) smallRods.delete();
        if (largeRods != null) largeRods.delete();
        if (glowSmallRods != null) glowSmallRods.delete();
        if (glowLargeRods != null) glowLargeRods.delete();
        if (flame != null) flame.delete();
    }
}
