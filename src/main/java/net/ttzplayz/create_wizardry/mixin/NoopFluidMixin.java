package net.ttzplayz.create_wizardry.mixin;

import io.redspace.ironsspellbooks.fluids.NoopFluid;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// redirect noop blood placement to our flowing blood block; other noop fluids untouched
@Mixin(NoopFluid.class)
public abstract class NoopFluidMixin {

    private boolean create_wizardry$isBlood() {
        return (Object) this == FluidRegistry.BLOOD.get();
    }

    @Inject(method = "createLegacyBlock", at = @At("HEAD"), cancellable = true)
    private void create_wizardry$createBloodBlock(FluidState state, CallbackInfoReturnable<BlockState> cir) {
        if (create_wizardry$isBlood()) {
            cir.setReturnValue(CWFluidRegistry.BLOOD_FLUID_BLOCK.get().defaultBlockState());
        }
    }
}
