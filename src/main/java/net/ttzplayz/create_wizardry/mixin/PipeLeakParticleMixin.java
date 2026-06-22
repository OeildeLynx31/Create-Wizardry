package net.ttzplayz.create_wizardry.mixin;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ttzplayz.create_wizardry.Config;
import net.ttzplayz.create_wizardry.block.pipe.ManaPipeTransport;
import net.ttzplayz.create_wizardry.particle.CWParticles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sheds rune particles off ordinary (copper) Create fluid pipes and Mechanical Pumps while they carry
 * mana, to show the node leaking. Arcane pipes/pumps (this mod's lossless family) reuse the same Create
 * BE classes, so we guard by <em>block type</em> ({@link ManaPipeTransport#isArcanePipe}) and never
 * sparkle them.
 *
 * <p>Targets {@code SmartBlockEntity.tick()} (the class that actually declares {@code tick}) and
 * narrows to Create's two copper pipe BEs plus the Pump BE, so the inherited-method injection resolves
 * reliably. The "is mana flowing here" signal is the live {@link PipeConnection#getProvidedFluid()} of
 * the node (for a pump, of the pipes on its facing axis) — which only exists once mana moves through
 * Create's real network (now also true for the Mana Siphon).
 */
@Mixin(SmartBlockEntity.class)
public class PipeLeakParticleMixin {

    private static final int CW_LEAK_PERIOD = 6; // ticks between rune puffs per node
    private static final int CW_LEAK_COUNT = 1;

    @Inject(method = "tick", at = @At("TAIL"))
    private void create_wizardry$leakRunes(CallbackInfo ci) {
        if (!Config.manaLeakingEnabled) return; // leaking disabled: no spill, no sparkle

        Object self = this;
        boolean isPipe = self instanceof FluidPipeBlockEntity || self instanceof StraightPipeBlockEntity;
        boolean isPump = self instanceof PumpBlockEntity;
        if (!isPipe && !isPump) return;

        SmartBlockEntity be = (SmartBlockEntity) self;
        if (!(be.getLevel() instanceof ServerLevel level)) return;

        BlockState state = be.getBlockState();
        if (ManaPipeTransport.isArcanePipe(state)) return; // arcane pipes/pumps don't leak

        BlockPos pos = be.getBlockPos();
        // Throttle, de-phased per position so all nodes don't puff on the same tick.
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), CW_LEAK_PERIOD) != 0) return;

        boolean carryingMana = isPump
                ? create_wizardry$pumpCarriesMana(level, pos, state)
                : create_wizardry$pipeCarriesMana(be);
        if (!carryingMana) return;

        CWParticles.spawnManaRunes(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                CW_LEAK_COUNT, 0.35, 0.02);
    }

    private static boolean create_wizardry$pipeCarriesMana(SmartBlockEntity be) {
        FluidTransportBehaviour pipe = be.getBehaviour(FluidTransportBehaviour.TYPE);
        if (pipe == null || pipe.interfaces == null) return false;
        for (PipeConnection connection : pipe.interfaces.values()) {
            if (ManaPipeTransport.isMana(connection.getProvidedFluid())) return true;
        }
        return false;
    }

    /**
     * A pump moves fluid along its facing axis, so we look at the pipes immediately in front of and
     * behind it and ask whether either is carrying mana on the face it shares with the pump.
     */
    private static boolean create_wizardry$pumpCarriesMana(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(BlockStateProperties.FACING)) return false;
        Direction facing = state.getValue(BlockStateProperties.FACING);
        for (Direction d : new Direction[]{facing, facing.getOpposite()}) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(d));
            if (!(neighbor instanceof SmartBlockEntity sbe)) continue;
            FluidTransportBehaviour pipe = sbe.getBehaviour(FluidTransportBehaviour.TYPE);
            if (pipe == null || pipe.interfaces == null) continue;
            PipeConnection connection = pipe.interfaces.get(d.getOpposite());
            if (connection != null && ManaPipeTransport.isMana(connection.getProvidedFluid())) return true;
        }
        return false;
    }
}
