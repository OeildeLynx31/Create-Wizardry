package net.ttzplayz.create_wizardry.client.rendering;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.Translate;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.Direction;
import net.ttzplayz.create_wizardry.block.mana_siphon.ManaSiphonBlockEntity;
import net.ttzplayz.create_wizardry.client.CWPartialModels;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


public class ManaSiphonVisual extends AbstractBlockEntityVisual<ManaSiphonBlockEntity>
        implements SimpleDynamicVisual {

    /** Max outward splay of each prong when fully expanded. Sign points the prong tips outward+down. */
    private static final float OUTWARD_DEGREES = -22.5f;
    /** Hinge of the (canonical, north-facing) prong: the inner-top edge where it meets the cap. */
    private static final float HINGE_X = 0.5f;
    private static final float HINGE_Y = 14f / 16f;
    private static final float HINGE_Z = 6f / 16f;

    private final TransformedInstance wheel;
    private final TransformedInstance[] prongs = new TransformedInstance[4];

    public ManaSiphonVisual(VisualizationContext ctx, ManaSiphonBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        wheel = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(CWPartialModels.MANA_SIPHON_WHEEL))
                .createInstance();
        for (int i = 0; i < prongs.length; i++) {
            prongs[i] = instancerProvider()
                    .instancer(InstanceTypes.TRANSFORMED, Models.partial(CWPartialModels.MANA_SIPHON_PRONG))
                    .createInstance();
        }
        relight(wheel);
        for (TransformedInstance prong : prongs) relight(prong);
        animate();
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        if (!isVisible(ctx.frustum()) || doDistanceLimitThisFrame(ctx))
            return;
        animate();
    }

    private void animate() {
        float angle = KineticBlockEntityRenderer.getAngleForBe(blockEntity, pos, Direction.Axis.Y);
        wheel.setIdentityTransform()
                .translate(getVisualPosition())
                .translate(Translate.CENTER)
                .rotateY(angle)
                .translateBack(Translate.CENTER)
                .setChanged();

        // Splay: tilt about the prong's hinge (canonical north frame), then rotate to each of the four
        // directions about the block centre. Composition is applied innermost-first, so the hinge tilt
        // lands before the directional Y-spin.
        float tilt = OUTWARD_DEGREES * blockEntity.prongAnimation.getValue(AnimationTickHolder.getPartialTicks());
        for (int i = 0; i < prongs.length; i++) {
            prongs[i].setIdentityTransform()
                    .translate(getVisualPosition())
                    .translate(Translate.CENTER)
                    .rotateYDegrees(90f * i)
                    .translateBack(Translate.CENTER)
                    .translate(HINGE_X, HINGE_Y, HINGE_Z)
                    .rotateXDegrees(tilt)
                    .translateBack(HINGE_X, HINGE_Y, HINGE_Z)
                    .setChanged();
        }
    }

    @Override
    public void updateLight(float partialTick) {
        relight(wheel);
        for (TransformedInstance prong : prongs) relight(prong);
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(wheel);
        for (TransformedInstance prong : prongs) consumer.accept(prong);
    }

    @Override
    protected void _delete() {
        wheel.delete();
        for (TransformedInstance prong : prongs) prong.delete();
    }
}
