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
        // Arcane Essence cluster drops are handled in code (ArcaneEssenceClusterBlock#getDrops, age-scaled);
        // this no-op loot table just satisfies datagen's known-block validation.
        add(CWBlocks.ARCANE_ESSENCE_CLUSTER.get(), noDrop());
        dropSelf(CWBlocks.ARCANE_BLOCK.get());
        dropSelf(CWBlocks.ARCANE_CASING.get());
        dropSelf(CWBlocks.ARCANE_PIPE.get());
        dropSelf(CWBlocks.SMART_ARCANE_PIPE.get());
        dropSelf(CWBlocks.ARCANE_PUMP.get());
        // Glass + encased variants drop the regular arcane pipe (mirrors Create's fluid pipe family).
        dropOther(CWBlocks.GLASS_ARCANE_PIPE.get(), CWBlocks.ARCANE_PIPE.get());
        dropOther(CWBlocks.ENCASED_ARCANE_PIPE.get(), CWBlocks.ARCANE_PIPE.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return CWBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
