package net.ttzplayz.create_wizardry.block.pipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlockEntity;

import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import org.jetbrains.annotations.NotNull;

/**
 * Arcane counterpart of Create's {@link SmartFluidPipeBlock}. Identical behaviour, own block entity type.
 */
public class SmartArcanePipeBlock extends SmartFluidPipeBlock {

    public static final MapCodec<SmartArcanePipeBlock> CODEC = simpleCodec(SmartArcanePipeBlock::new);

    public SmartArcanePipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<SmartFluidPipeBlockEntity> getBlockEntityClass() {
        return SmartFluidPipeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmartFluidPipeBlockEntity> getBlockEntityType() {
        return CWBlockEntities.SMART_ARCANE_PIPE.get();
    }

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
