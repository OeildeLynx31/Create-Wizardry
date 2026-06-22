package net.ttzplayz.create_wizardry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.ttzplayz.create_wizardry.block.pipe.ArcanePumpBlock;
import net.ttzplayz.create_wizardry.block.pipe.EncasedArcanePipeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create's pipe network special-cases its own blocks by identity, so the arcane pump/encased pipe
 * (separate blocks reusing Create's BE classes) get skipped. We widen those identity checks.
 */
@Mixin(FluidPropagator.class)
public class FluidPropagatorMixin {

    // pump never told to pressurize its pipes -> mana/fluid won't move through arcane pipes
    @WrapOperation(
            method = "propagateChangedPipe",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean create_wizardry$acceptArcanePump(BlockEntry<?> pump, BlockState state, Operation<Boolean> original) {
        return original.call(pump, state) || state.getBlock() instanceof ArcanePumpBlock;
    }

    // encased arcane pipe excluded from neighbour-change propagation
    @WrapOperation(
            method = "validateNeighbourChange",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean create_wizardry$acceptEncasedArcanePipe(BlockEntry<?> encased, BlockState state, Operation<Boolean> original) {
        return original.call(encased, state) || state.getBlock() instanceof EncasedArcanePipeBlock;
    }
}
