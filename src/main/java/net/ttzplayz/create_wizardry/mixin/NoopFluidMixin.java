package net.ttzplayz.create_wizardry.mixin;

import io.redspace.ironsspellbooks.fluids.NoopFluid;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;
import net.ttzplayz.create_wizardry.item.CWItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NoopFluid backs many Iron's Spells 'n Spellbooks fluids (blood, inks, elixirs, potions) and is
 * intentionally unplaceable (returns AIR for its block and bucket, and reports zero height). We make
 * only the {@code irons_spellbooks:blood} instance placeable as a static source pool, leaving every
 * other NoopFluid untouched. The fluid id stays irons_spellbooks:blood so ISS mechanics and the
 * existing Create recipes keep working.
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

    @Inject(method = "getAmount", at = @At("HEAD"), cancellable = true)
    private void create_wizardry$bloodAmount(FluidState state, CallbackInfoReturnable<Integer> cir) {
        if (create_wizardry$isBlood()) {
            cir.setReturnValue(8);
        }
    }

    @Inject(method = "getBucket", at = @At("HEAD"), cancellable = true)
    private void create_wizardry$bloodBucket(CallbackInfoReturnable<Item> cir) {
        if (create_wizardry$isBlood()) {
            Item bucket = CWItems.BLOOD_BUCKET.get();
            cir.setReturnValue(bucket != null ? bucket : Items.AIR);
        }
    }
}
