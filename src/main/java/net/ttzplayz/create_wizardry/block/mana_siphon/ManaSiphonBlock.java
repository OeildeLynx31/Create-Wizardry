package net.ttzplayz.create_wizardry.block.mana_siphon;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

import static com.simibubi.create.AllShapes.PUMP;
import static net.ttzplayz.create_wizardry.block.CWShapes.CHANNELER_SHAPE;

public class ManaSiphonBlock extends DirectionalKineticBlock implements IBE<ManaSiphonBlockEntity>, ICogWheel, SimpleWaterloggedBlock {

    // expanded radius
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public ManaSiphonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(super.defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(EXPANDED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return PUMP.get(state.getValue(FACING));
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EXPANDED, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof net.minecraft.world.entity.player.Player p) {
            withBlockEntityDo(level, pos, be -> {
                be.placerUuid = p.getUUID();
                be.notifyUpdate();
            });
            if (p instanceof net.minecraft.server.level.ServerPlayer sp && !sp.isFakePlayer())
                net.ttzplayz.create_wizardry.advancement.CWAdvancements.AURA_MONSTER.awardTo(sp);
        }
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
