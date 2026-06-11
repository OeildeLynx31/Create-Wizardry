package net.ttzplayz.create_wizardry.particle;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ttzplayz.create_wizardry.CreateWizardry;

import java.util.List;

public class CWParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CreateWizardry.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARCANE_RUNE = rune("arcane_rune");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_RUNE = rune("blood_rune");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ENDER_RUNE = rune("ender_rune");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EVOCATION_RUNE = rune("evocation_rune");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_RUNE = rune("fire_rune");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HOLY_RUNE = rune("holy_rune");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ICE_RUNE = rune("ice_rune");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LIGHTNING_RUNE = rune("lightning_rune");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NATURE_RUNE = rune("nature_rune");

    /** All rune particle types, for random selection. */
    public static final List<DeferredHolder<ParticleType<?>, SimpleParticleType>> RUNES = List.of(
            ARCANE_RUNE, BLOOD_RUNE, ENDER_RUNE, EVOCATION_RUNE, FIRE_RUNE,
            HOLY_RUNE, ICE_RUNE, LIGHTNING_RUNE, NATURE_RUNE);

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> rune(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(false) {});
    }

    /**
     * Spawns the signature mana burst of randomly-chosen rune particles. Shared by mana bucket vaporization
     * and mana spilling out of an open-ended pipe / pump so they look identical.
     */
    public static void spawnManaRunes(Level level, double x, double y, double z, int count, double spread, double speed) {
        for (int i = 0; i < count; i++) {
            SimpleParticleType rune = RUNES.get(level.random.nextInt(RUNES.size())).get();
            MagicManager.spawnParticles(level, rune, x, y, z, 1, spread, spread, spread, speed, false);
        }
    }

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
