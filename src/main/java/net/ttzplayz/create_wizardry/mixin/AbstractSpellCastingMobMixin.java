package net.ttzplayz.create_wizardry.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.world.entity.LivingEntity;
import net.ttzplayz.create_wizardry.effect.CWMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents a spell-casting mob from casting while it sits in an active Mana Siphon's field (it
 * carries the short-lived {@code SIPHON_LOCK} effect, refreshed every scan).
 *
 * <p>This has to be done at the start of {@code initiateCastSpell}: the mob's attack goals don't
 * gate on mana (base {@code AbstractSpell.checkPreCastConditions} returns true), so zeroing mana
 * doesn't stop them, and {@code cancelCast()} is worse than useless here because it routes through
 * {@code castComplete() -> onServerCastComplete(...)} and actually fires the spell. No-opping the
 * cast initiation is the only clean way to suppress it.
 */
@Mixin(AbstractSpellCastingMob.class)
public abstract class AbstractSpellCastingMobMixin {

    @Inject(method = "initiateCastSpell", at = @At("HEAD"), cancellable = true)
    private void create_wizardry$blockCastWhileSiphoned(AbstractSpell spell, int spellLevel, CallbackInfo ci) {
        if (((LivingEntity) (Object) this).hasEffect(CWMobEffects.SIPHON_LOCK)) {
            ci.cancel();
        }
    }
}
