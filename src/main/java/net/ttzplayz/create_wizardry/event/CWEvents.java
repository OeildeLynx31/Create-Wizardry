package net.ttzplayz.create_wizardry.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ttzplayz.create_wizardry.block.blaze_caster.BlazeCasterBlockEntity;
import net.ttzplayz.create_wizardry.block.channeler.ChannelerBlockEntity;
import net.ttzplayz.create_wizardry.block.mana_siphon.ManaSiphonBlockEntity;

public class CWEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ChannelerBlockEntity.registerCapabilities(event);
        BlazeCasterBlockEntity.registerCapabilities(event);
        ManaSiphonBlockEntity.registerCapabilities(event);
    }
}
