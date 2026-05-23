package net.ttzplayz.create_wizardry.block.blaze_caster;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import io.redspace.ironsspellbooks.api.item.IScroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.ttzplayz.create_wizardry.block.CWBlockEntities;

public class BlazeCasterBlock extends HorizontalDirectionalBlock implements IBE<BlazeCasterBlockEntity>, IWrenchable, SpecialBlockItemRequirement {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BlazeBurnerBlock.HeatLevel> HEAT_LEVEL = BlazeBurnerBlock.HEAT_LEVEL;
    public static final EnumProperty<CasterMode> MODE = EnumProperty.create("mode", CasterMode.class);

    private static final MapCodec<BlazeCasterBlock> CODEC = simpleCodec(BlazeCasterBlock::new);

    public BlazeCasterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.NONE)
                .setValue(FACING, Direction.NORTH)
                .setValue(MODE, CasterMode.SENTRY));
    }

    public static BlazeBurnerBlock.HeatLevel getHeatLevelOf(BlockState blockState) {
        return BlazeBurnerBlock.getHeatLevelOf(blockState);
    }

    public static int getLight(BlockState state) {
        BlazeBurnerBlock.HeatLevel level = BlazeBurnerBlock.getHeatLevelOf(state);
        if (level == BlazeBurnerBlock.HeatLevel.NONE) return 0;
        return level == BlazeBurnerBlock.HeatLevel.SMOULDERING ? 8 : 15;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEAT_LEVEL, FACING, MODE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(MODE, CasterMode.SENTRY);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return AllShapes.HEATER_BLOCK_SHAPE;
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        AdvancementBehaviour.setPlacedBy(worldIn, pos, placer);
        if (placer instanceof Player p)
            withBlockEntityDo(worldIn, pos, be -> be.placerUuid = p.getUUID());
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
        if (!context.getLevel().isClientSide) {
            context.getLevel().setBlockAndUpdate(context.getClickedPos(), state.cycle(MODE));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        return IWrenchable.super.getRotatedBlockState(originalState, targetedFace);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state,
            Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        // Creative blaze cake toggles creative mode
        if (stack.is(AllItems.CREATIVE_BLAZE_CAKE.get())) {
            if (!level.isClientSide)
                withBlockEntityDo(level, pos, BlazeCasterBlockEntity::toggleCreativeHeat);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Shift right-click: hat management
        if (player.isShiftKeyDown()) {
            if (!stack.isEmpty() && isHat(stack)) {
                boolean hatSlotEmpty = getBlockEntityOptional(level, pos)
                        .map(be -> be.heldHat.isEmpty()).orElse(false);
                if (hatSlotEmpty) {
                    if (!level.isClientSide) {
                        withBlockEntityDo(level, pos, be -> {
                            be.heldHat = stack.copyWithCount(1);
                            if (!player.isCreative()) stack.shrink(1);
                            be.notifyUpdate();
                        });
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            } else if (stack.isEmpty()) {
                boolean hasHat = getBlockEntityOptional(level, pos)
                        .map(be -> !be.heldHat.isEmpty()).orElse(false);
                if (hasHat) {
                    if (!level.isClientSide) {
                        withBlockEntityDo(level, pos, be -> {
                            ItemStack give = be.heldHat.copy();
                            be.heldHat = ItemStack.EMPTY;
                            be.notifyUpdate();
                            if (!player.getInventory().add(give)) player.drop(give, false);
                        });
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Insert a spell scroll into the held slot
        if (!stack.isEmpty() && stack.getItem() instanceof IScroll) {
            boolean hasSlot = getBlockEntityOptional(level, pos)
                    .map(be -> be.heldItem.isEmpty()).orElse(false);
            if (hasSlot) {
                if (!level.isClientSide) {
                    withBlockEntityDo(level, pos, be -> {
                        be.heldItem = stack.copyWithCount(1);
                        if (!player.isCreative()) stack.shrink(1);
                        be.notifyUpdate();
                    });
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Retrieve held scroll with empty hand
        if (stack.isEmpty()) {
            boolean hasItem = getBlockEntityOptional(level, pos)
                    .map(be -> !be.heldItem.isEmpty()).orElse(false);
            if (hasItem) {
                if (!level.isClientSide) {
                    withBlockEntityDo(level, pos, be -> {
                        ItemStack toGive = be.heldItem.copy();
                        be.heldItem = ItemStack.EMPTY;
                        be.notifyUpdate();
                        if (!player.getInventory().add(toGive))
                            player.drop(toGive, false);
                    });
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean isHat(ItemStack stack) {
        return stack.is(ItemRegistry.ELECTROMANCER_HELMET.get());
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
