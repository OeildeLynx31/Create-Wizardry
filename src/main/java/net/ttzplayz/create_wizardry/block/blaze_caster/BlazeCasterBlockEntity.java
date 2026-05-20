package net.ttzplayz.create_wizardry.block.blaze_caster;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import net.ttzplayz.create_wizardry.client.CWPartialModels;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static net.minecraft.util.ParticleUtils.spawnParticles;

public class BlazeCasterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    protected ItemStack heldItem = ItemStack.EMPTY;
    public SmartFluidTankBehaviour internalTank;
    public final LerpedFloat headAnimation = LerpedFloat.linear();
    public final LerpedFloat headAngle = LerpedFloat.angular();

    public boolean isActive() {
        return false;
    }

    public boolean isCreative() {
        return false;
    }

    public BlazeBurnerBlock.HeatLevel getHeatLevel() {
        return BlazeBurnerBlock.HeatLevel.SMOULDERING;
    }


    public BlazeCasterBlockEntity(BlockPos pos, BlockState state) {
        super(CWBlockEntities.BLAZE_CASTER_BE.get(), pos, state);
    }
    @OnlyIn(Dist.CLIENT)
    public PartialModel getBlazeModel(BlazeBurnerBlock.HeatLevel heatLevel, boolean active) {
        if (heatLevel == BlazeBurnerBlock.HeatLevel.SMOULDERING || heatLevel == BlazeBurnerBlock.HeatLevel.NONE)
            return CWPartialModels.BLAZE_CASTER_INERT;
        return BlazeBurnerRenderer.getBlazeModel(heatLevel, active);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getHatModel(BlazeBurnerBlock.HeatLevel heatLevel) {
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
    protected boolean shouldTickAnimation() {
        return !VisualizationManager.supportsVisualization(level);
    }
    @OnlyIn(Dist.CLIENT)
    protected void tickAnimation() {
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

        if (isCreative())
            return;
        updateBlockState();
    }
    public void updateBlockState() {
        setBlockHeat(getHeatLevel());
    }
    protected void setBlockHeat(BlazeBurnerBlock.HeatLevel newHeat) {
        BlazeBurnerBlock.HeatLevel currentHeat = getHeatLevelFromBlock();
        if (currentHeat == newHeat)
            return;
        assert level != null;
        onHeatChange(currentHeat, newHeat);
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BlazeCasterBlock.HEAT_LEVEL, newHeat));
        notifyUpdate();
    }
    protected void onHeatChange(BlazeBurnerBlock.HeatLevel currentHeat, BlazeBurnerBlock.HeatLevel newHeat) {}

    public BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock() {
        return BlazeCasterBlock.getHeatLevelOf(getBlockState());
    }


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        internalTank = SmartFluidTankBehaviour.single(this, 4000)
                .allowInsertion()
                .allowExtraction();
        behaviours.add(internalTank);
//        this.enchanter = new EnchanterBehaviour(this, new EnchanterTransform(), new TemplateItemTransform());
//        this.advancement = new AdvancementBehaviour(this);
//        behaviours.add(this.enchanter);
//        behaviours.add(this.advancement);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), heldItem);
        }
    }

    protected void spawnParticles(BlazeBurnerBlock.HeatLevel heatLevel) {
        assert level != null;
        if (heatLevel == BlazeBurnerBlock.HeatLevel.NONE)
            return;

        RandomSource random = level.getRandom();

        Vec3 center = VecHelper.getCenterOf(worldPosition);
        Vec3 smokePos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, .125f)
                .multiply(1, 0, 1));

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
}
