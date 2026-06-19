package net.ttzplayz.create_wizardry.block.mana_siphon;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.DeadKingBoss;
import io.redspace.ironsspellbooks.entity.mobs.ice_spider.IceSpiderEntity;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.FireBossEntity;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ttzplayz.create_wizardry.Config;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import net.ttzplayz.create_wizardry.block.CWBlocks;
import net.ttzplayz.create_wizardry.block.pipe.ManaPipeTransport;
import net.ttzplayz.create_wizardry.effect.CWMobEffects;
import net.ttzplayz.create_wizardry.entity.CWManaTransformations;
import net.ttzplayz.create_wizardry.particle.CWParticles;
import net.ttzplayz.create_wizardry.spell.CWTags;

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
    /** Rotational stress this machine draws from its kinetic network. */
    private static final float STRESS_IMPACT = 4f;
    /** Mana cost charged from the tank per Arcane Essence growth stage. */
    private static final int GROWTH_COST = 50;
    /** Cooldown between Arcane Essence growth stages (15 s). */
    private static final int GROWTH_COOLDOWN_TICKS = 300;
    /** Scans of draining before an Ice Spider Egg becomes a Turtle Egg. */
    private static final int EGG_DRAIN_STEPS = 30;
    /** Player Depletion duration in ticks (60s). */
    private static final int DEPLETION_DURATION = 1200;
    /** Spell handling. */
    private static final double SPELL_PULL_SPEED = 0.55;
    private static final double SPELL_CONSUME_DIST = 1.4;
    private static final int SPELL_MANA_PER_DAMAGE = 10;
    /** Cap on mana pushed downward per tick; the pump scales toward this with rotation speed. */
    private static final int PUMP_MAX_PER_TICK = 128;
    /** Ticks between re-evaluating the pump's downward destination. */
    private static final int PUMP_REFRESH_TICKS = 20;

    private int scanCooldown = SCAN_INTERVAL;
    private int growthCooldown = 0;
    private int pumpRefresh = 0;
    private ManaPipeTransport.Destination pumpDest;
    /** Per-side leak-aware tank capabilities, built lazily. */
    private final Map<Direction, IFluidHandler> decayingCaps = new HashMap<>();
    private final Map<BlockPos, Integer> eggProgress = new HashMap<>();
    /** Cumulative mB siphoned from each caster mob, keyed by UUID, toward its max-mana pool. */
    private final Map<UUID, Integer> casterDrain = new HashMap<>();

    public ManaSiphonBlockEntity(BlockPos pos, BlockState state) {
        super(CWBlockEntities.MANA_SIPHON_BE.get(), pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                BLOCK,
                CWBlockEntities.MANA_SIPHON_BE.get(),
                (be, context) -> be.decayingCapability(context));
    }

    /** Tank capability that leaks mana arriving through copper pipes (cached per side). */
    private IFluidHandler decayingCapability(Direction side) {
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
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        growthCooldown = compound.getInt("GrowthCooldown");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (internalTank == null) return added;
        int radius = currentRadius();
        int diameter = radius * 2 + 1;
        tooltip.add(Component.translatable("block.create_wizardry.mana_siphon")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" ")
                .append(Component.literal(diameter + "x" + diameter + " range")
                        .withStyle(ChatFormatting.DARK_GRAY)));
        containedFluidTooltip(tooltip, isPlayerSneaking, internalTank.getPrimaryHandler());
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        tickServer();
    }

    private void tickServer() {
        // Powered to function: nothing happens unless the wheel is being spun.
        if (getSpeed() == 0) return;
        if (growthCooldown > 0) growthCooldown--;

        tickPump();

        AABB box = currentBox();
        attractSpells(box); // spells move fast; pull every tick

        if (--scanCooldown > 0) return;
        scanCooldown = SCAN_INTERVAL;

        if (bossBreakCheck(box)) return;
        drainEntities(box);
        tickEggs();
        tickCrystallization();
    }

    // --- Geometry ----------------------------------------------------------

    private int currentRadius() {
        return getBlockState().getValue(ManaSiphonBlock.EXPANDED)
                ? Config.manaSiphonLargeRadius
                : Config.manaSiphonSmallRadius;
    }

    private AABB currentBox() {
        return new AABB(worldPosition).inflate(currentRadius());
    }

    // --- Boss interactions -------------------------------------------------

    /** Tyros, or an activated Dead King, overload and destroy the Siphon. Returns true if broken. */
    private boolean bossBreakCheck(AABB box) {
        boolean overload = !level.getEntitiesOfClass(FireBossEntity.class, box).isEmpty();
        if (!overload) {
            for (DeadKingBoss dk : level.getEntitiesOfClass(DeadKingBoss.class, box)) {
                if (!dk.isPhase(DeadKingBoss.Phases.FirstPhase)) {
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

    // --- Draining ----------------------------------------------------------

    private void drainEntities(AABB box) {
        int perOp = Config.manaSiphonDrainPerOp;
        Set<UUID> seenCasters = new HashSet<>();
        List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        for (LivingEntity e : living) {
            if (e instanceof DeadKingBoss) {
                // Only the dormant Dead King reaches here (activated breaks the block above):
                // an infinite, unchanging mana source.
                fillMana(perOp);
                spawnDrainParticles(e);
                continue;
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
                applySiphonLock(player);
                drainPlayer(player);
            }
        }
        // Forget progress for casters that left the radius / died.
        if (!casterDrain.isEmpty()) {
            casterDrain.keySet().retainAll(seenCasters);
        }
    }

    private void drainCaster(AbstractSpellCastingMob caster) {
        int accepted = fillMana(Config.manaSiphonDrainPerOp);
        if (accepted <= 0) return; // tank full

        // Disrupt: cannot cast, slowed, regen-locked and visibly shaking while drained.
        MagicData md = caster.getMagicData();
        md.setMana(0);
        md.resetCastingState();
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SCAN_INTERVAL + 10, 2, false, false));
        applySiphonLock(caster);
        shake(caster);
        spawnDrainParticles(caster);

        // Drain gradually against the mob's max-mana pool; transform only once fully siphoned.
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
            Mob result = spider.convertTo(EntityType.SPIDER, false);
            if (result != null) {
                CWParticles.spawnManaRunes(sl, result.getX(), result.getY() + result.getBbHeight() / 2,
                        result.getZ(), 16, result.getBbWidth() / 2, 0.1);
                sl.playSound(null, result.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.0F, 0.8F);
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
            int ratio = Config.playerManaPerMb;
            int tankSpace = CAPACITY - storedMana();
            int mbWanted = Math.min(Config.manaSiphonDrainPerOp, Math.min(tankSpace, (int) (mana / ratio)));
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

    /** Zeroes mana regen for a short, self-refreshing window so anything in range can't regen while siphoned. */
    private void applySiphonLock(LivingEntity e) {
        e.addEffect(new MobEffectInstance(CWMobEffects.SIPHON_LOCK, SCAN_INTERVAL + 5, 0, true, false, false));
    }

    private void shake(LivingEntity e) {
        double jx = (level.random.nextDouble() - 0.5) * 0.2;
        double jz = (level.random.nextDouble() - 0.5) * 0.2;
        e.setDeltaMovement(e.getDeltaMovement().add(jx, 0, jz));
        e.hurtMarked = true;
    }

    private void spawnDrainParticles(LivingEntity e) {
        if (level instanceof ServerLevel sl) {
            double ex = e.getX();
            double ey = e.getY() + e.getBbHeight() / 2;
            double ez = e.getZ();
            CWParticles.spawnManaRunes(sl, ex, ey, ez, 6, e.getBbWidth() / 2, 0.06);
            // Trail of runes connecting the drained entity to the top of the Siphon.
            Vec3 top = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5);
            CWParticles.spawnManaTrail(sl, new Vec3(ex, ey, ez), top, 8);
        }
    }

    // --- Spell attraction & consumption -----------------------------------

    private void attractSpells(AABB box) {
        List<AbstractMagicProjectile> spells = level.getEntitiesOfClass(AbstractMagicProjectile.class, box);
        if (spells.isEmpty()) return;
        Vec3 center = Vec3.atCenterOf(worldPosition);
        for (AbstractMagicProjectile spell : spells) {
            Vec3 toCenter = center.subtract(spell.position());
            double dist = toCenter.length();
            if (dist < SPELL_CONSUME_DIST) {
                int half = Math.max(1, Math.round(spell.getDamage()) * SPELL_MANA_PER_DAMAGE / 2);
                fillMana(half);
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

    // --- Ice Spider Eggs ---------------------------------------------------

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
        if (fillMana(Config.manaSiphonDrainPerOp) <= 0) return; // tank full

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
            if (level instanceof ServerLevel sl) {
                CWParticles.spawnManaRunes(sl, found.getX() + 0.5, found.getY() + 0.5, found.getZ() + 0.5, 4, 0.2, 0.05);
            }
        }
    }

    // --- Crystallization ---------------------------------------------------

    /**
     * Grows Arcane Essence in the gap directly above the Siphon, on the bottom face of a crystalline
     * block placed two blocks up. One stage per {@link #GROWTH_COOLDOWN_TICKS}, costing {@link #GROWTH_COST}.
     */
    private void tickCrystallization() {
        if (growthCooldown > 0) return;
        if (storedMana() < GROWTH_COST) return;

        BlockPos host = worldPosition.above(2);
        if (!level.getBlockState(host).is(CWTags.Blocks.CRYSTALLINE)) return;

        BlockPos clusterPos = worldPosition.above(1);
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
                    .setValue(ArcaneEssenceClusterBlock.FACING, Direction.DOWN)
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

    // --- Mana pump ---------------------------------------------------------

    /**
     * Acts as a downward-facing pump: pushes stored mana out the underside, along the connected
     * pipe network, into the nearest tank. The leak through copper pipes is applied by the
     * destination tank's {@link ManaPipeTransport.DecayingManaTank} wrapper, so the pump just
     * pushes raw mana and drains exactly what the tank reports consumed. Idle when nothing is
     * reachable below.
     */
    private void tickPump() {
        int stored = storedMana();
        if (stored <= 0) return;

        if (--pumpRefresh <= 0 || pumpDest == null) {
            pumpDest = ManaPipeTransport.findManaDestination(level, worldPosition, Direction.DOWN);
            pumpRefresh = PUMP_REFRESH_TICKS;
        }
        ManaPipeTransport.Destination dest = pumpDest;
        if (dest == null) return;

        IFluidHandler target = level.getCapability(BLOCK, dest.pos(), dest.fillSide());
        if (target == null) {
            pumpDest = null;
            return;
        }

        int toPush = Math.min(stored, Mth.clamp((int) Math.abs(getSpeed()), 1, PUMP_MAX_PER_TICK));
        // Mark this as pipe transport so the destination's wrapper applies the copper-pipe leak.
        ManaPipeTransport.enterPipeTransport();
        int consumed;
        try {
            consumed = target.fill(new FluidStack(MANA.get(), toPush), IFluidHandler.FluidAction.EXECUTE);
        } finally {
            ManaPipeTransport.exitPipeTransport();
        }
        if (consumed > 0) drainMana(Math.min(consumed, stored));
    }

    // --- Tank helpers ------------------------------------------------------

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

    private int storedMana() {
        if (internalTank == null) return 0;
        return internalTank.getPrimaryHandler().getFluidInTank(0).getAmount();
    }
}
