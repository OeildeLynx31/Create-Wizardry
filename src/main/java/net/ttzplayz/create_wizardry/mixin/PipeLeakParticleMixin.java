package net.ttzplayz.create_wizardry.mixin;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.ttzplayz.create_wizardry.block.pipe.ManaPipeTransport;
import net.ttzplayz.create_wizardry.particle.CWParticles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sheds rune particles off ordinary (copper) Create fluid pipes while they carry mana, to show the
 * pipe leaking. Arcane pipes (this mod's lossless family) reuse the same Create pipe BE classes, so
 * we guard by <em>block type</em> ({@link ManaPipeTransport#isArcanePipe}) and never sparkle them.
 *
 * <p>Targets {@code SmartBlockEntity.tick()} (the class that actually declares {@code tick}) and
 * narrows to Create's two copper pipe BEs, so the inherited-method injection resolves reliably. The
 * "is mana flowing here" signal is the pipe's live {@link PipeConnection#getProvidedFluid()} — which
 * only exists once mana moves through Create's real network (now also true for the Mana Siphon).
 */
@Mixin(SmartBlockEntity.class)
public class PipeLeakParticleMixin {

    private static final int CW_LEAK_PERIOD = 6; // ticks between rune puffs per pipe
    private static final int CW_LEAK_COUNT = 1;

    @Inject(method = "tick", at = @At("TAIL"))
    private void create_wizardry$leakRunes(CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof FluidPipeBlockEntity) && !(self instanceof StraightPipeBlockEntity)) return;

        SmartBlockEntity be = (SmartBlockEntity) self;
        if (!(be.getLevel() instanceof ServerLevel level)) return;

        BlockState state = be.getBlockState();
        if (ManaPipeTransport.isArcanePipe(state)) return; // arcane pipes don't leak

        BlockPos pos = be.getBlockPos();
        // Throttle, de-phased per position so all pipes don't puff on the same tick.
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), CW_LEAK_PERIOD) != 0) return;

        FluidTransportBehaviour pipe = be.getBehaviour(FluidTransportBehaviour.TYPE);
        if (pipe == null || pipe.interfaces == null) return;

        boolean carryingMana = false;
        for (PipeConnection connection : pipe.interfaces.values()) {
            if (ManaPipeTransport.isMana(connection.getProvidedFluid())) {
                carryingMana = true;
                break;
            }
        }
        if (!carryingMana) return;

        CWParticles.spawnManaRunes(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                CW_LEAK_COUNT, 0.35, 0.02);
    }
}
