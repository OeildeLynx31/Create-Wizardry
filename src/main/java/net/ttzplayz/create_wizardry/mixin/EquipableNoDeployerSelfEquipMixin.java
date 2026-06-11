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

/**
 * Stops Create's Deployer from equipping wearable items (armor, the Tarnished Crown, elytra, heads…)
 * onto its own fake player. Right-clicking an equipable normally swaps it into the player's armor
 * slot via {@link Equipable#swapWithEquipmentSlot}; for a {@link DeployerFakePlayer} that means the
 * Deployer pointlessly "wears" the item and empties its hand instead of applying it to a target. By
 * failing the swap, the item stays in the Deployer's hand so it can be used on an entity — e.g.
 * deploying the Tarnished Crown onto a Mana-exposed Skeleton to convert it into a Necromancer.
 */
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
