package net.ttzplayz.create_wizardry.mixin;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ttzplayz.create_wizardry.block.pipe.ManaPipeTransport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes Create's own Fluid Tank leak mana that arrives through copper pipes, exactly like the
 * mod's mana tanks. Create exposes the tank capability via {@code handlerForCapability()}; we wrap
 * the returned handler in a {@link ManaPipeTransport.DecayingManaTank}. Only mana fills decay —
 * tanks holding water/lava/etc. are untouched.
 *
 * <p>Guarded to the controller: a non-controller tank block delegates to the controller's
 * (already-wrapped) handler, so wrapping only on the controller path avoids double-wrapping.
 * Create's own method names aren't SRG-remapped at runtime, so targeting by name needs no refmap.
 */
@Mixin(FluidTankBlockEntity.class)
public abstract class FluidTankManaLeakMixin {

    @Shadow
    public abstract boolean isController();

    @Inject(method = "handlerForCapability", at = @At("RETURN"), cancellable = true)
    private void create_wizardry$leakMana(CallbackInfoReturnable<IFluidHandler> cir) {
        if (!isController()) return;
        IFluidHandler original = cir.getReturnValue();
        if (original == null || original instanceof ManaPipeTransport.DecayingManaTank) return;
        cir.setReturnValue(ManaPipeTransport.decaying((BlockEntity) (Object) this, null, original));
    }
}
