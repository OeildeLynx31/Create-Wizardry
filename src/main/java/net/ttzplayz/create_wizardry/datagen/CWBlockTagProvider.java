package net.ttzplayz.create_wizardry.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.ttzplayz.create_wizardry.CreateWizardry;
import net.ttzplayz.create_wizardry.block.CWBlocks;
import net.ttzplayz.create_wizardry.spell.CWTags;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS;

public class CWBlockTagProvider extends BlockTagsProvider {
    public CWBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, CreateWizardry.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                CWBlocks.ARCANE_BLOCK.get(),
                CWBlocks.ARCANE_PIPE.get(),
                CWBlocks.SMART_ARCANE_PIPE.get(),
                CWBlocks.GLASS_ARCANE_PIPE.get(),
                CWBlocks.ENCASED_ARCANE_PIPE.get(),
                CWBlocks.MANA_SIPHON.get(),
                CWBlocks.ARCANE_ESSENCE_CLUSTER.get());
        tag(STORAGE_BLOCKS).add(CWBlocks.ARCANE_BLOCK.get());

        // Gem/crystal blocks the Mana Siphon can crystallize Arcane Essence onto.
        tag(CWTags.Blocks.CRYSTALLINE).add(
                Blocks.DIAMOND_BLOCK,
                Blocks.EMERALD_BLOCK,
                Blocks.LAPIS_BLOCK,
                Blocks.AMETHYST_BLOCK,
                Blocks.BUDDING_AMETHYST);
    }
}
