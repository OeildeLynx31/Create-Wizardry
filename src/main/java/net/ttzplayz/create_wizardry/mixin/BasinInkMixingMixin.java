package net.ttzplayz.create_wizardry.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ttzplayz.create_wizardry.advancement.CWAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(BasinBlockEntity.class)
// ink advancement
public abstract class BasinInkMixingMixin {

    @Inject(method = "acceptOutputs", at = @At("RETURN"))
    private void create_wizardry$awardInkMixing(List<ItemStack> outputItems, List<FluidStack> outputFluids,
                                                boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (simulate || !cir.getReturnValueZ() || outputFluids.isEmpty()) return;

        Set<Fluid> inks = Set.of(
                FluidRegistry.COMMON_INK.get(),
                FluidRegistry.UNCOMMON_INK.get(),
                FluidRegistry.RARE_INK.get(),
                FluidRegistry.EPIC_INK.get(),
                FluidRegistry.LEGENDARY_INK.get());

        boolean mixedInk = false;
        for (FluidStack stack : outputFluids) {
            if (!stack.isEmpty() && inks.contains(stack.getFluid())) {
                mixedInk = true;
                break;
            }
        }
        if (!mixedInk) return;

        BasinBlockEntity self = (BasinBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        AABB area = new AABB(self.getBlockPos()).inflate(6.0);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            if (!player.isFakePlayer()) {
                CWAdvancements.INDUSTRIAL_INK.awardTo(player);
            }
        }
    }
}
