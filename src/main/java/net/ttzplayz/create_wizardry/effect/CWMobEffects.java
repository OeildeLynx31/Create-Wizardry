package net.ttzplayz.create_wizardry.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ttzplayz.create_wizardry.CreateWizardry;

public class CWMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, CreateWizardry.MOD_ID);

    // depletion effect
    public static final DeferredHolder<MobEffect, MobEffect> DEPLETION =
            MOB_EFFECTS.register("mana_depletion", () ->
                    new DepletionMobEffect(MobEffectCategory.HARMFUL, 0x7B4FB5)
                            .addAttributeModifier(
                                    Attributes.MOVEMENT_SPEED,
                                    CreateWizardry.id("mana_depletion_slowness"),
                                    -0.30D,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                            // regen 4x slower while depleted, never fully stops
                            .addAttributeModifier(
                                    AttributeRegistry.MANA_REGEN,
                                    CreateWizardry.id("mana_depletion_slow_regen"),
                                    -0.75D,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    // siphon lock
    public static final DeferredHolder<MobEffect, MobEffect> SIPHON_LOCK =
            MOB_EFFECTS.register("siphon_lock", () ->
                    new DepletionMobEffect(MobEffectCategory.NEUTRAL, 0x7B4FB5)
                            .addAttributeModifier(
                                    AttributeRegistry.MANA_REGEN,
                                    CreateWizardry.id("siphon_lock_no_regen"),
                                    -1.0D,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    // Applies SIPHON_LOCK and broadcasts it to tracking clients. Vanilla does NOT sync mob-effect
    // changes to already-tracking clients (only to passengers / on initial tracking), so without this
    // broadcast the client never sees the effect and the render-side drain wobble can't trigger.
    public static void applySiphonLock(ServerLevel level, LivingEntity entity, int durationTicks) {
        MobEffectInstance instance = new MobEffectInstance(SIPHON_LOCK, durationTicks, 0, true, false, false);
        entity.addEffect(instance);
        level.getChunkSource().broadcast(entity,
                new ClientboundUpdateMobEffectPacket(entity.getId(), instance, false));
    }

    // Counterpart to applySiphonLock: the client never self-removes expired effects (removal is
    // server-gated) and vanilla doesn't sync removals to trackers, so broadcast it when the lock ends
    // or the wobble would persist forever.
    public static void broadcastSiphonLockRemoval(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            level.getChunkSource().broadcast(entity,
                    new ClientboundRemoveMobEffectPacket(entity.getId(), SIPHON_LOCK));
        }
    }

    // effect base
    private static class DepletionMobEffect extends MobEffect {
        protected DepletionMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
