package net.ttzplayz.create_wizardry.mixin;

import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// keep wearables in a deployer's hand instead of self-equipping, so they can be deployed onto mobs
@Mixin(Equipable.class)
public interface EquipableNoDeployerSelfEquipMixin {

    @Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void create_wizardry$noDeployerSelfEquip(Item item, Level level, Player player, InteractionHand hand,
        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (player instanceof DeployerFakePlayer) {
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }
}
