package net.ttzplayz.create_wizardry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.ttzplayz.create_wizardry.block.pipe.EncasedArcanePipeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets the encased arcane pipe draw its rim attachment like Create's own encased pipe, which is
 * hardcoded by block identity in getRenderedRimAttachment.
 */
@Mixin(FluidTransportBehaviour.class)
public class FluidTransportBehaviourMixin {

    @WrapOperation(
            method = "getRenderedRimAttachment",
            at = @At(value = "INVOKE",
                    target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean create_wizardry$acceptEncasedArcanePipe(BlockEntry<?> encased, BlockState state, Operation<Boolean> original) {
        return original.call(encased, state) || state.getBlock() instanceof EncasedArcanePipeBlock;
    }
}
