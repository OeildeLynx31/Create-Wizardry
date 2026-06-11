package net.ttzplayz.create_wizardry.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ttzplayz.create_wizardry.CreateWizardry;
import net.ttzplayz.create_wizardry.block.blaze_caster.BlazeCasterBlock;
import net.ttzplayz.create_wizardry.block.channeler.ChannelerBlock;
import net.ttzplayz.create_wizardry.block.pipe.ArcaneGlassPipeBlock;
import net.ttzplayz.create_wizardry.block.pipe.ArcanePipeBlock;
import net.ttzplayz.create_wizardry.block.pipe.EncasedArcanePipeBlock;
import net.ttzplayz.create_wizardry.block.pipe.SmartArcanePipeBlock;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;
import net.ttzplayz.create_wizardry.item.CWItems;

import java.util.function.Supplier;

public class CWBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateWizardry.MOD_ID);

public static final DeferredBlock<ChannelerBlock> CHANNELER =
        registerBlock("channeler", () -> new ChannelerBlock(
                Block.Properties.of()
                        .mapColor(MapColor.COLOR_ORANGE)
                        .strength(3.5F)
                        .sound(SoundType.METAL)
                        .lightLevel(powered -> 4)
                        .noOcclusion()
        ));

    public static final DeferredBlock<ArcaneCasingBlock> ARCANE_CASING =
            registerBlock("arcane_casing", () -> new ArcaneCasingBlock(
                    Block.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(1.5F)
                            .sound(SoundType.DEEPSLATE)
            ));

    public static final DeferredBlock<BlazeCasterBlock> BLAZE_CASTER =
            registerBlock("blaze_caster", () -> new BlazeCasterBlock(
                    Block.Properties.of()
                            .mapColor(MapColor.COLOR_BLUE)
                            .strength(3.5F)
                            .sound(SoundType.METAL)
                            .lightLevel(BlazeCasterBlock::getLight)
                            .noOcclusion()
            ));

    // ---- Arcane pipes (mirror Create's fluid pipe family) ----
    public static final DeferredBlock<ArcanePipeBlock> ARCANE_PIPE =
            registerBlock("arcane_pipe", () -> new ArcanePipeBlock(pipeProperties()));

    public static final DeferredBlock<SmartArcanePipeBlock> SMART_ARCANE_PIPE =
            registerBlock("smart_arcane_pipe", () -> new SmartArcanePipeBlock(
                    pipeProperties().mapColor(MapColor.TERRACOTTA_YELLOW)));

    // Glass + encased variants have no item of their own (obtained via wrench / encasing), mirroring Create.
    public static final DeferredBlock<ArcaneGlassPipeBlock> GLASS_ARCANE_PIPE =
            registerBlockNoItem("glass_arcane_pipe", () -> new ArcaneGlassPipeBlock(
                    pipeProperties().noOcclusion()));

    public static final DeferredBlock<EncasedArcanePipeBlock> ENCASED_ARCANE_PIPE =
            registerBlockNoItem("encased_arcane_pipe", () -> new EncasedArcanePipeBlock(
                    pipeProperties().noOcclusion().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)));

    private static BlockBehaviour.Properties pipeProperties() {
        return Block.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(0.5F)
                .sound(SoundType.COPPER)
                .requiresCorrectToolForDrops()
                .forceSolidOff();
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> DeferredBlock<T> registerBlockNoItem(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }
    

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        CWItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
