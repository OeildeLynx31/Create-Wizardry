package net.ttzplayz.create_wizardry.block.mana_siphon;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
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

    private static final int DEPLETION_DURATION = 300;

    private static final double SPELL_PULL_SPEED = 0.55;
    private static final double SPELL_CONSUME_DIST = 1.4;
    private static final int SPELL_MANA_PER_DAMAGE = 10;

    private static final int PUMP_MAX_PER_TICK = 128;

    private int scanCooldown = SCAN_INTERVAL;
    private int growthCooldown = 0;

    private float lastPumpSpeed = Float.NaN;
    /** Lazy tank build */
    private final Map<Direction, IFluidHandler> decayingCaps = new HashMap<>();
    private final Map<BlockPos, Integer> eggProgress = new HashMap<>();
    /**mB siphoned from caster out of max-mana pool */
    private final Map<UUID, Integer> casterDrain = new HashMap<>();
    /** Player who placed the siphon (for advancements) */
    public UUID placerUuid;

    public ManaSiphonBlockEntity(BlockPos pos, BlockState state) {
        super(CWBlockEntities.MANA_SIPHON_BE.get(), pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                BLOCK,
                CWBlockEntities.MANA_SIPHON_BE.get(),
                (be, context) -> be.decayingCapability(context));
    }


    private IFluidHandler decayingCapability(Direction side) {
        if (side != Direction.DOWN) return null; // only connect from the bottom
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
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        growthCooldown = compound.getInt("GrowthCooldown");
        placerUuid = compound.hasUUID("Placer") ? compound.getUUID("Placer") : null;
    }

    /** Award an advancement to the placer if they are online */
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
                        + (expanded ? "expanded" : "confined")).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" (" + diameter + "x" + diameter + ")")
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
        if (growthCooldown > 0) growthCooldown--;
        tickCrystallization();

        if (getSpeed() == 0) return;
        tickPump();

        AABB box = currentBox();
        attractSpells(box); // pull every tick

        if (--scanCooldown > 0) return;
        scanCooldown = SCAN_INTERVAL;

        if (bossBreakCheck(box)) return;
        drainEntities(box);
        tickEggs();
    }

    // EXPANDED/CONFINED RADIUS

    private int currentRadius() {
        return getBlockState().getValue(ManaSiphonBlock.EXPANDED)
                ? Config.manaSiphonLargeRadius
                : Config.manaSiphonSmallRadius;
    }

    private AABB currentBox() {
        return new AABB(worldPosition).inflate(currentRadius());
    }

    // BOSS INTERACTIONS

    /** Tyros/Dead King overload and destroy the Siphon; returns true if broken */
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

    // DRAINING

    private void drainEntities(AABB box) {
        int perOp = Config.manaSiphonDrainPerOp;
        Set<UUID> seenCasters = new HashSet<>();
        List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        for (LivingEntity e : living) {
            if (e instanceof DeadKingBoss) {
                fillMana(perOp);
                spawnDrainParticles(e);
                continue; // inf mana if dead king is asleep
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
        // Suppress the caster while it sits in the field, even if our tank is full: zero its mana
        // and (re)apply SIPHON_LOCK. The actual cast block is done by AbstractSpellCastingMobMixin,
        // which no-ops initiateCastSpell while SIPHON_LOCK is present (calling cancelCast() here
        // would instead *complete* the spell, since cancelCast -> castComplete -> onServerCastComplete).
        MagicData md = caster.getMagicData();
        md.setMana(0);
        md.resetCastingState();
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SCAN_INTERVAL + 10, 2, false, false));
        applySiphonLock(caster);

        int accepted = fillMana(Config.manaSiphonDrainPerOp);
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

    private void applySiphonLock(LivingEntity e) {
        e.addEffect(new MobEffectInstance(CWMobEffects.SIPHON_LOCK, SCAN_INTERVAL + 5, 0, true, false, false));
    }

    private void spawnDrainParticles(LivingEntity e) {
        if (level instanceof ServerLevel sl) {
            double ex = e.getX();
            double ey = e.getY() + e.getBbHeight() / 2;
            double ez = e.getZ();
            CWParticles.spawnManaRunes(sl, ex, ey, ez, 6, e.getBbWidth() / 2, 0.06);
            // Rune trail
            Vec3 top = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5);
            CWParticles.spawnManaTrail(sl, new Vec3(ex, ey, ez), top, 8);
        }
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

    // MANA CRYSTALLIZATION

    /**
     * Grows Arcane Essence on the bottom face of a crystalline block two blocks up from the siphon
     * One stage per GROWTH_COOLDOWN_TICKS, costing GROWTH_COST
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

    // PUMP

    private void tickPump() {
        BlockPos below = worldPosition.below();
        FluidTransportBehaviour pipeBelow = FluidPropagator.getPipe(level, below);
        if (pipeBelow != null) {
            maintainDownwardPressure(pipeBelow);
        } else {
            lastPumpSpeed = Float.NaN; // pipe removed; force a fresh apply if one returns
            if (storedMana() > 0) directFillBelow();
        }
    }

    private void maintainDownwardPressure(FluidTransportBehaviour firstPipe) {
        float speed = Math.abs(getSpeed());
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
        BlockPos first = worldPosition.below();
        entryFace.put(first, Direction.UP); // fluid enters the first pipe from the Siphon above
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
        IFluidHandler target = level.getCapability(BLOCK, worldPosition.below(), Direction.UP);
        if (target == null) return;
        int toPush = Math.min(stored, Mth.clamp((int) Math.abs(getSpeed()), 1, PUMP_MAX_PER_TICK));
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

    private int storedMana() {
        if (internalTank == null) return 0;
        return internalTank.getPrimaryHandler().getFluidInTank(0).getAmount();
    }
}
