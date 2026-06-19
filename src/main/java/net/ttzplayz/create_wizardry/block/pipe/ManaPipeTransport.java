package net.ttzplayz.create_wizardry.block.pipe;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/**
 * Routing helper for mana moved through Create's fluid-pipe network.
 *
 * <p>Arcane pipes (this mod's pipe family) carry mana losslessly; any other fluid pipe
 * (Create's copper pipes) <em>leaks</em>, so mana decays by {@code M·e^(-0.05b)} where
 * {@code b} is the number of copper pipe blocks crossed on the way to a tank. Searches
 * minimize {@code b}, mirroring "the shortest copper run wins".
 *
 * <p>The leak is applied at the receiving tank via {@link DecayingManaTank}: whatever moved
 * the mana (the Mana Siphon's pump, a Create Mechanical Pump, gravity) the loss is identical,
 * since applying the full factor once at the destination equals leaking per block along the run.
 */
public final class ManaPipeTransport {

    private ManaPipeTransport() {}

    /** Safety cap on how many pipe blocks a single search will visit. */
    private static final int MAX_PIPES = 256;
    /** Per-copper-block leak rate from {@code M·e^(-0.05b)}. */
    private static final double LEAK_RATE = 0.05;

    /**
     * Reentrancy depth of "mana is being moved through pipes". The leak applies only while this is
     * non-zero, so a tank filled directly (a bucket, a hopper, an adjacent machine) is never docked —
     * only mana that actually travels the pipe network is. Set around Create's
     * {@code FluidNetwork.tick()} (via {@code FluidNetworkManaLeakMixin}) and around the Mana
     * Siphon's own pump push. Server fluid ticks run on one thread, so a plain counter is enough.
     */
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

    /** Arcane pipes are lossless; everything else (Create copper pipes) leaks. */
    public static boolean isArcanePipe(BlockState state) {
        Block b = state.getBlock();
        return b instanceof ArcanePipeBlock
                || b instanceof SmartArcanePipeBlock
                || b instanceof ArcaneGlassPipeBlock
                || b instanceof EncasedArcanePipeBlock;
    }

    /** Fraction of mana that survives crossing {@code copperBlocks} leaky pipe blocks. */
    public static double decayFactor(int copperBlocks) {
        if (copperBlocks <= 0) return 1.0;
        return Math.exp(-LEAK_RATE * copperBlocks);
    }

    public static boolean isMana(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == CWFluidRegistry.MANA.get();
    }

    /** A reachable mana tank: the block to fill, the side to fill it from, and the copper run to it. */
    public record Destination(BlockPos pos, Direction fillSide, int copperBlocks) {}

    /**
     * Walks the connected fluid-pipe network leaving {@code fromPos} through {@code fromSide} and
     * returns the nearest block that can accept mana, choosing the route that crosses the fewest
     * copper pipe blocks. Returns {@code null} if nothing acceptable is reachable.
     */
    @Nullable
    public static Destination findManaDestination(Level level, BlockPos fromPos, Direction fromSide) {
        Hit hit = search(level, fromPos, fromSide, ManaPipeTransport::acceptsMana);
        return hit == null ? null : new Destination(hit.pos(), hit.side(), hit.copper());
    }

    /**
     * Number of copper (leaky) pipe blocks between {@code tankPos} and the nearest tank/endpoint
     * feeding the copper run, along the shortest copper route. Returns {@code -1} if none is
     * reachable (no leak). A {@code null} {@code fromSide} searches every face and takes the minimum.
     *
     * <p>The terminal is any non-pipe block exposing a fluid handler — matched on <em>presence</em>,
     * not contents — so the upstream source is found even while it is drained/empty with fluid in
     * transit through the network (the case that broke pump-driven tank→tank transfers). A
     * Mechanical Pump is a {@code FluidTransportBehaviour} with no tank capability, so it is walked
     * through (counting as one leaky block) rather than ending the search.
     */
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

    /** Wraps {@code delegate} so mana filled into it leaks by the copper distance back to its source. */
    public static IFluidHandler decaying(BlockEntity be, @Nullable Direction side, IFluidHandler delegate) {
        return new DecayingManaTank(be, side, delegate);
    }

    // --- Shared network search ---------------------------------------------

    private record Hit(BlockPos pos, Direction side, int copper) {}

    @FunctionalInterface
    private interface Terminal {
        boolean test(Level level, BlockPos pos, Direction side);
    }

    /**
     * Dijkstra over pipe positions leaving {@code fromPos} through {@code fromSide}, cost = leaky
     * (copper) pipe blocks crossed (inclusive). Returns the first non-pipe block satisfying
     * {@code terminal} (optimal, since costs pop non-decreasing), or {@code null}.
     */
    @Nullable
    private static Hit search(Level level, BlockPos fromPos, Direction fromSide, Terminal terminal) {
        BlockPos first = fromPos.relative(fromSide);
        if (!level.isLoaded(first)) return null;

        FluidTransportBehaviour firstPipe = FluidPropagator.getPipe(level, first);
        if (firstPipe == null) {
            // Non-pipe block sitting directly against the source/tank.
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

    /**
     * Terminal for the upstream search: a non-pipe block that exposes a fluid handler. Resolves the
     * capability only — no {@code fill}/{@code drain} — so it never re-enters a wrapper's
     * {@link DecayingManaTank#factor()} (which would recurse between adjacent tanks), and it matches
     * the source tank regardless of whether it currently holds mana.
     */
    private static boolean hasFluidHandler(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null;
    }

    // --- Destination-side decay wrapper ------------------------------------

    /**
     * Delegating fluid handler that leaks mana on insertion: a {@code fill} of mana stores only
     * {@code offered·factor} while still reporting the full raw amount consumed from the network,
     * so the leaked remainder vanishes. The decay factor is the copper distance back to the
     * nearest mana source, recomputed at most once per {@link #REFRESH_TICKS}. Non-mana fluids
     * and all read/drain operations pass straight through untouched.
     */
    public static final class DecayingManaTank implements IFluidHandler {

        private static final int REFRESH_TICKS = 20;

        private final BlockEntity be;
        @Nullable private final Direction side;
        private final IFluidHandler delegate;
        private double factor = 1.0;
        /** Game time at which the cached factor expires; 0 forces a compute on first use. */
        private long recomputeAt = 0L;

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
                int b = copperDistanceToSource(level, be.getBlockPos(), side);
                factor = b < 0 ? 1.0 : decayFactor(b);
                recomputeAt = now + REFRESH_TICKS;
            }
            return factor;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // Only mana that actually travelled the pipe network leaks; direct fills (bucket, hopper,
            // an adjacent machine) pass straight through untouched.
            if (!isMana(resource) || !inPipeTransport()) return delegate.fill(resource, action);
            double f = factor();
            if (f >= 1.0) return delegate.fill(resource, action);

            int offered = resource.getAmount();
            int want = (int) Math.floor(offered * f);
            if (want <= 0) return 0;

            int stored = delegate.fill(resource.copyWithAmount(want), action);
            if (stored <= 0) return 0;
            // Report the raw network amount that had to leave to store `stored` (the rest leaked).
            int rawConsumed = stored >= want ? offered : (int) Math.ceil(stored / f);
            return Math.min(offered, rawConsumed);
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
