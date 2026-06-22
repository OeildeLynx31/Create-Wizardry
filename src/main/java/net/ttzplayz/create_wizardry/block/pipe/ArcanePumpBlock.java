package net.ttzplayz.create_wizardry.block.pipe;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import org.jetbrains.annotations.NotNull;

// create's pump with our own be type so it can be mana-insulated
public class ArcanePumpBlock extends PumpBlock {

    public ArcanePumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Class<PumpBlockEntity> getBlockEntityClass() {
        return PumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
        return CWBlockEntities.ARCANE_PUMP.get();
    }
}
