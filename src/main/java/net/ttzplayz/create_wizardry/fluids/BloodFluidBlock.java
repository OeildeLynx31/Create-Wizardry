package net.ttzplayz.create_wizardry.fluids;

import io.redspace.ironsspellbooks.registries.FluidRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ttzplayz.create_wizardry.item.CWItems;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A static, non-flowing fluid block for Iron's Spells 'n Spellbooks' {@code irons_spellbooks:blood}.
 * <p>
 * Blood is registered by ISS as a {@code NoopFluid} with no flowing states / no LEVEL property, so a
 * vanilla {@link net.minecraft.world.level.block.LiquidBlock} cannot wrap it (its constructor calls
 * {@code fluid.getFlowing(1..8)} and crashes). This block sidesteps that: it simply reports the blood
 * source {@link FluidState} so the fluid renderer draws it, behaves as a passable invisible block, and
 * can be scooped back into a blood bucket. It does not spread.
 */
public class BloodFluidBlock extends Block implements BucketPickup {

    public BloodFluidBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return FluidRegistry.BLOOD.get().defaultFluidState();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        return new ItemStack(CWItems.BLOOD_BUCKET.get());
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }
}
