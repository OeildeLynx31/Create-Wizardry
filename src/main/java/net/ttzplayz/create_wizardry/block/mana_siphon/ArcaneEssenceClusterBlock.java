package net.ttzplayz.create_wizardry.block.mana_siphon;

import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ArcaneEssenceClusterBlock extends Block implements SimpleWaterloggedBlock {

    public static final int MAX_AGE = 2;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_AGE);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape[] SHAPES_BY_AGE = new VoxelShape[] {
            Block.box(5, 0, 5, 11, 4, 11),   // age 0 (bud)
            Block.box(4, 0, 4, 12, 8, 12),   // age 1 (cluster)
            Block.box(3, 0, 3, 13, 12, 13),  // age 2 (crystal)
    };

    public ArcaneEssenceClusterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AGE, 0)
                .setValue(FACING, Direction.DOWN)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, FACING, WATERLOGGED);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        return rotateShapeForFace(SHAPES_BY_AGE[state.getValue(AGE)], direction);
    }

    private static VoxelShape rotateShapeForFace(VoxelShape upShape, Direction face) {
        // Cheap approximation: the up-facing shapes read fine on any face for a placeholder model.
        return upShape;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos attachedTo = pos.relative(facing.getOpposite());
        return level.getBlockState(attachedTo).isFaceSturdy(level, attachedTo, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            int count = harvestYield(state.getValue(AGE), level.getRandom());
            popResource(level, pos, new ItemStack(ItemRegistry.ARCANE_ESSENCE.get(), count));
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            // Reset to a fresh bud on its host so it can regrow.
            level.setBlockAndUpdate(pos, state.setValue(AGE, 0));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Essence dropped per stage: bud 1-2, cluster 3-4, crystal 5-7. */
    private static int harvestYield(int age, RandomSource random) {
        return switch (age) {
            case 0 -> 1 + random.nextInt(2);   // 1-2
            case 1 -> 3 + random.nextInt(2);   // 3-4
            default -> 5 + random.nextInt(3);  // 5-7
        };
    }
}
