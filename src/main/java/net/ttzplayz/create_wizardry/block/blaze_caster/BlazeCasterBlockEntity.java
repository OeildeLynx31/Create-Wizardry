package net.ttzplayz.create_wizardry.block.blaze_caster;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import net.ttzplayz.create_wizardry.client.CWPartialModels;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class BlazeCasterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    protected ItemStack heldItem = ItemStack.EMPTY;
    protected ItemStack heldHat = ItemStack.EMPTY;
    public SmartFluidTankBehaviour internalTank;
    public final LerpedFloat headAnimation = LerpedFloat.linear();
    public final LerpedFloat headAngle = LerpedFloat.angular();

    protected boolean creative = false;
    protected int castTicksRemaining = 0;
    protected int cooldownTicksRemaining = 0;
    @Nullable protected UUID placerUuid;
    protected boolean wasPowered = false;

    public boolean isActive() {
        return castTicksRemaining > 0;
    }

    public boolean isCreative() {
        return creative;
    }

    public BlazeBurnerBlock.HeatLevel getHeatLevel() {
        if (castTicksRemaining > 0) return BlazeBurnerBlock.HeatLevel.FADING;
        boolean hasMana = creative || (internalTank != null
                && internalTank.getPrimaryHandler().getFluidInTank(0).getAmount() > 0);
        return hasMana ? BlazeBurnerBlock.HeatLevel.SMOULDERING : BlazeBurnerBlock.HeatLevel.NONE;
    }

    public void toggleCreativeHeat() {
        creative = !creative;
        updateBlockState();
        notifyUpdate();
    }

    public BlazeCasterBlockEntity(BlockPos pos, BlockState state) {
        super(CWBlockEntities.BLAZE_CASTER_BE.get(), pos, state);
        headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(BlazeBurnerBlock.FACING)
                .orElse(Direction.SOUTH)) + 180) % 360);
    }

    @OnlyIn(Dist.CLIENT)
    public PartialModel getBlazeModel(BlazeBurnerBlock.HeatLevel heatLevel, boolean active) {
        if (!heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING))
            return CWPartialModels.BLAZE_CASTER_INERT;
        String element = getElementId();
        return CWPartialModels.BLAZE_BY_ELEMENT.getOrDefault(element, CWPartialModels.BLAZE_CASTER_NONE);
    }

    public String getElementId() {
        if (heldItem.isEmpty()) return "none";
        ISpellContainer container = ISpellContainer.get(heldItem);
        if (container == null || container.isEmpty()) return "none";
        SpellData sd = container.getSpellAtIndex(0);
        if (sd == null || sd == SpellData.EMPTY) return "none";
        AbstractSpell spell = sd.getSpell();
        if (spell == null) return "none";
        SchoolType school = spell.getSchoolType();
        if (school == null) return "none";
        String path = school.getId().getPath();
        return CWPartialModels.BLAZE_BY_ELEMENT.containsKey(path) ? path : "none";
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getHatModel(BlazeBurnerBlock.HeatLevel heatLevel) {
        if (heldHat.isEmpty()) return null;
        return heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)
                ? CWPartialModels.ELECTROMANCER_HAT
                : CWPartialModels.ELECTROMANCER_HAT_SMALL;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getGogglesModel(BlazeBurnerBlock.HeatLevel heatLevel) {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getEyesModel(BlazeBurnerBlock.HeatLevel heatLevel) {
        if (!heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING)) return null;
        return heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)
                ? CWPartialModels.BLAZE_CASTER_ACTIVE_EYES
                : CWPartialModels.BLAZE_CASTER_IDLE_EYES;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean showed = false;
        if (internalTank != null)
            showed = containedFluidTooltip(tooltip, isPlayerSneaking, internalTank.getPrimaryHandler());

        if (!heldItem.isEmpty()) {
            ISpellContainer container = ISpellContainer.get(heldItem);
            if (container != null && !container.isEmpty()) {
                SpellData sd = container.getSpellAtIndex(0);
                if (sd != null && sd != SpellData.EMPTY) {
                    tooltip.add(Component.translatable("create_wizardry.tooltip.spell",
                            Component.translatable(sd.getSpell().getComponentId()))
                            .withStyle(ChatFormatting.GRAY));
                    showed = true;
                }
            }
        } else {
            tooltip.add(Component.translatable("create_wizardry.tooltip.no_scroll")
                    .withStyle(ChatFormatting.DARK_GRAY));
            showed = true;
        }

        if (!heldHat.isEmpty()) {
            tooltip.add(Component.translatable("create_wizardry.tooltip.hat",
                    heldHat.getHoverName()).withStyle(ChatFormatting.GRAY));
            showed = true;
        }

        CasterMode mode = getBlockState().getValue(BlazeCasterBlock.MODE);
        tooltip.add(Component.translatable("create_wizardry.tooltip.mode." + mode.getSerializedName())
                .withStyle(ChatFormatting.AQUA));
        if (creative) {
            tooltip.add(Component.translatable("create_wizardry.tooltip.creative_mode")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            showed = true;
        }
        return showed;
    }

    @OnlyIn(Dist.CLIENT)
    protected boolean shouldTickAnimation() {
        return !VisualizationManager.supportsVisualization(level);
    }

    @OnlyIn(Dist.CLIENT)
    public void tickAnimation() {
        boolean active = getHeatLevelFromBlock().isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && isActive();
        if (active) {
            headAngle.chase((AngleHelper.horizontalAngle(getBlockState()
                    .getOptionalValue(BlazeBurnerBlock.FACING)
                    .orElse(Direction.SOUTH)) + 180) % 360, .125f, LerpedFloat.Chaser.EXP);
            headAngle.tickChaser();
        } else {
            float target = 0;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && !player.isInvisible()) {
                double x;
                double z;
                if (isVirtual()) {
                    x = -4;
                    z = -10;
                } else {
                    x = player.getX();
                    z = player.getZ();
                }
                double dx = x - (getBlockPos().getX() + 0.5);
                double dz = z - (getBlockPos().getZ() + 0.5);
                target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
            }
            target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
            headAngle.chase(target, .25f, LerpedFloat.Chaser.exp(5));
            headAngle.tickChaser();
        }

        headAnimation.chase(active ? 1 : 0, .25f, LerpedFloat.Chaser.exp(.25f));
        headAnimation.tickChaser();
    }

    @Override
    public void tick() {
        super.tick();
        assert level != null;
        if (level.isClientSide) {
            if (shouldTickAnimation())
                tickAnimation();
            if (!isVirtual())
                spawnParticles(getHeatLevelFromBlock());
            return;
        }

        // Cooldown ticks down unconditionally
        if (cooldownTicksRemaining > 0)
            cooldownTicksRemaining--;

        // Resolve spell from held scroll
        if (heldItem.isEmpty()) { cancelCast(); return; }
        ISpellContainer container = ISpellContainer.get(heldItem);
        if (container == null || container.isEmpty()) { cancelCast(); return; }
        SpellData sd = container.getSpellAtIndex(0);
        if (sd == null || sd == SpellData.EMPTY) { cancelCast(); return; }
        AbstractSpell spell = sd.getSpell();
        int spellLevel = sd.getLevel();

        CasterMode mode = getBlockState().getValue(BlazeCasterBlock.MODE);

        if (mode == CasterMode.IMPULSE) {
            boolean powered = level.hasNeighborSignal(worldPosition);
            boolean risingEdge = powered && !wasPowered;
            wasPowered = powered;
            if (castTicksRemaining > 0) {
                castTicksRemaining--;
                if (castTicksRemaining == 0) {
                    executeCast(spell, spellLevel, null);
                    cooldownTicksRemaining = spell.getSpellCooldown();
                }
            } else if (risingEdge) {
                tryStartCast(spell, spellLevel);
            }
            updateBlockState();
            return;
        }

        // Sentry mode
        LivingEntity target = findTarget(16.0);
        if (castTicksRemaining > 0) {
            castTicksRemaining--;
            if (castTicksRemaining == 0) {
                executeCast(spell, spellLevel, target);
                cooldownTicksRemaining = spell.getSpellCooldown();
                if (target != null)
                    tryStartCast(spell, spellLevel);
            }
        } else if (target != null) {
            tryStartCast(spell, spellLevel);
        }
        updateBlockState();
    }

    private void cancelCast() {
        if (castTicksRemaining > 0) {
            castTicksRemaining = 0;
            updateBlockState();
        }
    }

    private void tryStartCast(AbstractSpell spell, int spellLevel) {
        if (cooldownTicksRemaining > 0) return;
        int manaCost = spell.getManaCost(spellLevel) * 10;
        IFluidHandler handler = internalTank.getPrimaryHandler();
        if (!creative && handler.getFluidInTank(0).getAmount() < manaCost) return;
        if (!creative)
            handler.drain(manaCost, IFluidHandler.FluidAction.EXECUTE);
        castTicksRemaining = Math.max(1, spell.getCastTime(spellLevel));
    }

    @Nullable
    private LivingEntity findTarget(double range) {
        if (!(level instanceof ServerLevel)) return null;
        AABB box = new AABB(worldPosition).inflate(range);
        return level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive()
                // if placerUuid is null (UUID not yet captured), exclude all players as a safe default
                && !(e instanceof Player p && (placerUuid == null || p.getUUID().equals(placerUuid)))
        ).stream()
         .min(Comparator.comparingDouble(e -> e.distanceToSqr(
                 worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)))
         .orElse(null);
    }

    private void executeCast(AbstractSpell spell, int spellLevel, @Nullable LivingEntity target) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ArmorStand proxy = new ArmorStand(EntityType.ARMOR_STAND, serverLevel);
        proxy.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 0.75, worldPosition.getZ() + 0.5);
        if (target != null) {
            double dx = target.getX() - proxy.getX();
            double dy = target.getEyeY() - proxy.getEyeY();
            double dz = target.getZ() - proxy.getZ();
            proxy.setYRot((float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f);
            proxy.setXRot((float) (-Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI)));
        } else {
            Direction facing = getBlockState().getValue(BlazeCasterBlock.FACING);
            proxy.setYRot(switch (facing) {
                case NORTH -> 180f;
                case SOUTH -> 0f;
                case EAST -> -90f;
                default -> 90f;
            });
            proxy.setXRot(0f);
        }
        spell.onCast(serverLevel, spellLevel, proxy, CastSource.MOB, new MagicData(true));
    }

    public void updateBlockState() {
        setBlockHeat(getHeatLevel());
    }

    protected void setBlockHeat(BlazeBurnerBlock.HeatLevel newHeat) {
        if (level == null) return;
        BlockState currentState = level.getBlockState(worldPosition);
        BlazeBurnerBlock.HeatLevel currentHeat = BlazeCasterBlock.getHeatLevelOf(currentState);
        if (currentHeat == newHeat)
            return;
        onHeatChange(currentHeat, newHeat);
        level.setBlockAndUpdate(worldPosition, currentState.setValue(BlazeCasterBlock.HEAT_LEVEL, newHeat));
        notifyUpdate();
    }

    protected void onHeatChange(BlazeBurnerBlock.HeatLevel currentHeat, BlazeBurnerBlock.HeatLevel newHeat) {}

    public BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock() {
        if (level != null) return BlazeCasterBlock.getHeatLevelOf(level.getBlockState(worldPosition));
        return BlazeCasterBlock.getHeatLevelOf(getBlockState());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        internalTank = SmartFluidTankBehaviour.single(this, 4000)
                .allowInsertion()
                .allowExtraction()
                .whenFluidUpdates(() -> {
                    IFluidHandler h = internalTank.getPrimaryHandler();
                    FluidStack current = h.getFluidInTank(0);
                    if (!current.isEmpty() && current.getFluid().getFluidType() != CWFluidRegistry.MANA_TYPE.get())
                        h.drain(current.getAmount(), IFluidHandler.FluidAction.EXECUTE);
                    updateBlockState();
                });
        behaviours.add(internalTank);
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putBoolean("Creative", creative);
        if (!heldItem.isEmpty())
            compound.put("HeldItem", heldItem.save(registries));
        if (!heldHat.isEmpty())
            compound.put("HeldHat", heldHat.save(registries));
        compound.putInt("CastTicks", castTicksRemaining);
        compound.putInt("CooldownTicks", cooldownTicksRemaining);
        if (placerUuid != null)
            compound.putUUID("PlacerUuid", placerUuid);
        compound.putBoolean("WasPowered", wasPowered);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        creative = compound.getBoolean("Creative");
        heldItem = compound.contains("HeldItem")
                ? ItemStack.parseOptional(registries, compound.getCompound("HeldItem"))
                : ItemStack.EMPTY;
        heldHat = compound.contains("HeldHat")
                ? ItemStack.parseOptional(registries, compound.getCompound("HeldHat"))
                : ItemStack.EMPTY;
        castTicksRemaining = compound.getInt("CastTicks");
        cooldownTicksRemaining = compound.getInt("CooldownTicks");
        placerUuid = compound.hasUUID("PlacerUuid") ? compound.getUUID("PlacerUuid") : null;
        wasPowered = compound.getBoolean("WasPowered");
        super.read(compound, registries, clientPacket);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), heldItem);
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), heldHat);
        }
    }

    protected void spawnParticles(BlazeBurnerBlock.HeatLevel heatLevel) {
        assert level != null;
        if (heatLevel == BlazeBurnerBlock.HeatLevel.NONE)
            return;

        RandomSource random = level.getRandom();
        Vec3 center = VecHelper.getCenterOf(worldPosition);
        Vec3 smokePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, .125f).multiply(1, 0, 1));

        if (random.nextInt(4) != 0)
            return;

        boolean empty = level.getBlockState(worldPosition.above())
                .getCollisionShape(level, worldPosition.above())
                .isEmpty();

        if (empty || random.nextInt(8) == 0)
            level.addParticle(ParticleTypes.LARGE_SMOKE, smokePos.x, smokePos.y, smokePos.z, 0, 0, 0);

        double yMotion = empty ? .0625f : random.nextDouble() * .0125f;
        Vec3 flamePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, .5f)
                        .multiply(1, .25f, 1)
                        .normalize()
                        .scale((empty ? .25f : .5) + random.nextDouble() * .125f))
                .add(0, .5, 0);

        if (heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.SEETHING)) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, flamePos.x, flamePos.y, flamePos.z, 0, yMotion, 0);
        } else if (heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) {
            level.addParticle(ParticleTypes.FLAME, flamePos.x, flamePos.y, flamePos.z, 0, yMotion, 0);
        }
    }

    protected void spawnParticleBurst(boolean soul) {
        assert level != null;
        Vec3 c = VecHelper.getCenterOf(worldPosition);
        RandomSource random = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, .5f)
                    .multiply(1, .25f, 1)
                    .normalize();
            Vec3 pos = c.add(offset.scale(.5 + random.nextDouble() * .125f)).add(0, .125, 0);
            Vec3 motion = offset.scale(1 / 32f);

            level.addParticle(soul ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                    pos.x, pos.y, pos.z,
                    motion.x, motion.y, motion.z);
        }
    }

    @Override
    public <T> boolean hasData(Supplier<AttachmentType<T>> type) {
        return super.hasData(type);
    }

    @Override
    public <T> T getData(Supplier<AttachmentType<T>> type) {
        return super.getData(type);
    }

    @Override
    public @Nullable <T> T setData(Supplier<AttachmentType<T>> type, T data) {
        return super.setData(type, data);
    }

    @Override
    public ModelData getModelData() {
        return super.getModelData();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CWBlockEntities.BLAZE_CASTER_BE.get(),
                (be, context) -> {
                    if (be.internalTank == null) return null;
                    return be.internalTank.getCapability();
                }
        );
    }
}
