package net.ttzplayz.create_wizardry.block.blaze_caster;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ttzplayz.create_wizardry.client.CWPartialModels;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class SpellcastingBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    protected ItemStack heldItem = ItemStack.EMPTY;
    protected boolean canUseSpellbooks;
    public SmartFluidTankBehaviour internalTank;

    public SpellcastingBlockEntity(BlockPos pos, BlockState state) {
        super(null, pos, state);
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

    public @Nullable IFluidHandler getFluidHandler(@Nullable Direction side) {
        if ((side == Direction.DOWN || side == null) && !isRemoved())
            return internalTank.getCapability();
        return null;
    }

}
