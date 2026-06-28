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
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import net.ttzplayz.create_wizardry.item.CWItems;

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
            withBlockEntityDo(worldIn, pos, be -> {
                be.placerUuid = p.getUUID();
                be.notifyUpdate();
            });
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        // toggle caster mode
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, state.cycle(MODE));
            withBlockEntityDo(level, pos, be -> {
                be.lockedHead = false;
                be.notifyUpdate();
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
        return IWrenchable.super.updateAfterWrenched(newState, context);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // impulse: lock/unlock the head toward the player
        if (state.getValue(MODE) == CasterMode.IMPULSE) {
            if (!level.isClientSide) {
                Player player = context.getPlayer();
                double dx = player != null ? player.getX() - (pos.getX() + 0.5) : 0;
                double dz = player != null ? player.getZ() - (pos.getZ() + 0.5) : 0;
                final float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
                withBlockEntityDo(level, pos, be -> {
                    be.lockedHead = !be.lockedHead;
                    if (be.lockedHead) be.lockedYaw = yaw;
                    be.notifyUpdate();
                });
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // sentry: pick the block up
        return IWrenchable.super.onSneakWrenched(state, context);
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

        // Caster's Scone superheats the caster for a few minutes (½ cooldown, +10% damage,
        // unlocks eldritch spells)
        if (stack.is(CWItems.CASTERS_SCONE.get())) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, BlazeCasterBlockEntity::applySuperheat);
                if (!player.isCreative()) stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Shift right-click: hat management
        if (player.isShiftKeyDown()) {
            if (!stack.isEmpty() && isHat(stack)) {
                // consume the click so armor auto-equip never fires; swap is server-only
                if (!level.isClientSide) {
                    withBlockEntityDo(level, pos, be -> {
                        ItemStack previous = be.heldHat.copy();
                        be.heldHat = stack.copyWithCount(1);
                        returnOrDrop(player, previous);
                        if (!player.isCreative()) stack.shrink(1);
                        be.updateTankCapacity();
                        be.notifyUpdate();
                    });
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            } else if (stack.isEmpty()) {
                boolean hasHat = getBlockEntityOptional(level, pos)
                        .map(be -> !be.heldHat.isEmpty()).orElse(false);
                if (hasHat) {
                    if (!level.isClientSide) {
                        withBlockEntityDo(level, pos, be -> {
                            ItemStack give = be.heldHat.copy();
                            be.heldHat = ItemStack.EMPTY;
                            be.updateTankCapacity();
                            be.notifyUpdate();
                            if (!player.getInventory().add(give)) player.drop(give, false);
                        });
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Insert (or swap) a hat into the hat slot (non-shift right-click)
        if (!stack.isEmpty() && isHat(stack)) {
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, be -> {
                    ItemStack previous = be.heldHat.copy();
                    be.heldHat = stack.copyWithCount(1);
                    returnOrDrop(player, previous);
                    if (!player.isCreative()) stack.shrink(1);
                    be.updateTankCapacity();
                    be.notifyUpdate();
                });
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Insert (or swap) a spell scroll into the held slot
        if (!stack.isEmpty() && stack.getItem() instanceof IScroll) {
            // Block insertion of spells incompatible with the Blaze Caster
            ISpellContainer container = ISpellContainer.get(stack);
            if (container != null && !container.isEmpty()) {
                SpellData sd = container.getSpellAtIndex(0);
                if (sd != null && sd != SpellData.EMPTY
                        && BlazeCasterBlockEntity.isSpellBlacklisted(sd.getSpell())) {
                    if (!level.isClientSide)
                        player.displayClientMessage(
                            Component.translatable("create_wizardry.message.spell_incompatible"), true);
                    // Consume the interaction so the scroll never casts: the Blaze Caster takes
                    // full priority over the scroll's right-click cast, so a misclick can't waste
                    // it. (Returning FAIL would not consume the action and the cast would fire.)
                    // The incompatible scroll is intentionally left in hand, not inserted.
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            if (!level.isClientSide) {
                withBlockEntityDo(level, pos, be -> {
                    // Swap out whatever scroll is currently held (empty if none)
                    ItemStack previous = be.heldItem.copy();
                    be.heldItem = stack.copyWithCount(1);
                    // Return the old scroll before consuming the new one. add() still stacks it
                    // onto a matching scroll if the player already has one; otherwise, with a full
                    // inventory it drops on the ground rather than being absorbed into the slot
                    // that shrink() would have freed.
                    returnOrDrop(player, previous);
                    if (!player.isCreative()) stack.shrink(1);
                    be.notifyUpdate();
                });
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
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

    private static final TagKey<Item> WIZARD_HATS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "armors/helmets"));

    private static boolean isHat(ItemStack stack) {
        return stack.is(WIZARD_HATS);
    }

    // Return a swapped-out item to the player, or drop it at their feet if it won't fit.
    // add() merges onto a matching stack (or fills an empty slot); in survival it returns false
    // with the remainder left in `stack` when nothing fits. In creative it instead silently voids
    // an unplaceable stack (and returns true), so only call add() when there is real room and drop
    // otherwise. `stack` is always a single item here, so any available room fits it fully — this
    // keeps the "stack onto a matching scroll" behavior even in creative.
    private static void returnOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        Inventory inv = player.getInventory();
        if (inv.getSlotWithRemainingSpace(stack) >= 0 || inv.getFreeSlot() >= 0)
            inv.add(stack);
        else
            player.drop(stack, false);
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
