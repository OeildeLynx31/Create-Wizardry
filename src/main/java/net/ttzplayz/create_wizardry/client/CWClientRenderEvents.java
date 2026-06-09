package net.ttzplayz.create_wizardry.client;

import io.redspace.ironsspellbooks.render.SpellRenderingHelper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.ttzplayz.create_wizardry.CreateWizardry;

/**
 * Client-side rendering for Blaze Caster spell visuals that ISS only draws for players / its own
 * caster-mobs. The Blaze Caster channels through an invisible ArmorStand proxy, so we render the
 * Ray of Siphoning beam on that proxy ourselves — mirroring ISS's
 * {@code ClientPlayerEvents.afterLivingRender}. This runs in the entity render pipeline, so it
 * works regardless of whether Flywheel is suppressing the block-entity renderer.
 */
@EventBusSubscriber(modid = CreateWizardry.MOD_ID, value = Dist.CLIENT)
public class CWClientRenderEvents {

    @SubscribeEvent
    public static void afterLivingRender(RenderLivingEvent.Post<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        if (ClientBlazeBeams.contains(entity.getId())) {
            SpellRenderingHelper.renderRayOfSiphoning(entity, event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide())
            ClientBlazeBeams.clear();
    }
}
