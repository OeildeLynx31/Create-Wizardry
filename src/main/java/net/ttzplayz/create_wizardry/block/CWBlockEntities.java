package net.ttzplayz.create_wizardry.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ttzplayz.create_wizardry.CreateWizardry;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import net.ttzplayz.create_wizardry.block.blaze_caster.BlazeCasterBlockEntity;
import net.ttzplayz.create_wizardry.block.channeler.ChannelerBlockEntity;



public class CWBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CreateWizardry.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChannelerBlockEntity>> CHANNELER_BE =
            BLOCK_ENTITIES.register(
                    "channeler_be",
                    () -> BlockEntityType.Builder.of(
                    ChannelerBlockEntity::new,
                            CWBlocks.CHANNELER.get())
                            .build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlazeCasterBlockEntity>> BLAZE_CASTER_BE =
            BLOCK_ENTITIES.register(
                    "blaze_caster_be",
                    () -> BlockEntityType.Builder.of(BlazeCasterBlockEntity::new, CWBlocks.BLAZE_CASTER.get()).build(null)
            );

    // Arcane pipe family reuses Create's block entity implementations with our own block-bound types.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> ARCANE_PIPE =
            BLOCK_ENTITIES.register("arcane_pipe", () -> BlockEntityType.Builder.<FluidPipeBlockEntity>of(
                    (pos, state) -> new FluidPipeBlockEntity(CWBlockEntities.ARCANE_PIPE.get(), pos, state),
                    CWBlocks.ARCANE_PIPE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StraightPipeBlockEntity>> GLASS_ARCANE_PIPE =
            BLOCK_ENTITIES.register("glass_arcane_pipe", () -> BlockEntityType.Builder.<StraightPipeBlockEntity>of(
                    (pos, state) -> new StraightPipeBlockEntity(CWBlockEntities.GLASS_ARCANE_PIPE.get(), pos, state),
                    CWBlocks.GLASS_ARCANE_PIPE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> ENCASED_ARCANE_PIPE =
            BLOCK_ENTITIES.register("encased_arcane_pipe", () -> BlockEntityType.Builder.<FluidPipeBlockEntity>of(
                    (pos, state) -> new FluidPipeBlockEntity(CWBlockEntities.ENCASED_ARCANE_PIPE.get(), pos, state),
                    CWBlocks.ENCASED_ARCANE_PIPE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmartFluidPipeBlockEntity>> SMART_ARCANE_PIPE =
            BLOCK_ENTITIES.register("smart_arcane_pipe", () -> BlockEntityType.Builder.<SmartFluidPipeBlockEntity>of(
                    (pos, state) -> new SmartFluidPipeBlockEntity(CWBlockEntities.SMART_ARCANE_PIPE.get(), pos, state),
                    CWBlocks.SMART_ARCANE_PIPE.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}