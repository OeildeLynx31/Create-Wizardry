package net.ttzplayz.create_wizardry.block.mana_siphon;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.redspace.ironsspellbooks.api.item.IScroll;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.DeadKingBoss;
import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.DeadKingCorpseEntity;
import io.redspace.ironsspellbooks.entity.mobs.ice_spider.IceSpiderEntity;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossEntity;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ttzplayz.create_wizardry.CWConfig;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import net.ttzplayz.create_wizardry.block.CWBlocks;
import net.ttzplayz.create_wizardry.block.pipe.ManaPipeTransport;
import net.ttzplayz.create_wizardry.client.ClientManaSiphons;
import net.ttzplayz.create_wizardry.effect.CWMobEffects;
import net.ttzplayz.create_wizardry.entity.CWManaTransformations;
import net.ttzplayz.create_wizardry.item.CWItems;
import net.ttzplayz.create_wizardry.particle.CWParticles;
import net.ttzplayz.create_wizardry.util.CWTags;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK;
import static net.ttzplayz.create_wizardry.fluids.CWFluidRegistry.MANA;

public class ManaSiphonBlockEntity extends KineticBlockEntity {

    public SmartFluidTankBehaviour internalTank;

    private static final int CAPACITY = 1000;
    private static final int SCAN_INTERVAL = 10;

    private static final float STRESS_IMPACT = 4f;

    private static final int GROWTH_COST = 50;

    private static final int GROWTH_COOLDOWN_TICKS = 300;

    private static final int EGG_DRAIN_STEPS = 30;

    private static final int ARMOR_PILE_MANA = 1500;

    private static final int DEPLETION_DURATION = 300;

    private static final double SPELL_PULL_SPEED = 0.55;
    private static final double SPELL_CONSUME_DIST = 1.4;

    private static final int PUMP_MAX_PER_TICK = 128;

    // Speed used to drive farming/pumping. When the config makes rotation optional and the
    // block isn't spinning, fall back to a fixed emulated speed so the pump still flows.
    private static final float UNPOWERED_PUMP_SPEED = 32f;

    private int scanCooldown = SCAN_INTERVAL;
    private int growthCooldown = 0;

    private float lastPumpSpeed = Float.NaN;
    // lazy caps
    private final Map<Direction, IFluidHandler> decayingCaps = new HashMap<>();
    private final Map<BlockPos, Integer> eggProgress = new HashMap<>();
    private final Map<BlockPos, Integer> armorPileProgress = new HashMap<>();
    // per-item transformation warm-up (accumulated ticks keyed by item entity UUID)
    private final Map<UUID, Integer> itemDrainProgress = new HashMap<>();
    // caster drain
    private final Map<UUID, Integer> casterDrain = new HashMap<>();
    // placer uuid
    public UUID placerUuid;

    // 0..1 prong-splay, driven both sides for the client visual
    public final LerpedFloat prongAnimation = LerpedFloat.linear();

    // client mana-orb state
    public final LerpedFloat orbDrain = LerpedFloat.linear();
    public float orbSpin = 0f;
    public float prevOrbSpin = 0f;
    public boolean clientDraining = false;
    private int drainPulseTicks = 0;
    private int orbRingCooldown = 0;

    public ManaSiphonBlockEntity(BlockPos pos, BlockState state) {
        super(CWBlockEntities.MANA_SIPHON_BE.get(), pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                BLOCK,
                CWBlockEntities.MANA_SIPHON_BE.get(),
                (be, context) -> be.decayingCapability(context));
    }


    // the direction the crystal grows ("front"); fluid output is the opposite side
    private Direction facing() {
        return getBlockState().getValue(ManaSiphonBlock.FACING);
    }

    // Operating speed driving farming/pumping. Falls back to an emulated speed when the config
    // lets the Siphon run without rotation; otherwise mirrors the real shaft speed.
    private float effectiveSpeed() {
        float speed = getSpeed();
        if (speed != 0) return speed;
        return CWConfig.manaSiphonRequiresRotation ? 0f : UNPOWERED_PUMP_SPEED;
    }

    // floating orb sits just off the front face; rune tethers terminate slightly closer in
    private Vec3 orbCenter() {
        return Vec3.atCenterOf(worldPosition).add(Vec3.atLowerCornerOf(facing().getNormal()).scale(0.75));
    }

    private Vec3 tetherPoint() {
        return Vec3.atCenterOf(worldPosition).add(Vec3.atLowerCornerOf(facing().getNormal()).scale(0.5));
    }

    private IFluidHandler decayingCapability(Direction side) {
        if (side != facing().getOpposite()) return null; // only connect from the output (back) side
        if (internalTank == null) return null;
        return decayingCaps.computeIfAbsent(side,
                s -> ManaPipeTransport.decaying(this, s, internalTank.getCapability()));
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        internalTank = SmartFluidTankBehaviour.single(this, CAPACITY)
                .allowInsertion()
                .allowExtraction();
        behaviours.add(internalTank);
    }

    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = STRESS_IMPACT;
        return STRESS_IMPACT;
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("GrowthCooldown", growthCooldown);
        if (placerUuid != null) compound.putUUID("Placer", placerUuid);
        if (clientPacket) compound.putBoolean("Draining", drainPulseTicks > 0);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        growthCooldown = compound.getInt("GrowthCooldown");
        placerUuid = compound.hasUUID("Placer") ? compound.getUUID("Placer") : null;
        if (clientPacket) clientDraining = compound.getBoolean("Draining");
    }

    // award placer
    private void awardOwner(net.ttzplayz.create_wizardry.advancement.CWAdvancement advancement) {
        if (placerUuid == null || level == null) return;
        Player owner = level.getPlayerByUUID(placerUuid);
        if (owner != null) advancement.awardTo(owner);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (internalTank == null) return added;
        boolean expanded = getBlockState().getValue(ManaSiphonBlock.EXPANDED);
        int diameter = currentRadius() * 2 + 1;
        tooltip.add(Component.translatable("block.create_wizardry.mana_siphon")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" ")
                .append(Component.translatable("create_wizardry.tooltip.mana_siphon."
                        + (expanded ? "expanded" : "confined")).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (" + diameter + "x" + diameter + ")")
                        .withStyle(ChatFormatting.DARK_GRAY)));
        containedFluidTooltip(tooltip, isPlayerSneaking, internalTank.getPrimaryHandler());
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        // splay prongs toward target on EXPANDED change, driven both sides for the client visual
        prongAnimation.chase(getBlockState().getValue(ManaSiphonBlock.EXPANDED) ? 1 : 0, .2f, LerpedFloat.Chaser.EXP);
        prongAnimation.tickChaser();
        if (level.isClientSide) {
            tickClientOrb();
            return;
        }
        tickServer();
    }

    // spin/grow the orb and orbit its rune ring
    private void tickClientOrb() {
        ClientManaSiphons.add(worldPosition);
        orbDrain.chase(clientDraining ? 1 : 0, 0.15f, LerpedFloat.Chaser.EXP);
        orbDrain.tickChaser();
        prevOrbSpin = orbSpin;
        orbSpin += 6f * (1f + 4f * orbDrain.getValue()); // faster while draining
        spawnOrbRing();
    }

    private void spawnOrbRing() {
        int mana = storedMana();
        if (mana <= 0) return;
        if (--orbRingCooldown > 0) return;
        orbRingCooldown = 4;
        int count = Math.min(8, ((mana - 1) / 250 + 1) * 2); // 2/4/6/8 by tier
        Vec3 center = orbCenter();
        double phase = Math.toRadians(orbSpin);
        for (int i = 0; i < count; i++) {
            double a = phase + i * (Math.PI * 2 / count);
            double px = center.x + Math.cos(a) * 0.45;
            double pz = center.z + Math.sin(a) * 0.45;
            SimpleParticleType rune = CWParticles.RUNES.get(i % CWParticles.RUNES.size()).get();
            level.addParticle(rune, px, center.y, pz, 0, 0.005, 0);
        }
    }

    private void tickServer() {
        if (growthCooldown > 0) growthCooldown--;
        if (drainPulseTicks > 0 && --drainPulseTicks == 0) notifyUpdate();
        tickCrystallization();

        if (effectiveSpeed() == 0) return;
        tickPump();

        AABB box = currentBox();
        attractSpells(box); // pull every tick

        if (--scanCooldown > 0) return;
        scanCooldown = SCAN_INTERVAL;

        if (bossBreakCheck(box)) return;
        drainEntities(box);
        tickEggs();
        tickArmorPiles();
        tickSoulRune();
        if (CWConfig.manaSiphonDrainItems) tickItems(box);
    }

    // EXPANDED/CONFINED RADIUS

    private int currentRadius() {
        return getBlockState().getValue(ManaSiphonBlock.EXPANDED)
                ? CWConfig.manaSiphonLargeRadius
                : CWConfig.manaSiphonSmallRadius;
    }

    private AABB currentBox() {
        return new AABB(worldPosition).inflate(currentRadius());
    }

    // BOSS INTERACTIONS

    // boss break
    private boolean bossBreakCheck(AABB box) {
        boolean overload = !level.getEntitiesOfClass(FireBossEntity.class, box).isEmpty();
        // A fully-awakened Dead King overloads the siphon
        if (!overload) {
            overload = !level.getEntitiesOfClass(DeadKingBoss.class, box).isEmpty();
        }
        // So does a Dead King corpse the instant it begins awakening
        if (!overload) {
            for (DeadKingCorpseEntity corpse : level.getEntitiesOfClass(DeadKingCorpseEntity.class, box)) {
                if (corpse.triggered()) {
                    overload = true;
                    break;
                }
            }
        }
        if (!overload) return false;

        level.levelEvent(2001, worldPosition, net.minecraft.world.level.block.Block.getId(getBlockState()));
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
        if (level instanceof ServerLevel sl) {
            CWParticles.spawnManaRunes(sl, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, 24, 0.4, 0.2);
        }
        level.destroyBlock(worldPosition, false);
        return true;
    }

    // DRAINING

    private void drainEntities(AABB box) {
        int perOp = CWConfig.manaSiphonDrainPerOp;
        Set<UUID> seenCasters = new HashSet<>();
        List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        for (LivingEntity e : living) {
            if (e instanceof DeadKingCorpseEntity) {
                fillMana(perOp);
                spawnDrainParticles(e);
                continue; // inf mana from the sleeping dead king
            }
            if (e instanceof IceSpiderEntity spider) {
                seenCasters.add(spider.getUUID());
                if (spider.getHealth() < spider.getMaxHealth() * 0.25f) {
                    drainCaster(spider);
                }
                continue;
            }
            if (e instanceof AbstractSpellCastingMob caster) {
                seenCasters.add(caster.getUUID());
                drainCaster(caster);
                continue;
            }
            if (e instanceof Player player) {
                if (player.hasEffect(CWMobEffects.DEPLETION)) continue; // depleted: not drained, regens slowly
                applySiphonLock(player);
                drainPlayer(player);
            }
        }
        // Forget progress for casters that left the radius / died
        if (!casterDrain.isEmpty()) {
            casterDrain.keySet().retainAll(seenCasters);
        }
    }

    private void drainCaster(AbstractSpellCastingMob caster) {
        // zero mana and reapply SIPHON_LOCK; the mixin no-ops casts while locked (cancelCast would complete the spell)
        MagicData md = caster.getMagicData();
        md.setMana(0);
        md.resetCastingState();
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SCAN_INTERVAL + 10, 2, false, false));
        applySiphonLock(caster);

        int accepted = fillMana(CWConfig.manaSiphonDrainPerOp);
        if (accepted <= 0) return; // tank full: suppressed, but no further drain progress
        spawnDrainParticles(caster);
        awardOwner(net.ttzplayz.create_wizardry.advancement.CWAdvancements.YOUR_SOUL_IS_MINE);

        // transforms when all mana is siphoned
        int pool = Math.max(1, (int) caster.getAttributeValue(AttributeRegistry.MAX_MANA));
        int progress = casterDrain.getOrDefault(caster.getUUID(), 0) + accepted;
        if (progress >= pool) {
            casterDrain.remove(caster.getUUID());
            revert(caster);
        } else {
            casterDrain.put(caster.getUUID(), progress);
        }
    }

    private void revert(AbstractSpellCastingMob caster) {
        if (!(level instanceof ServerLevel sl)) return;
        if (caster instanceof IceSpiderEntity spider) {
            CWManaTransformations.dropKeyItem(sl, spider);
            Mob result = spider.convertTo(EntityType.SPIDER, false);
            if (result != null) {
                CWParticles.spawnManaRunes(sl, result.getX(), result.getY() + result.getBbHeight() / 2,
                        result.getZ(), 16, result.getBbWidth() / 2, 0.1);
                sl.playSound(null, result.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.0F, 0.8F);
                CWManaTransformations.applyTransformationDamage(sl, result);
            }
            return;
        }
        CWManaTransformations.revertDrainedCaster(sl, caster);
    }

    private void drainPlayer(Player player) {
        if (player.isCreative() || player.isSpectator()) return;
        MagicData md = MagicData.getPlayerMagicData(player);
        float mana = md.getMana();
        if (mana > 0) {
            int ratio = CWConfig.playerManaPerMb;
            int tankSpace = CAPACITY - storedMana();
            int mbWanted = Math.min(CWConfig.manaSiphonDrainPerOp, Math.min(tankSpace, (int) (mana / ratio)));
            if (mbWanted > 0) {
                int filled = fillMana(mbWanted);
                if (filled > 0) {
                    md.setMana(mana - (float) filled * ratio);
                    if (player instanceof ServerPlayer sp) {
                        PacketDistributor.sendToPlayer(sp, new SyncManaPacket(md));
                    }
                    spawnDrainParticles(player);
                }
            }
        }
        if (md.getMana() <= 0) {
            player.addEffect(new MobEffectInstance(CWMobEffects.DEPLETION, DEPLETION_DURATION, 0, false, true, true));
        }
    }

    private void applySiphonLock(LivingEntity e) {
        if (level instanceof ServerLevel sl) {
            CWMobEffects.applySiphonLock(sl, e, SCAN_INTERVAL + 5);
        }
    }

    private void spawnDrainParticles(LivingEntity e) {
        if (level instanceof ServerLevel sl) {
            double ex = e.getX();
            double ey = e.getY() + e.getBbHeight() / 2;
            double ez = e.getZ();
            CWParticles.spawnManaRunes(sl, ex, ey, ez, 6, e.getBbWidth() / 2, 0.06);
            // Rune trail
            Vec3 top = tetherPoint();
            CWParticles.spawnManaTrail(sl, new Vec3(ex, ey, ez), top, 8);
        }
        markDraining();
    }

    // rune burst at a drained block + a rune-line tether back to the Siphon (mirrors item/entity draining)
    private void spawnBlockDrainParticles(BlockPos pos) {
        if (level instanceof ServerLevel sl) {
            double bx = pos.getX() + 0.5, by = pos.getY() + 0.5, bz = pos.getZ() + 0.5;
            CWParticles.spawnManaRunes(sl, bx, by, bz, 6, 0.3, 0.06);
            Vec3 top = tetherPoint();
            CWParticles.spawnManaTrail(sl, new Vec3(bx, by, bz), top, 8);
        }
        markDraining();
    }

    // light the orb's fast-spin pulse and sync it to the client
    private void markDraining() {
        boolean was = drainPulseTicks > 0;
        drainPulseTicks = 20;
        if (!was) notifyUpdate();
    }

    // SPELL ATTRACTION CODE

    private void attractSpells(AABB box) {
        List<AbstractMagicProjectile> spells = level.getEntitiesOfClass(AbstractMagicProjectile.class, box);
        if (spells.isEmpty()) return;
        Vec3 center = Vec3.atCenterOf(worldPosition);
        for (AbstractMagicProjectile spell : spells) {
            Vec3 toCenter = center.subtract(spell.position());
            double dist = toCenter.length();
            if (dist < SPELL_CONSUME_DIST) {
                // spells absorbed in-radius bank mana scaled by their damage (see CWConfig)
                int mana = Math.max(1, (int) (Math.round(spell.getDamage()) * CWConfig.manaSiphonSpellManaPerDamage));
                fillMana(mana);
                if (level instanceof ServerLevel sl) {
                    CWParticles.spawnManaRunes(sl, spell.getX(), spell.getY(), spell.getZ(), 10, 0.2, 0.1);
                }
                level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.4F);
                spell.discard();
            } else {
                spell.setDeltaMovement(toCenter.normalize().scale(SPELL_PULL_SPEED));
                spell.hasImpulse = true;
            }
        }
    }

    // Ice Spider Eggs

    private void tickEggs() {
        int radius = currentRadius();
        BlockPos found = null;
        for (BlockPos p : BlockPos.betweenClosed(
                worldPosition.offset(-radius, -radius, -radius),
                worldPosition.offset(radius, radius, radius))) {
            if (level.getBlockState(p).is(BlockRegistry.ICE_SPIDER_EGG.get())) {
                found = p.immutable();
                break;
            }
        }
        if (found == null) {
            if (!eggProgress.isEmpty()) eggProgress.clear();
            return;
        }
        if (fillMana(CWConfig.manaSiphonDrainPerOp) <= 0) return; // tank full

        int prog = eggProgress.getOrDefault(found, 0) + 1;
        if (prog >= EGG_DRAIN_STEPS) {
            level.setBlockAndUpdate(found, Blocks.TURTLE_EGG.defaultBlockState());
            eggProgress.remove(found);
            level.playSound(null, found, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
            if (level instanceof ServerLevel sl) {
                CWParticles.spawnManaRunes(sl, found.getX() + 0.5, found.getY() + 0.5, found.getZ() + 0.5, 16, 0.3, 0.1);
            }
        } else {
            eggProgress.put(found, prog);
            spawnBlockDrainParticles(found);
        }
    }

    // Armor Piles

    private void tickArmorPiles() {
        int radius = currentRadius();
        BlockPos found = null;
        for (BlockPos p : BlockPos.betweenClosed(
                worldPosition.offset(-radius, -radius, -radius),
                worldPosition.offset(radius, radius, radius))) {
            if (level.getBlockState(p).is(BlockRegistry.ARMOR_PILE_BLOCK.get())) {
                found = p.immutable();
                break;
            }
        }
        if (found == null) {
            if (!armorPileProgress.isEmpty()) armorPileProgress.clear();
            return;
        }
        int accepted = fillMana(CWConfig.manaSiphonDrainPerOp);
        if (accepted <= 0) return; // tank full

        int prog = armorPileProgress.getOrDefault(found, 0) + accepted;
        if (prog >= ARMOR_PILE_MANA) {
            armorPileProgress.remove(found);
            level.removeBlock(found, false);
            dropArmorPileLoot(found);
            level.playSound(null, found, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
            if (level instanceof ServerLevel sl) {
                CWParticles.spawnManaRunes(sl, found.getX() + 0.5, found.getY() + 0.5, found.getZ() + 0.5, 16, 0.3, 0.1);
            }
        } else {
            armorPileProgress.put(found, prog);
            spawnBlockDrainParticles(found);
        }
    }

    // Cinderous Soul Rune

    // Drains infinite mana while a dormant cinderous soul rune is in range.
    // Once it's used to summon Tyros, the FireBoss overloads the siphon in bossBreakCheck.
    private void tickSoulRune() {
        int radius = currentRadius();
        BlockPos found = null;
        for (BlockPos p : BlockPos.betweenClosed(
                worldPosition.offset(-radius, -radius, -radius),
                worldPosition.offset(radius, radius, radius))) {
            if (level.getBlockState(p).is(BlockRegistry.CINDEROUS_KEYSTONE.get())) {
                found = p.immutable();
                break;
            }
        }
        if (found == null) return;
        fillMana(CWConfig.manaSiphonDrainPerOp); // inf mana from the soul rune
        spawnBlockDrainParticles(found);
    }

    private static final Item[] ARMOR_PILE_LOOT = {
            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, Items.NETHERITE_INGOT
    };

    private void dropArmorPileLoot(BlockPos pos) {
        Item drop = ARMOR_PILE_LOOT[level.random.nextInt(ARMOR_PILE_LOOT.length)];
        Block.popResource(level, pos, new ItemStack(drop));
    }

    // ITEM DRAINING

    // mana (mB) it costs to craft an arcane ingot/sheet/magic cloth via filling; drained items bank a fraction of this
    private static final int ARCANE_CRAFT_MANA = 500;

    // yield (mB) banked + optional transmuted result item (null = item is consumed)
    private record DrainResult(int yieldMb, Item result) {}

    // warm one dropped magical item at a time: tether a rune-line to it, then transform it
    private void tickItems(AABB box) {
        // prefer continuing an item that is already warming up; otherwise take the first drainable one
        ItemEntity target = null, firstDrainable = null;
        DrainResult drain = null, firstDrain = null;
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            DrainResult d = classifyItem(ie.getItem());
            if (d == null) continue;
            if (firstDrainable == null) { firstDrainable = ie; firstDrain = d; }
            if (itemDrainProgress.containsKey(ie.getUUID())) { target = ie; drain = d; break; }
        }
        if (target == null) { target = firstDrainable; drain = firstDrain; }

        if (target == null) {
            if (!itemDrainProgress.isEmpty()) itemDrainProgress.clear();
            return;
        }

        UUID id = target.getUUID();
        itemDrainProgress.keySet().retainAll(Set.of(id)); // drop any stale warm-ups

        // rune-line tether + orb pulse while the item is being transformed
        spawnItemDrainParticles(target);
        markDraining();

        int progress = itemDrainProgress.getOrDefault(id, 0) + SCAN_INTERVAL;
        if (progress < CWConfig.manaSiphonTransformDelayTicks) {
            itemDrainProgress.put(id, progress);
            return;
        }

        int accepted = fillMana(drain.yieldMb());
        if (accepted <= 0) {
            // tank full / nothing banked: hold at the delay and retry next scan, keeping the item
            itemDrainProgress.put(id, CWConfig.manaSiphonTransformDelayTicks);
            return;
        }
        itemDrainProgress.remove(id);

        ItemStack stack = target.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) target.discard(); else target.setItem(stack);
        if (drain.result() != null) {
            Block.popResource(level, target.blockPosition(), new ItemStack(drain.result()));
        }
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.4F);
        if (level instanceof ServerLevel sl) {
            CWParticles.spawnManaRunes(sl, target.getX(), target.getY(), target.getZ(), 8, 0.2, 0.08);
        }
    }

    // rune burst at the item + a rune-line tether back to the Siphon, while it warms up
    private void spawnItemDrainParticles(ItemEntity ie) {
        if (level instanceof ServerLevel sl) {
            CWParticles.spawnManaRunes(sl, ie.getX(), ie.getY(), ie.getZ(), 6, 0.2, 0.06);
            Vec3 top = tetherPoint();
            CWParticles.spawnManaTrail(sl, new Vec3(ie.getX(), ie.getY(), ie.getZ()), top, 8);
        }
    }

    // classify a dropped stack into a drain yield + result, or null if not drainable
    private DrainResult classifyItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();

        if (item instanceof IScroll) {
            ISpellContainer container = ISpellContainer.get(stack);
            if (container == null) return null;
            SpellData spellData = container.getSpellAtIndex(0);
            if (spellData == null || spellData == SpellData.EMPTY) return null;
            AbstractSpell spell = spellData.getSpell();
            if (spell == null) return null;
            int cost = spell.getManaCost(spellData.getLevel());
            if (cost <= 0) return null;
            int yield = Math.max(1, (int) Math.round(CWConfig.manaSiphonScrollDrainPercent * cost));
            return new DrainResult(yield, Items.PAPER);
        }
        if (item == ItemRegistry.ARCANE_INGOT.get()) {
            return new DrainResult(arcaneItemYield(), randomIngot());
        }
        if (item == CWBlocks.ARCANE_BLOCK.get().asItem()) {
            return new DrainResult(arcaneItemYield() * 9, randomMineralBlock()); // a block is 9 ingots
        }
        if (item == CWItems.ARCANE_SHEET.get()) {
            return new DrainResult(arcaneItemYield(), randomSheet());
        }
        if (item == ItemRegistry.MAGIC_CLOTH.get()) {
            return new DrainResult(arcaneItemYield(), Items.WHITE_WOOL);
        }
        return null;
    }

    private int arcaneItemYield() {
        return Math.max(1, (int) Math.round(CWConfig.manaSiphonItemDrainPercent * ARCANE_CRAFT_MANA));
    }

    private static final Item[] BASE_INGOTS = {Items.GOLD_INGOT, Items.IRON_INGOT, Items.COPPER_INGOT};

    private Item randomIngot() {
        Item special = rollSpecial(Items.NETHERITE_INGOT, createItem("brass_ingot"));
        return special != null ? special : BASE_INGOTS[level.random.nextInt(BASE_INGOTS.length)];
    }

    private static final Item[] BASE_BLOCKS = {Items.GOLD_BLOCK, Items.IRON_BLOCK, Items.COPPER_BLOCK};

    private Item randomMineralBlock() {
        Item special = rollSpecial(Items.NETHERITE_BLOCK, createItem("brass_block"));
        return special != null ? special : BASE_BLOCKS[level.random.nextInt(BASE_BLOCKS.length)];
    }

    private Item randomSheet() {
        Item[] base = {createItem("golden_sheet"), createItem("iron_sheet"), createItem("copper_sheet")};
        Item special = rollSpecial(createItem("sturdy_sheet"), createItem("brass_sheet"));
        return special != null ? special : base[level.random.nextInt(base.length)];
    }

    // roll the optional rare/brass upgrade, or null to fall back to a base result
    private Item rollSpecial(Item rare, Item brass) {
        if (!CWConfig.manaSiphonSpecialIngotDrops) return null;
        double r = level.random.nextDouble();
        if (rare != null && r < CWConfig.manaSiphonRareDropChance) return rare;
        if (brass != null && r < CWConfig.manaSiphonRareDropChance + CWConfig.manaSiphonBrassDropChance) return brass;
        return null;
    }

    private static final Map<String, Item> CREATE_ITEMS = new HashMap<>();

    // lazily resolve a create: item by path, falling back to iron ingot if absent
    private static Item createItem(String path) {
        return CREATE_ITEMS.computeIfAbsent(path, p -> {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", p));
            return item == Items.AIR ? Items.IRON_INGOT : item;
        });
    }

    // MANA CRYSTALLIZATION

    // grow essence
    private void tickCrystallization() {
        if (growthCooldown > 0) return;
        if (storedMana() < GROWTH_COST) return;

        Direction f = facing();
        BlockPos host = worldPosition.relative(f, 2);
        if (!level.getBlockState(host).is(CWTags.Blocks.CRYSTALLINE)) return;

        BlockPos clusterPos = worldPosition.relative(f, 1);
        BlockState cs = level.getBlockState(clusterPos);

        if (cs.getBlock() instanceof ArcaneEssenceClusterBlock) {
            if (cs.getValue(ArcaneEssenceClusterBlock.AGE) < ArcaneEssenceClusterBlock.MAX_AGE) {
                drainMana(GROWTH_COST);
                level.setBlockAndUpdate(clusterPos,
                        cs.setValue(ArcaneEssenceClusterBlock.AGE, cs.getValue(ArcaneEssenceClusterBlock.AGE) + 1));
                crystalGrowFx(clusterPos);
                growthCooldown = GROWTH_COOLDOWN_TICKS;
            }
        } else if (cs.isAir() || cs.canBeReplaced()) {
            drainMana(GROWTH_COST);
            BlockState cluster = CWBlocks.ARCANE_ESSENCE_CLUSTER.get().defaultBlockState()
                    .setValue(ArcaneEssenceClusterBlock.FACING, f.getOpposite())
                    .setValue(ArcaneEssenceClusterBlock.AGE, 0)
                    .setValue(ArcaneEssenceClusterBlock.WATERLOGGED, cs.getFluidState().getType() == Fluids.WATER);
            level.setBlockAndUpdate(clusterPos, cluster);
            crystalGrowFx(clusterPos);
            growthCooldown = GROWTH_COOLDOWN_TICKS;
        }
    }

    private void crystalGrowFx(BlockPos pos) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.0F);
        if (level instanceof ServerLevel sl) {
            CWParticles.spawnManaRunes(sl, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.25, 0.05);
        }
    }

    // PUMP

    private void tickPump() {
        BlockPos out = worldPosition.relative(facing().getOpposite());
        FluidTransportBehaviour outputPipe = FluidPropagator.getPipe(level, out);
        if (outputPipe != null) {
            maintainDownwardPressure(outputPipe);
        } else {
            lastPumpSpeed = Float.NaN; // pipe removed; force a fresh apply if one returns
            if (storedMana() > 0) directFillBelow();
        }
    }

    private void maintainDownwardPressure(FluidTransportBehaviour firstPipe) {
        float speed = Math.abs(effectiveSpeed());
        if (speed == 0) {
            lastPumpSpeed = Float.NaN;
            return;
        }
        if (speed == lastPumpSpeed && firstPipe.hasAnyPressure()) return; // stable: let the network flow
        distributePressureDown(speed);
        lastPumpSpeed = speed;
    }

    private void distributePressureDown(float pressure) {
        int max = FluidPropagator.getPumpRange();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        Map<BlockPos, Direction> entryFace = new HashMap<>();
        BlockPos first = worldPosition.relative(facing().getOpposite());
        entryFace.put(first, facing()); // fluid enters the first pipe from the Siphon on its front side
        frontier.add(first);

        while (!frontier.isEmpty() && visited.size() < max) {
            BlockPos pos = frontier.poll();
            if (!visited.add(pos)) continue;
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, pos);
            if (pipe == null) continue;
            pipe.wipePressure(); // stable, non-accumulating pressure
            BlockState state = level.getBlockState(pos);
            Direction in = entryFace.get(pos);
            for (Direction d : FluidPropagator.getPipeConnections(state, pipe)) {
                boolean inbound = d == in;
                pipe.addPressure(d, inbound, pressure);
                if (!inbound) {
                    BlockPos next = pos.relative(d);
                    if (!visited.contains(next) && FluidPropagator.getPipe(level, next) != null) {
                        entryFace.putIfAbsent(next, d.getOpposite());
                        frontier.add(next);
                    }
                }
            }
        }
    }

    private void directFillBelow() {
        int stored = storedMana();
        IFluidHandler target = level.getCapability(BLOCK, worldPosition.relative(facing().getOpposite()), facing());
        if (target == null) return;
        int toPush = Math.min(stored, Mth.clamp((int) Math.abs(effectiveSpeed()), 1, PUMP_MAX_PER_TICK));
        ManaPipeTransport.enterPipeTransport();
        int consumed;
        try {
            consumed = target.fill(new FluidStack(MANA.get(), toPush), IFluidHandler.FluidAction.EXECUTE);
        } finally {
            ManaPipeTransport.exitPipeTransport();
        }
        if (consumed > 0) drainMana(Math.min(consumed, stored));
    }

    // TANK HELPERS

    private int fillMana(int mb) {
        if (internalTank == null || mb <= 0) return 0;
        int accepted = internalTank.getPrimaryHandler()
                .fill(new FluidStack(MANA.get(), mb), IFluidHandler.FluidAction.EXECUTE);
        if (accepted > 0) {
            setChanged();
            internalTank.sendDataLazily();
        }
        return accepted;
    }

    private int drainMana(int mb) {
        if (internalTank == null || mb <= 0) return 0;
        FluidStack drained = internalTank.getPrimaryHandler()
                .drain(new FluidStack(MANA.get(), mb), IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
            setChanged();
            internalTank.sendDataLazily();
        }
        return drained.getAmount();
    }

    public int storedMana() {
        if (internalTank == null) return 0;
        return internalTank.getPrimaryHandler().getFluidInTank(0).getAmount();
    }
}
