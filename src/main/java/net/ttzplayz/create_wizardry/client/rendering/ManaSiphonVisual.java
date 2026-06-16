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
import net.minecraft.core.Direction;
import net.ttzplayz.create_wizardry.block.mana_siphon.ManaSiphonBlockEntity;
import net.ttzplayz.create_wizardry.client.CWPartialModels;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** Renders the Mana Siphon's wheel spinning at its kinetic rotation speed (Flywheel backend). */
public class ManaSiphonVisual extends AbstractBlockEntityVisual<ManaSiphonBlockEntity>
        implements SimpleDynamicVisual {

    private final TransformedInstance wheel;

    public ManaSiphonVisual(VisualizationContext ctx, ManaSiphonBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        wheel = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(CWPartialModels.MANA_SIPHON_WHEEL))
                .createInstance();
        relight(wheel);
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
    }

    @Override
    public void updateLight(float partialTick) {
        relight(wheel);
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(wheel);
    }

    @Override
    protected void _delete() {
        wheel.delete();
    }
}
