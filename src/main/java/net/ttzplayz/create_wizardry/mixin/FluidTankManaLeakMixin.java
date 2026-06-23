package net.ttzplayz.create_wizardry.mixin;

import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ttzplayz.create_wizardry.block.pipe.ManaPipeTransport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// piped mana leaks
@Mixin(FluidTankBlockEntity.class)
public abstract class FluidTankManaLeakMixin {

    @Shadow
    public abstract boolean isController();

    @Inject(method = "handlerForCapability", at = @At("RETURN"), cancellable = true)
    private void create_wizardry$leakMana(CallbackInfoReturnable<IFluidHandler> cir) {
        if (!isController()) return;
        // creative tanks are infinite sources
        if ((Object) this instanceof CreativeFluidTankBlockEntity) return;
        IFluidHandler original = cir.getReturnValue();
        if (original == null || original instanceof ManaPipeTransport.DecayingManaTank) return;
        cir.setReturnValue(ManaPipeTransport.decaying((BlockEntity) (Object) this, null, original));
    }
}
