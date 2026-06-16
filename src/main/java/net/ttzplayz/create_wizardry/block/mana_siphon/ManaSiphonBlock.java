package net.ttzplayz.create_wizardry.block.mana_siphon;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

public class ManaSiphonBlock extends KineticBlock implements IBE<ManaSiphonBlockEntity>, ICogWheel, SimpleWaterloggedBlock {

    /** false = small (3x3) drain area, true = expanded (7x7). */
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public ManaSiphonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(EXPANDED, false)
                .setValue(WATERLOGGED, false));
    }

    // --- Kinetics: a small cog on the vertical axis (Millstone-style), driven by an adjacent
    //     vertical cogwheel. No shaft on any face, leaving the underside free for the mana pump. ---

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EXPANDED, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide) {
            BlockState newState = state.cycle(EXPANDED);
            level.setBlockAndUpdate(pos, newState);
            level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.6F,
                    newState.getValue(EXPANDED) ? 1.2F : 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public Class<ManaSiphonBlockEntity> getBlockEntityClass() {
        return ManaSiphonBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ManaSiphonBlockEntity> getBlockEntityType() {
        return CWBlockEntities.MANA_SIPHON_BE.get();
    }
}
