package net.ttzplayz.create_wizardry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ttzplayz.create_wizardry.block.CWBlocks;

public class CWCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateWizardry.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATE_WIZARDRY_TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .icon(() -> CWBlocks.BLAZE_CASTER.toStack())
                    .title(Component.translatable("itemGroup.create_wizardry.main"))
                    .build());

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
