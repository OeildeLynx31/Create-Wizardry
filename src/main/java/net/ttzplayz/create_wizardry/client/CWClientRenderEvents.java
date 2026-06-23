package net.ttzplayz.create_wizardry.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
import software.bernie.geckolib.event.GeoRenderEvent;

// renders blaze caster beams on their armorstand proxy, bypassing flywheel ber suppression
@EventBusSubscriber(modid = CreateWizardry.MOD_ID, value = Dist.CLIENT)
public class CWClientRenderEvents {

    // peak drain-wobble twist, in degrees
    private static final float WOBBLE_DEGREES = 2.5f;

    @SubscribeEvent
    public static void afterLivingRender(RenderLivingEvent.Post<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        if (ClientBlazeBeams.contains(entity.getId())) {
            SpellRenderingHelper.renderRayOfSiphoning(entity, event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick());
        }
    }

    // wobble drained mobs like a converting villager; players excluded.
    // vanilla-rendered mobs (e.g. the villager/piglin/skeleton/spider conversion targets) come through here
    @SubscribeEvent
    public static void wobbleDrainedMob(RenderLivingEvent.Pre<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> event) {
        applyDrainWobble(event.getEntity(), event.getPoseStack());
    }

    // Iron's Spellbooks casters are GeckoLib-rendered and never fire RenderLivingEvent, so wobble them
    // off GeckoLib's own pre-render event instead (these are the mobs that "have mana")
    @SubscribeEvent
    public static void wobbleDrainedGeckoMob(GeoRenderEvent.Entity.Pre event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            applyDrainWobble(entity, event.getPoseStack());
        }
    }

    private static void applyDrainWobble(LivingEntity entity, PoseStack poseStack) {
        if (entity instanceof Mob && entity.hasEffect(CWMobEffects.SIPHON_LOCK)) {
            float wobble = (float) (Math.cos(entity.tickCount * 3.25) * WOBBLE_DEGREES);
            poseStack.mulPose(Axis.YP.rotationDegrees(wobble));
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
