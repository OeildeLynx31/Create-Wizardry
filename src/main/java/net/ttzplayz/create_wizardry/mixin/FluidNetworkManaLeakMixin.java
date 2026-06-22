package net.ttzplayz.create_wizardry.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.fluids.FluidNetwork;
import net.ttzplayz.create_wizardry.block.pipe.ManaPipeTransport;
import org.spongepowered.asm.mixin.Mixin;

// brackets the network tick so only piped mana leaks
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
