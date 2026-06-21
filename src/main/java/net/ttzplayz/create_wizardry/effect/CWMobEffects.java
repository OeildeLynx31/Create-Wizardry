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

    /**
     * Inflicted on players whose mana is fully siphoned: heavy slowness and (via the
     * SpellPreCastEvent hook in CWEvents) an inability to cast spells.
     */
    public static final DeferredHolder<MobEffect, MobEffect> DEPLETION =
            MOB_EFFECTS.register("mana_depletion", () ->
                    new DepletionMobEffect(MobEffectCategory.HARMFUL, 0x7B4FB5)
                            .addAttributeModifier(
                                    Attributes.MOVEMENT_SPEED,
                                    CreateWizardry.id("mana_depletion_slowness"),
                                    -0.30D,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                            // Mana regenerates 4x slower (25%) while depleted, but never fully stops.
                            .addAttributeModifier(
                                    AttributeRegistry.MANA_REGEN,
                                    CreateWizardry.id("mana_depletion_slow_regen"),
                                    -0.75D,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /**
     * Hidden, short-lived effect applied to anything within an active Mana Siphon's radius:
     * zeroes mana regeneration so caught entities cannot regenerate while being absorbed from.
     */
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

    /** MobEffect's constructor is protected, so a trivial subclass is needed to instantiate it. */
    private static class DepletionMobEffect extends MobEffect {
        protected DepletionMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
