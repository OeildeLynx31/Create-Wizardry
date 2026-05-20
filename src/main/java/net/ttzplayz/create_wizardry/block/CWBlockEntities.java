package net.ttzplayz.create_wizardry.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ttzplayz.create_wizardry.CreateWizardry;
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


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}