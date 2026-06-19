package net.ttzplayz.create_wizardry.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.fluids.FluidNetwork;
import net.ttzplayz.create_wizardry.block.pipe.ManaPipeTransport;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Marks Create's fluid-network transfer tick as "mana is travelling through pipes", so the
 * {@link ManaPipeTransport.DecayingManaTank} leak applies only to mana that actually moves through
 * the pipe network — and never to a tank filled directly (a bucket, a hopper, an adjacent machine).
 * {@code FluidNetwork.tick()} holds the sole {@code IFluidHandler.fill} call in Create's fluid
 * transport, so this brackets exactly the piped transfers. Wrapped (not HEAD/RETURN injected) so the
 * depth is balanced even if the tick throws.
 */
@Mixin(FluidNetwork.class)
public class FluidNetworkManaLeakMixin {

    @WrapMethod(method = "tick")
    private void create_wizardry$gateManaLeak(Operation<Void> original) {
        ManaPipeTransport.enterPipeTransport();
        try {
            original.call();
        } finally {
            ManaPipeTransport.exitPipeTransport();
        }
    }
}
