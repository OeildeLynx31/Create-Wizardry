package net.ttzplayz.create_wizardry.event;

import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.ttzplayz.create_wizardry.CreateWizardry;
import net.ttzplayz.create_wizardry.advancement.CWAdvancements;
import net.ttzplayz.create_wizardry.item.CWItems;

@EventBusSubscriber(modid = CreateWizardry.MOD_ID)
// pickup advancements
public class CWAdvancementEvents {

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer sp) || sp.isFakePlayer()) return;
        ItemStack stack = event.getOriginalStack();
        if (stack.isEmpty()) return;

        if (stack.is(CWItems.ARCANE_SHEET.get())) {
            CWAdvancements.SPLOINK.awardTo(sp);
        } else if (stack.is(ItemRegistry.ARCANE_ESSENCE.get())) {
            // essence synthesised from dusts + mana
            CWAdvancements.ALCH_101.awardTo(sp);
        }
    }
}
