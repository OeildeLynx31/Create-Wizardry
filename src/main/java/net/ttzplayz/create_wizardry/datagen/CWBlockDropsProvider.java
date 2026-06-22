package net.ttzplayz.create_wizardry.datagen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.ttzplayz.create_wizardry.block.CWBlocks;

import java.util.Set;

public class CWBlockDropsProvider extends BlockLootSubProvider {
    protected CWBlockDropsProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(CWBlocks.CHANNELER.get());
        dropSelf(CWBlocks.BLAZE_CASTER.get());
        dropSelf(CWBlocks.MANA_SIPHON.get());
        // cluster drops handled in code; no-op table satisfies datagen validation
        add(CWBlocks.ARCANE_ESSENCE_CLUSTER.get(), noDrop());
        dropSelf(CWBlocks.ARCANE_BLOCK.get());
        dropSelf(CWBlocks.ARCANE_CASING.get());
        dropSelf(CWBlocks.ARCANE_PIPE.get());
        dropSelf(CWBlocks.SMART_ARCANE_PIPE.get());
        dropSelf(CWBlocks.ARCANE_PUMP.get());
        // glass + encased drop the regular arcane pipe
        dropOther(CWBlocks.GLASS_ARCANE_PIPE.get(), CWBlocks.ARCANE_PIPE.get());
        dropOther(CWBlocks.ENCASED_ARCANE_PIPE.get(), CWBlocks.ARCANE_PIPE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return CWBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
