package net.ttzplayz.create_wizardry.client;

import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.render.SpellRenderingHelper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.ttzplayz.create_wizardry.CreateWizardry;
import net.ttzplayz.create_wizardry.client.rendering.ManaSiphonOrbRenderer;
import net.ttzplayz.create_wizardry.effect.CWMobEffects;

// renders blaze caster beams on their armorstand proxy, bypassing flywheel ber suppression
@EventBusSubscriber(modid = CreateWizardry.MOD_ID, value = Dist.CLIENT)
public class CWClientRenderEvents {

    @SubscribeEvent
    public static void afterLivingRender(RenderLivingEvent.Post<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        if (ClientBlazeBeams.contains(entity.getId())) {
            SpellRenderingHelper.renderRayOfSiphoning(entity, event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick());
        }
    }

    // wobble drained mobs like a converting villager; players excluded
    @SubscribeEvent
    public static void wobbleDrainedMob(RenderLivingEvent.Pre<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Mob && entity.hasEffect(CWMobEffects.SIPHON_LOCK)) {
            float wobble = (float) (Math.cos(entity.tickCount * 3.25) * Math.PI * 0.25);
            event.getPoseStack().mulPose(Axis.YP.rotationDegrees(wobble));
        }
    }

    // mana orbs render here so they show whether or not flywheel suppresses the ber
    @SubscribeEvent
    public static void renderManaOrbs(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        ManaSiphonOrbRenderer.renderAll(event.getPoseStack(), event.getCamera(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientBlazeBeams.clear();
            ClientManaSiphons.clear();
        }
    }
}
