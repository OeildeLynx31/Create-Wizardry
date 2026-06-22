package net.ttzplayz.create_wizardry.gametest;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ttzplayz.create_wizardry.CreateWizardry;
import net.ttzplayz.create_wizardry.block.CWBlocks;
import net.ttzplayz.create_wizardry.block.mana_siphon.ManaSiphonBlockEntity;
import net.ttzplayz.create_wizardry.block.pipe.ManaPipeTransport;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;

// in-world checks for the copper-pipe mana leak M*e^(-0.05b); piped fills bracketed, direct fills not
@GameTestHolder(CreateWizardry.MOD_ID)
@PrefixGameTestTemplate(false)
public class ManaLeakGameTests {

    private static final int PIPES = 6;
    private static final int OFFERED = 1000;

    @GameTest(template = "manaleak", timeoutTicks = 120)
    public static void copperPipesLeak(GameTestHelper helper) {
        // 6 copper pipes -> ~741 delivered
        runLeakTest(helper, AllBlocks.FLUID_PIPE.getDefaultState(), PIPES, Math.exp(-0.05 * PIPES));
    }

    @GameTest(template = "manaleak", timeoutTicks = 120)
    public static void arcanePipesLossless(GameTestHelper helper) {
        // arcane never counts, full 1000 delivered
        runLeakTest(helper, CWBlocks.ARCANE_PIPE.get().defaultBlockState(), PIPES, 1.0);
    }

    // regression: chunked delivery must still track 1000*e^(-0.05b), not lose a fraction per chunk
    @GameTest(template = "manaleak", timeoutTicks = 120)
    public static void copperLeakFollowsCurveInChunks(GameTestHelper helper) {
        BlockPos src = new BlockPos(1, 1, 1);
        for (int i = 0; i < PIPES; i++) {
            helper.setBlock(new BlockPos(2 + i, 1, 1), AllBlocks.FLUID_PIPE.getDefaultState());
        }
        BlockPos dst = new BlockPos(2 + PIPES, 1, 1);
        helper.setBlock(src, AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(dst, AllBlocks.FLUID_TANK.getDefaultState());

        helper.runAfterDelay(10, () -> {
            IFluidHandler dstCap = capabilityAt(helper, dst);
            helper.assertTrue(dstCap != null, "destination tank capability present");

            int stored = deliverThroughPipesChunked(dstCap, OFFERED, 8);
            int expected = (int) Math.floor(OFFERED * Math.exp(-0.05 * PIPES));
            helper.assertTrue(Math.abs(stored - expected) <= 2,
                    "chunked delivery stored " + stored + " mB, expected ~" + expected
                            + " mB, per-chunk flooring must not bias the leak");
            helper.succeed();
        });
    }

    // regression: tank-to-tank pump leak found by source presence not contents; empty source still leaks
    @GameTest(template = "manaleak", timeoutTicks = 120)
    public static void copperLeaksWithEmptySource(GameTestHelper helper) {
        BlockPos src = new BlockPos(1, 1, 1);
        for (int i = 0; i < PIPES; i++) {
            helper.setBlock(new BlockPos(2 + i, 1, 1), AllBlocks.FLUID_PIPE.getDefaultState());
        }
        BlockPos dst = new BlockPos(2 + PIPES, 1, 1);
        helper.setBlock(src, AllBlocks.FLUID_TANK.getDefaultState()); // present but never filled
        helper.setBlock(dst, AllBlocks.FLUID_TANK.getDefaultState());

        helper.runAfterDelay(10, () -> {
            IFluidHandler dstCap = capabilityAt(helper, dst);
            helper.assertTrue(dstCap != null, "destination tank capability present");

            int stored = deliverThroughPipes(dstCap, OFFERED);
            int expected = (int) Math.floor(OFFERED * Math.exp(-0.05 * PIPES));
            helper.assertTrue(Math.abs(stored - expected) <= 2,
                    "delivered " + stored + " mB, expected ~" + expected + " mB, an empty source must still leak");
            helper.succeed();
        });
    }

    // regression: footprint search finds the copper run into a multiblock tank's non-controller member
    @GameTest(template = "manaleak", timeoutTicks = 120)
    public static void copperLeaksIntoMultiblockTank(GameTestHelper helper) {
        // 6 copper pipes feeding the top block of a 1x1x2 tank
        BlockPos src = new BlockPos(1, 2, 1);
        for (int i = 0; i < PIPES; i++) {
            helper.setBlock(new BlockPos(2 + i, 2, 1), AllBlocks.FLUID_PIPE.getDefaultState());
        }
        BlockPos dstBottom = new BlockPos(2 + PIPES, 1, 1); // controller
        BlockPos dstTop = new BlockPos(2 + PIPES, 2, 1);    // piped-into member
        helper.setBlock(src, AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(dstBottom, AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(dstTop, AllBlocks.FLUID_TANK.getDefaultState());

        helper.runAfterDelay(20, () -> {
            // confirm the tanks merged into a 2-tall multiblock
            var topBE = (com.simibubi.create.content.fluids.tank.FluidTankBlockEntity) helper.getBlockEntity(dstTop);
            helper.assertTrue(topBE != null && !topBE.isController(), "top tank is a non-controller member");
            var ctrl = (com.simibubi.create.content.fluids.tank.FluidTankBlockEntity) topBE.getControllerBE();
            helper.assertTrue(ctrl != null && ctrl.getHeight() == 2, "destination formed a 2-tall tank");

            IFluidHandler dstCap = capabilityAt(helper, dstBottom);
            helper.assertTrue(dstCap != null, "destination tank capability present");

            int stored = deliverThroughPipes(dstCap, OFFERED);
            int expected = (int) Math.floor(OFFERED * Math.exp(-0.05 * PIPES));
            helper.assertTrue(Math.abs(stored - expected) <= 2,
                    "multiblock tank stored " + stored + " mB, expected ~" + expected
                            + " mB, the copper run reaches it through a non-controller block");
            helper.succeed();
        });
    }

    // regression: direct bucket fill must not leak even over a copper network
    @GameTest(template = "manaleak", timeoutTicks = 120)
    public static void directFillDoesNotLeak(GameTestHelper helper) {
        BlockPos src = new BlockPos(1, 1, 1);
        for (int i = 0; i < PIPES; i++) {
            helper.setBlock(new BlockPos(2 + i, 1, 1), AllBlocks.FLUID_PIPE.getDefaultState());
        }
        BlockPos dst = new BlockPos(2 + PIPES, 1, 1);
        helper.setBlock(src, AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(dst, AllBlocks.FLUID_TANK.getDefaultState());

        helper.runAfterDelay(10, () -> {
            IFluidHandler dstCap = capabilityAt(helper, dst);
            helper.assertTrue(dstCap != null, "destination tank capability present");

            // bucket-style, not pipe transport
            dstCap.fill(new FluidStack(CWFluidRegistry.MANA.get(), OFFERED), IFluidHandler.FluidAction.EXECUTE);
            int stored = dstCap.getFluidInTank(0).getAmount();
            helper.assertTrue(stored == OFFERED,
                    "direct (bucket) fill stored " + stored + " mB, expected the full " + OFFERED + ", must not leak");
            helper.succeed();
        });
    }

    private static void runLeakTest(GameTestHelper helper, BlockState pipe, int count, double expectedFactor) {
        BlockPos src = new BlockPos(1, 1, 1);
        for (int i = 0; i < count; i++) {
            helper.setBlock(new BlockPos(2 + i, 1, 1), pipe);
        }
        BlockPos dst = new BlockPos(2 + count, 1, 1);
        helper.setBlock(src, AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(dst, AllBlocks.FLUID_TANK.getDefaultState());

        // let connections settle
        helper.runAfterDelay(10, () -> {
            IFluidHandler srcCap = capabilityAt(helper, src);
            IFluidHandler dstCap = capabilityAt(helper, dst);
            helper.assertTrue(srcCap != null && dstCap != null, "both tank capabilities present");

            // seed source directly, no leak
            int seeded = srcCap.fill(new FluidStack(CWFluidRegistry.MANA.get(), 8000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(seeded > 0, "source tank seeded with mana (filled " + seeded + ")");

            int stored = deliverThroughPipes(dstCap, OFFERED);
            int expected = (int) Math.floor(OFFERED * expectedFactor);
            helper.assertTrue(Math.abs(stored - expected) <= 2,
                    "delivered " + stored + " mB, expected ~" + expected + " mB (factor " + expectedFactor + ")");
            helper.succeed();
        });
    }

    // siphon spun by a motor must pump its mana down through copper into the tank below
    @GameTest(template = "manaleak", timeoutTicks = 160)
    public static void siphonPumpsDownThroughNetwork(GameTestHelper helper) {
        int z = 3;
        BlockPos tank = new BlockPos(8, 1, z);
        BlockPos pipeA = new BlockPos(8, 2, z);
        BlockPos pipeB = new BlockPos(8, 3, z); // 2 copper pipes -> b = 2
        BlockPos siphon = new BlockPos(8, 4, z);
        BlockPos cog = new BlockPos(9, 4, z);
        BlockPos motor = new BlockPos(9, 5, z);

        helper.setBlock(tank, AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(pipeA, AllBlocks.FLUID_PIPE.getDefaultState());
        helper.setBlock(pipeB, AllBlocks.FLUID_PIPE.getDefaultState());
        helper.setBlock(siphon, CWBlocks.MANA_SIPHON.get().defaultBlockState());
        helper.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));   // meshes with the Y-axis Siphon
        helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(DirectionalKineticBlock.FACING, Direction.DOWN));      // drives the cogwheel below

        helper.runAfterDelay(3, () -> {
            ManaSiphonBlockEntity be = (ManaSiphonBlockEntity) helper.getBlockEntity(siphon);
            helper.assertTrue(be != null && be.internalTank != null, "siphon present");
            be.internalTank.getPrimaryHandler()
                    .fill(new FluidStack(CWFluidRegistry.MANA.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
        });

        helper.runAfterDelay(140, () -> {
            ManaSiphonBlockEntity be = (ManaSiphonBlockEntity) helper.getBlockEntity(siphon);
            helper.assertTrue(Math.abs(be.getSpeed()) > 0,
                    "creative motor spins the siphon (speed=" + be.getSpeed() + ")");

            int delivered = capabilityAt(helper, tank).getFluidInTank(0).getAmount();
            int siphonLeft = be.internalTank.getPrimaryHandler().getFluidInTank(0).getAmount();
            helper.assertTrue(delivered > 0,
                    "siphon pumped mana DOWN into the tank through the network (got " + delivered + " mB)");
            helper.assertTrue(siphonLeft < 1000,
                    "siphon tank drained while pumping (left " + siphonLeft + " mB)");
            helper.succeed();
        });
    }

    private static IFluidHandler capabilityAt(GameTestHelper helper, BlockPos relative) {
        return ((ServerLevel) helper.getLevel())
                .getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(relative), null);
    }

    // fill handler with mana under pipe transport, return stored
    private static int deliverThroughPipes(IFluidHandler handler, int amount) {
        ManaPipeTransport.enterPipeTransport();
        try {
            handler.fill(new FluidStack(CWFluidRegistry.MANA.get(), amount), IFluidHandler.FluidAction.EXECUTE);
        } finally {
            ManaPipeTransport.exitPipeTransport();
        }
        return handler.getFluidInTank(0).getAmount();
    }

    // deliver amount in chunk-sized fills mirroring create's chunked transfer, return total stored
    private static int deliverThroughPipesChunked(IFluidHandler handler, int amount, int chunk) {
        ManaPipeTransport.enterPipeTransport();
        try {
            for (int sent = 0; sent < amount; sent += chunk) {
                int size = Math.min(chunk, amount - sent);
                FluidStack mana = new FluidStack(CWFluidRegistry.MANA.get(), size);
                handler.fill(mana, IFluidHandler.FluidAction.SIMULATE);
                handler.fill(mana, IFluidHandler.FluidAction.EXECUTE);
            }
        } finally {
            ManaPipeTransport.exitPipeTransport();
        }
        return handler.getFluidInTank(0).getAmount();
    }
}
