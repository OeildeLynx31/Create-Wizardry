package net.ttzplayz.create_wizardry.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
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

    // effect base
    private static class DepletionMobEffect extends MobEffect {
        protected DepletionMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
