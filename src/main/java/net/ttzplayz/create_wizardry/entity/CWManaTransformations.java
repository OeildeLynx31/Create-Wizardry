package net.ttzplayz.create_wizardry.entity;

import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;
import net.ttzplayz.create_wizardry.particle.CWParticles;

import java.util.List;
import java.util.function.Supplier;

/**
 * Turns vanilla mobs into Iron's Spells caster mobs when they are exposed to Mana together with the
 * matching Iron's Spells spellbook, mirroring how vanilla zombifies a piglin.
 *
 * Two-phase: a trigger <em>arms</em> a pending conversion (the mob holds the book and, if a piglin,
 * admires it gold-style); a tick handler <em>completes</em> it 5 seconds later.
 *
 * Trigger paths:
 *  - drop/deploy: a matching item dropped onto the mob while it is in a Mana spout/pool (from
 *    {@code ManaEffectHandler}). The only path for the Tarnished Crown, since Create's Deployer can't apply armor.
 *  - give/deploy: a player or Deployer uses the matching item on the mob while it is Mana-exposed (from
 *    the {@code PlayerInteractEvent.EntityInteract} hook).
 */
public final class CWManaTransformations {

    private CWManaTransformations() {}

    /** Persistent-data key stamped by the Mana spout handler so transient spouting still counts as exposure. */
    private static final String MANA_EXPOSED_KEY = "cw:mana_exposed_until";
    /** Persistent-data keys for an armed-but-not-yet-completed conversion. */
    private static final String CONVERT_TO_KEY = "cw:convert_to";
    private static final String CONVERT_AT_KEY = "cw:convert_at";

    /** How long (ticks) a Mana touch keeps a mob "exposed" for the interaction path. */
    private static final int EXPOSURE_TICKS = 30;
    /** Delay between arming and completing a conversion (5 seconds). */
    private static final int CONVERSION_DELAY = 100;

    /** A single vanilla-mob -> caster-mob conversion. */
    private record Conversion(Item trigger, EntityType<?> from, Supplier<EntityType<? extends Mob>> to) {}

    private static final List<Conversion> CONVERSIONS = List.of(
            new Conversion(ItemRegistry.DRUIDIC_SPELL_BOOK.get(), EntityType.PIGLIN,
                    EntityRegistry.APOTHECARIST::get),
            new Conversion(ItemRegistry.TARNISHED_CROWN.get(), EntityType.SKELETON,
                    EntityRegistry.NECROMANCER::get),
            new Conversion(ItemRegistry.VILLAGER_SPELL_BOOK.get(), EntityType.VILLAGER,
                    EntityRegistry.PRIEST::get),
            new Conversion(ItemRegistry.BLAZE_SPELL_BOOK.get(), EntityType.VILLAGER,
                    EntityRegistry.PYROMANCER::get),
            new Conversion(ItemRegistry.ICE_SPELL_BOOK.get(), EntityType.VILLAGER,
                    EntityRegistry.CRYOMANCER::get)
    );

    private static Conversion findConversion(Item item, EntityType<?> type) {
        for (Conversion c : CONVERSIONS) {
            if (c.trigger() == item && c.from() == type) {
                return c;
            }
        }
        return null;
    }

    private static boolean hasConversionFor(EntityType<?> type) {
        for (Conversion c : CONVERSIONS) {
            if (c.from() == type) {
                return true;
            }
        }
        return false;
    }

    // --- Exposure tracking -------------------------------------------------

    /** Marks an entity as recently touched by Mana (called from the Mana spout/pipe handler). */
    public static void markManaExposed(LivingEntity entity) {
        entity.getPersistentData().putLong(MANA_EXPOSED_KEY, entity.level().getGameTime() + EXPOSURE_TICKS);
    }

    /** True if the entity was recently spouted with Mana or is currently standing in a Mana fluid. */
    public static boolean isManaExposed(LivingEntity entity) {
        Level level = entity.level();
        if (entity.getPersistentData().getLong(MANA_EXPOSED_KEY) >= level.getGameTime()) {
            return true;
        }
        return isInMana(level, entity.blockPosition())
                || isInMana(level, BlockPos.containing(entity.getEyePosition()));
    }

    private static boolean isInMana(Level level, BlockPos pos) {
        var fluid = level.getFluidState(pos).getType();
        return fluid == CWFluidRegistry.MANA.get() || fluid == CWFluidRegistry.MANA_FLOWING.get();
    }

    // --- Trigger paths -----------------------------------------------------

    /** Drop/deploy path: arm a conversion if a matching item entity lies on the (Mana-exposed) mob. */
    public static void tryConvertViaDrop(ServerLevel level, LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        if (isPending(mob) || !hasConversionFor(mob.getType())) return;

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(0.5));
        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getItem();
            Conversion c = findConversion(stack.getItem(), mob.getType());
            if (c == null) continue;
            armConversion(mob, c, stack);
            stack.shrink(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }
            return;
        }
    }

    /** Give/deploy path: arm a conversion if the used item matches and the mob is Mana-exposed. */
    public static boolean tryConvertViaInteract(Player player, Mob mob, ItemStack stack) {
        Conversion c = findConversion(stack.getItem(), mob.getType());
        if (c == null || isPending(mob) || !isManaExposed(mob)) return false;
        if (!(mob.level() instanceof ServerLevel)) return false;

        armConversion(mob, c, stack);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    // --- Arm / complete ----------------------------------------------------

    private static boolean isPending(Mob mob) {
        return mob.getPersistentData().contains(CONVERT_AT_KEY);
    }

    private static void armConversion(Mob mob, Conversion c, ItemStack triggerStack) {
        EntityType<? extends Mob> target = c.to().get();
        CompoundTag data = mob.getPersistentData();
        data.putString(CONVERT_TO_KEY, BuiltInRegistries.ENTITY_TYPE.getKey(target).toString());
        data.putLong(CONVERT_AT_KEY, mob.level().getGameTime() + CONVERSION_DELAY);

        // Hold the book up for the duration; for piglins, the gold-style admire pose.
        mob.setItemSlot(EquipmentSlot.OFFHAND, triggerStack.copyWithCount(1));
        mob.setDropChance(EquipmentSlot.OFFHAND, 0F);
        if (mob instanceof Piglin piglin) {
            // Don't let it zombify mid-admire while the 5s timer runs.
            piglin.setImmuneToZombification(true);
            piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, CONVERSION_DELAY);
            mob.playSound(SoundEvents.PIGLIN_ADMIRING_ITEM, 1.0F, 1.0F);
        } else {
            mob.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
        }
    }

    /** Called every tick for every mob (server side): drives the admire animation and completes on schedule. */
    public static void tickPendingConversion(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(CONVERT_AT_KEY)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;

        long now = level.getGameTime();
        // Don't let the mob die mid-transform: undead won't burn in daylight, piglins won't zombify.
        mob.clearFire();
        if (mob instanceof Piglin piglin) {
            // Keep the piglin admiring so vanilla AI can't drop the book or clear the pose.
            piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, 40L);
            piglin.setImmuneToZombification(true);
        }
        if (now % 5L == 0L) {
            CWParticles.spawnManaRunes(level, mob.getX(), mob.getY() + mob.getBbHeight() / 2,
                    mob.getZ(), 6, mob.getBbWidth() / 2, 0.05);
        }

        if (now >= data.getLong(CONVERT_AT_KEY)) {
            completeConversion(level, mob, data);
        }
    }

    private static void completeConversion(ServerLevel level, Mob mob, CompoundTag data) {
        ResourceLocation id = ResourceLocation.tryParse(data.getString(CONVERT_TO_KEY));
        data.remove(CONVERT_TO_KEY);
        data.remove(CONVERT_AT_KEY);
        if (id == null) return;

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null) return;
        @SuppressWarnings("unchecked")
        EntityType<? extends Mob> mobType = (EntityType<? extends Mob>) type;

        Mob result = mob.convertTo(mobType, false);
        if (result == null) return; // conversion event cancelled
        result.finalizeSpawn(level, level.getCurrentDifficultyAt(result.blockPosition()),
                MobSpawnType.CONVERSION, null);

        CWParticles.spawnManaRunes(level, result.getX(), result.getY() + result.getBbHeight() / 2,
                result.getZ(), 16, result.getBbWidth() / 2, 0.1);
        level.playSound(null, result.blockPosition(), SoundRegistry.EVOCATION_CAST.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
    }
}
