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

/**
 * NoopFluid backs many Iron's Spells 'n Spellbooks fluids (blood, inks, elixirs, potions) and is
 * intentionally unplaceable: its {@code createLegacyBlock} returns AIR, so the blood bucket (which
 * wraps the noop {@code irons_spellbooks:blood}) places nothing. For the blood instance only, we
 * redirect that to our own flowing-blood block ({@link CWFluidRegistry#BLOOD_FLUID_BLOCK}). Every
 * other NoopFluid is left untouched, and the bucket/recipe/spell identity stays irons_spellbooks:blood.
 */
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
