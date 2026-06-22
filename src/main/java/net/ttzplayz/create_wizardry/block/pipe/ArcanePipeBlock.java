package net.ttzplayz.create_wizardry.block.pipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import net.ttzplayz.create_wizardry.block.CWBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// arcane pipe
public class ArcanePipeBlock extends FluidPipeBlock {

    public static final MapCodec<ArcanePipeBlock> CODEC = simpleCodec(ArcanePipeBlock::new);

    public ArcanePipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (tryRemoveBracket(context))
            return InteractionResult.SUCCESS;

        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        Axis axis = getStraightAxis(state);
        if (axis == null) {
            Vec3 clickLocation = context.getClickLocation()
                .subtract(pos.getX(), pos.getY(), pos.getZ());
            double closest = Float.MAX_VALUE;
            Direction argClosest = Direction.UP;
            for (Direction direction : Iterate.directions) {
                if (clickedFace.getAxis() == direction.getAxis())
                    continue;
                Vec3 centerOf = Vec3.atCenterOf(direction.getNormal());
                double distance = centerOf.distanceToSqr(clickLocation);
                if (distance < closest) {
                    closest = distance;
                    argClosest = direction;
                }
            }
            axis = argClosest.getAxis();
        }

        if (clickedFace.getAxis() == axis)
            return InteractionResult.PASS;
        if (!world.isClientSide) {
            withBlockEntityDo(world, pos, fpte -> fpte.getBehaviour(FluidTransportBehaviour.TYPE).interfaces.values()
                .stream()
                .filter(pc -> pc != null && pc.hasFlow())
                .findAny()
                .ifPresent($ -> AllAdvancements.GLASS_PIPE.awardTo(context.getPlayer())));

            FluidTransportBehaviour.cacheFlows(world, pos);
            world.setBlockAndUpdate(pos, CWBlocks.GLASS_ARCANE_PIPE.get()
                .defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, axis)
                .setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED)));
            FluidTransportBehaviour.loadFlows(world, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private static Axis getStraightAxis(BlockState state) {
        return FluidPropagator.getStraightPipeAxis(state);
    }

    @Override
    public Class<FluidPipeBlockEntity> getBlockEntityClass() {
        return FluidPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return CWBlockEntities.ARCANE_PIPE.get();
    }

    @Override
    protected @NotNull MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }
}
