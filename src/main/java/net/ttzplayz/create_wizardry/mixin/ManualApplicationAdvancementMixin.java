package net.ttzplayz.create_wizardry.mixin;

import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.ttzplayz.create_wizardry.advancement.CWAdvancements;
import net.ttzplayz.create_wizardry.block.CWBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Grants "The Enlightenment Age" when an Arcane Casing is made via item application, mirroring how
 * Create awards its Andesite/Brass Age advancements. Create's {@code awardAdvancements} runs for both
 * the manual (player right-click) and deployer paths, since the deployer also fires
 * {@code PlayerInteractEvent.RightClickBlock} through {@code CommonHooks.onRightClickBlock}. We only
 * award real players (the deployer's fake player is skipped), matching the "manually applying" case.
 */
@Mixin(ManualApplicationRecipe.class)
public class ManualApplicationAdvancementMixin {

    @Inject(method = "awardAdvancements", at = @At("HEAD"))
    private static void create_wizardry$awardArcaneCasing(Player player, BlockState placed, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer sp) || sp.isFakePlayer()) return;
        if (placed.is(CWBlocks.ARCANE_CASING.get())) {
            CWAdvancements.ENLIGHTENMENT_AGE.awardTo(sp);
        }
        // insulating a pipe/pump earns the advancement
        if (placed.is(net.ttzplayz.create_wizardry.util.CWTags.Blocks.MANA_INSULATED)) {
            CWAdvancements.SPLASH_GUARD_ON.awardTo(sp);
        }
    }
}
