package net.ttzplayz.create_wizardry.block.pipe;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

// routes mana through create's pipes; copper leaks M*e^(-0.05b), arcane is lossless
public final class ManaPipeTransport {

    private ManaPipeTransport() {}

    // max pipes per search
    private static final int MAX_PIPES = 256;

    // sub-mB remainder carried per tank so chunked delivery totals M*e^(-0.05b) exactly
    private static final Map<GlobalPos, Double> LEAK_CARRY = new HashMap<>();

    // nonzero while mana is moving through pipes; leak applies only then
    private static int pipeTransportDepth = 0;

    public static void enterPipeTransport() {
        pipeTransportDepth++;
    }

    public static void exitPipeTransport() {
        if (pipeTransportDepth > 0) pipeTransportDepth--;
    }

    public static boolean inPipeTransport() {
        return pipeTransportDepth > 0;
    }

    // mana-insulated nodes (arcane family) are lossless; tag-driven
    public static boolean isArcanePipe(BlockState state) {
        return state.is(net.ttzplayz.create_wizardry.util.CWTags.Blocks.MANA_INSULATED);
    }

    // fraction surviving N copper blocks
    public static double decayFactor(int copperBlocks) {
        if (copperBlocks <= 0 || !net.ttzplayz.create_wizardry.Config.manaLeakingEnabled) return 1.0;
        return Math.exp(-net.ttzplayz.create_wizardry.Config.manaPipeLossRate * copperBlocks);
    }

    public static boolean isMana(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == CWFluidRegistry.MANA.get();
    }

    // reachable tank: pos, fill side, copper distance
    public record Destination(BlockPos pos, Direction fillSide, int copperBlocks) {}

    // nearest mana-accepting block, fewest copper blocks; null if none
    @Nullable
    public static Destination findManaDestination(Level level, BlockPos fromPos, Direction fromSide) {
        Hit hit = search(level, fromPos, fromSide, ManaPipeTransport::acceptsMana);
        return hit == null ? null : new Destination(hit.pos(), hit.side(), hit.copper());
    }

    // shortest copper distance to an upstream source; -1 if none, null side searches all faces
    public static int copperDistanceToSource(Level level, BlockPos tankPos, @Nullable Direction fromSide) {
        if (fromSide != null) {
            Hit hit = search(level, tankPos, fromSide, ManaPipeTransport::hasFluidHandler);
            return hit == null ? -1 : hit.copper();
        }
        int best = -1;
        for (Direction d : Iterate.directions) {
            Hit hit = search(level, tankPos, d, ManaPipeTransport::hasFluidHandler);
            if (hit != null && (best < 0 || hit.copper() < best)) best = hit.copper();
        }
        return best;
    }

    // multiblock-tank variant: shortest copper run from any footprint face, excluding own members
    public static int copperDistanceFromFootprint(Level level, Set<BlockPos> own) {
        Terminal external = (lvl, pos, side) -> !own.contains(pos) && hasFluidHandler(lvl, pos, side);
        int best = -1;
        for (BlockPos p : own) {
            for (Direction d : Iterate.directions) {
                if (own.contains(p.relative(d))) continue; // face into another member: not an exit
                Hit hit = search(level, p, d, external);
                if (hit != null && (best < 0 || hit.copper() < best)) best = hit.copper();
            }
        }
        return best;
    }

    // wraps delegate so filled mana leaks by copper distance to source
    public static IFluidHandler decaying(BlockEntity be, @Nullable Direction side, IFluidHandler delegate) {
        return new DecayingManaTank(be, side, delegate);
    }

    // --- Shared network search ---------------------------------------------

    private record Hit(BlockPos pos, Direction side, int copper) {}

    @FunctionalInterface
    private interface Terminal {
        boolean test(Level level, BlockPos pos, Direction side);
    }

    // dijkstra over pipes, cost = copper blocks crossed; first terminal hit or null
    @Nullable
    private static Hit search(Level level, BlockPos fromPos, Direction fromSide, Terminal terminal) {
        BlockPos first = fromPos.relative(fromSide);
        if (!level.isLoaded(first)) return null;

        FluidTransportBehaviour firstPipe = FluidPropagator.getPipe(level, first);
        if (firstPipe == null) {
            // non-pipe block against source/tank
            return terminal.test(level, first, fromSide.getOpposite())
                    ? new Hit(first, fromSide.getOpposite(), 0)
                    : null;
        }
        if (!firstPipe.canHaveFlowToward(level.getBlockState(first), fromSide.getOpposite()))
            return null;

        Map<BlockPos, Integer> dist = new HashMap<>();
        PriorityQueue<BlockPos> queue =
                new PriorityQueue<>(Comparator.comparingInt(p -> dist.getOrDefault(p, Integer.MAX_VALUE)));
        Set<BlockPos> settled = new HashSet<>();

        dist.put(first, isArcanePipe(level.getBlockState(first)) ? 0 : 1);
        queue.add(first);

        while (!queue.isEmpty() && settled.size() < MAX_PIPES) {
            BlockPos p = queue.poll();
            if (!settled.add(p)) continue;
            int pCost = dist.get(p);

            BlockState pState = level.getBlockState(p);
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, p);
            if (pipe == null) continue;

            for (Direction d : Iterate.directions) {
                if (!pipe.canHaveFlowToward(pState, d)) continue;
                BlockPos n = p.relative(d);
                if (n.equals(fromPos) || !level.isLoaded(n)) continue;

                FluidTransportBehaviour nPipe = FluidPropagator.getPipe(level, n);
                if (nPipe != null) {
                    if (!nPipe.canHaveFlowToward(level.getBlockState(n), d.getOpposite())) continue;
                    int nCost = pCost + (isArcanePipe(level.getBlockState(n)) ? 0 : 1);
                    if (nCost < dist.getOrDefault(n, Integer.MAX_VALUE)) {
                        dist.put(n, nCost);
                        queue.add(n);
                    }
                } else if (terminal.test(level, n, d.getOpposite())) {
                    return new Hit(n, d.getOpposite(), pCost);
                }
            }
        }
        return null;
    }

    private static boolean acceptsMana(Level level, BlockPos pos, Direction side) {
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        if (handler == null) return false;
        return handler.fill(new FluidStack(CWFluidRegistry.MANA.get(), Integer.MAX_VALUE),
                IFluidHandler.FluidAction.SIMULATE) > 0;
    }

    // upstream terminal: non-pipe block with a fluid handler; resolves capability only, no fill/drain
    private static boolean hasFluidHandler(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null;
    }

    // --- Destination-side decay wrapper ------------------------------------

    // delegating handler that leaks mana on fill by the cached copper-distance factor
    public static final class DecayingManaTank implements IFluidHandler {

        private static final int REFRESH_TICKS = 20;

        private final BlockEntity be;
        @Nullable private final Direction side;
        private final IFluidHandler delegate;
        private double factor = 1.0;
        // game time the cached factor expires
        private long recomputeAt = 0L;
        // throttle for the devastation advancement scan
        private long nextLeakAdvancementCheck = 0L;

        DecayingManaTank(BlockEntity be, @Nullable Direction side, IFluidHandler delegate) {
            this.be = be;
            this.side = side;
            this.delegate = delegate;
        }

        private double factor() {
            Level level = be.getLevel();
            if (level == null) return 1.0;
            long now = level.getGameTime();
            if (now >= recomputeAt) {
                int b = copperDistance(level);
                factor = b < 0 ? 1.0 : decayFactor(b);
                recomputeAt = now + REFRESH_TICKS;
            }
            return factor;
        }

        // copper distance to source, footprint-aware for multiblock tanks
        private int copperDistance(Level level) {
            Set<BlockPos> own = multiblockFootprint();
            return own != null
                    ? copperDistanceFromFootprint(level, own)
                    : copperDistanceToSource(level, be.getBlockPos(), side);
        }

        // member positions of a multiblock create tank, or null for single-block
        @Nullable
        private Set<BlockPos> multiblockFootprint() {
            if (!(be instanceof com.simibubi.create.content.fluids.tank.FluidTankBlockEntity tank)) return null;
            int w = tank.getWidth();
            int h = tank.getHeight();
            if (w <= 1 && h <= 1) return null;
            BlockPos c = be.getBlockPos();
            Set<BlockPos> own = new HashSet<>(w * w * h);
            for (int dx = 0; dx < w; dx++)
                for (int dy = 0; dy < h; dy++)
                    for (int dz = 0; dz < w; dz++)
                        own.add(c.offset(dx, dy, dz));
            return own;
        }

        // award devastation to a nearby player on a long (>20 block) copper leak
        private void awardDevastationIfClose() {
            Level level = be.getLevel();
            if (level == null) return;
            long now = level.getGameTime();
            if (now < nextLeakAdvancementCheck) return;
            nextLeakAdvancementCheck = now + REFRESH_TICKS;
            int b = copperDistance(level);
            if (b <= 20) return;
            BlockPos p = be.getBlockPos();
            net.minecraft.world.entity.player.Player nearest =
                    level.getNearestPlayer(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 16.0, false);
            if (nearest instanceof net.minecraft.server.level.ServerPlayer sp && !sp.isFakePlayer())
                net.ttzplayz.create_wizardry.advancement.CWAdvancements.A_DEVASTATING_LOSS.awardTo(sp);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // only piped mana leaks; direct fills pass through
            if (!isMana(resource) || !inPipeTransport()) return delegate.fill(resource, action);
            Level level = be.getLevel();
            double f = level == null ? 1.0 : factor();
            if (f >= 1.0) return delegate.fill(resource, action);

            // leaking through copper, award nearby player
            if (action.execute()) awardDevastationIfClose();

            int offered = resource.getAmount();
            if (offered <= 0) return 0;

            // carry sub-mB remainder so chunked transfers total offered*f; advance on execute only
            GlobalPos key = GlobalPos.of(level.dimension(), be.getBlockPos());
            double carry = LEAK_CARRY.getOrDefault(key, 0.0);
            double exact = offered * f + carry;
            int want = (int) Math.floor(exact);

            if (want <= 0) {
                // nothing lands this chunk; bank the fraction if the tank has room
                if (delegate.fill(resource.copyWithAmount(1), FluidAction.SIMULATE) <= 0) return 0;
                if (action.execute()) LEAK_CARRY.put(key, exact);
                return offered;
            }

            int stored = delegate.fill(resource.copyWithAmount(want), action);
            if (stored <= 0) return 0;

            if (stored >= want) {
                // all of want fit, rest leaked; bank remainder
                if (action.execute()) {
                    double remainder = exact - want;
                    if (remainder < 1.0e-6) LEAK_CARRY.remove(key); else LEAK_CARRY.put(key, remainder);
                }
                return offered;
            }
            // tank filled mid-chunk; spend carry
            if (action.execute()) LEAK_CARRY.remove(key);
            return Math.min(offered, (int) Math.ceil(stored / f));
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return delegate.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return delegate.drain(maxDrain, action);
        }

        @Override
        public int getTanks() {
            return delegate.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return delegate.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return delegate.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return delegate.isFluidValid(tank, stack);
        }
    }
}
