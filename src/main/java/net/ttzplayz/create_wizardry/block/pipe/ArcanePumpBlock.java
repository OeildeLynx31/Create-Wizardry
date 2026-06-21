package net.ttzplayz.create_wizardry.block.pipe;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import org.jetbrains.annotations.NotNull;

/**
 * Arcane counterpart of Create's {@link PumpBlock}. Behaves identically (a kinetic fluid pump), but
 * uses Create: Wizardry's own block entity type so it can be tagged {@code MANA_INSULATED} and move
 * mana losslessly — see {@link ManaPipeTransport#isArcanePipe}.
 */
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
