package net.ttzplayz.create_wizardry.particle;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ttzplayz.create_wizardry.CreateWizardry;

import java.util.List;
import java.util.Map;

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

    // rune types
    public static final List<DeferredHolder<ParticleType<?>, SimpleParticleType>> RUNES = List.of(
            ARCANE_RUNE, BLOOD_RUNE, ENDER_RUNE, EVOCATION_RUNE, FIRE_RUNE,
            HOLY_RUNE, ICE_RUNE, LIGHTNING_RUNE, NATURE_RUNE);

    // rune keyed by spell school id; unmapped elements (incl. "none") fall back to ARCANE_RUNE.
    public static final Map<String, DeferredHolder<ParticleType<?>, SimpleParticleType>> RUNE_BY_ELEMENT = Map.of(
            "fire", FIRE_RUNE,
            "lightning", LIGHTNING_RUNE,
            "ice", ICE_RUNE,
            "ender", ENDER_RUNE,
            "blood", BLOOD_RUNE,
            "evocation", EVOCATION_RUNE,
            "holy", HOLY_RUNE,
            "nature", NATURE_RUNE);

    public static SimpleParticleType runeFor(String element) {
        return RUNE_BY_ELEMENT.getOrDefault(element, ARCANE_RUNE).get();
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> rune(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(false) {});
    }

    // mana burst
    public static void spawnManaRunes(Level level, double x, double y, double z, int count, double spread, double speed) {
        for (int i = 0; i < count; i++) {
            SimpleParticleType rune = RUNES.get(level.random.nextInt(RUNES.size())).get();
            MagicManager.spawnParticles(level, rune, x, y, z, 1, spread, spread, spread, speed, false);
        }
    }

    // rune trail
    public static void spawnManaTrail(Level level, Vec3 from, Vec3 to, int count) {
        for (int i = 0; i < count; i++) {
            double t = (i + 1.0) / (count + 1.0);
            double jx = (level.random.nextDouble() - 0.5) * 0.1;
            double jy = (level.random.nextDouble() - 0.5) * 0.1;
            double jz = (level.random.nextDouble() - 0.5) * 0.1;
            Vec3 p = from.lerp(to, t);
            SimpleParticleType rune = RUNES.get(level.random.nextInt(RUNES.size())).get();
            MagicManager.spawnParticles(level, rune, p.x + jx, p.y + jy, p.z + jz, 1, 0, 0, 0, 0, false);
        }
    }

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
