package net.ttzplayz.create_wizardry.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.world.entity.LivingEntity;
import net.ttzplayz.create_wizardry.effect.CWMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSpellCastingMob.class)
// suppress siphoned casts
public abstract class AbstractSpellCastingMobMixin {

    @Inject(method = "initiateCastSpell", at = @At("HEAD"), cancellable = true)
    private void create_wizardry$blockCastWhileSiphoned(AbstractSpell spell, int spellLevel, CallbackInfo ci) {
        if (((LivingEntity) (Object) this).hasEffect(CWMobEffects.SIPHON_LOCK)) {
            ci.cancel();
        }
    }
}
