package net.ttzplayz.create_wizardry.block.blaze_caster;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllShapes;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.ttzplayz.create_wizardry.block.CWBlockEntities;

import static net.ttzplayz.create_wizardry.block.CWShapes.CHANNELER_SHAPE;

public class BlazeCasterBlock extends HorizontalDirectionalBlock implements IBE<BlazeCasterBlockEntity>, IWrenchable, SpecialBlockItemRequirement {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BlazeBurnerBlock.HeatLevel> HEAT_LEVEL = EnumProperty.create("blaze", BlazeBurnerBlock.HeatLevel.class,
            heatLevel -> heatLevel != BlazeBurnerBlock.HeatLevel.NONE);

    private static final MapCodec<BlazeCasterBlock> CODEC = simpleCodec(BlazeCasterBlock::new);

    public BlazeCasterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING)
                .setValue(FACING, Direction.NORTH));
    }

    public static BlazeBurnerBlock.HeatLevel getHeatLevelOf(BlockState blockState) {
        return blockState.hasProperty(HEAT_LEVEL) ? blockState.getValue(HEAT_LEVEL) : BlazeBurnerBlock.HeatLevel.NONE;
    }

    public static int getLight(BlockState state) {
        BlazeBurnerBlock.HeatLevel level = state.getValue(HEAT_LEVEL);
        return level == BlazeBurnerBlock.HeatLevel.SMOULDERING ? 8 : 15;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEAT_LEVEL, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return AllShapes.HEATER_BLOCK_SHAPE;
        //state.getValue(FACING)
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        AdvancementBehaviour.setPlacedBy(worldIn, pos, placer);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return IWrenchable.super.onWrenched(state, context);
    }

    @Override
    public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
        return IWrenchable.super.updateAfterWrenched(newState, context);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        return IWrenchable.super.getRotatedBlockState(originalState, targetedFace);
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
        return null;
    }

    @Override
    public Class<BlazeCasterBlockEntity> getBlockEntityClass() {
        return BlazeCasterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BlazeCasterBlockEntity> getBlockEntityType() {
        return CWBlockEntities.BLAZE_CASTER_BE.get();
    }
}
