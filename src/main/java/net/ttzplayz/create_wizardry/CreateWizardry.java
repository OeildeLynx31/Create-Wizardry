package net.ttzplayz.create_wizardry;

import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.CreateClient;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;
import io.redspace.ironsspellbooks.fluids.SimpleClientFluidType;
import io.redspace.ironsspellbooks.fluids.SimpleTintedClientFluidType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.ttzplayz.create_wizardry.entity.CWManaTransformations;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.GameData;
import net.neoforged.neoforge.registries.RegisterEvent;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.fluids.pipes.GlassPipeVisual;
import com.simibubi.create.content.fluids.pipes.TransparentStraightPipeRenderer;
import com.simibubi.create.content.fluids.pump.PumpRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.ttzplayz.create_wizardry.advancement.CWAdvancements;
import net.ttzplayz.create_wizardry.advancement.CWTriggers;
import net.ttzplayz.create_wizardry.block.CWBlocks;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import net.ttzplayz.create_wizardry.client.CWPartialModels;
import net.ttzplayz.create_wizardry.client.CWSpriteShifts;
import net.ttzplayz.create_wizardry.client.pipe.ArcanePartialModels;
import net.ttzplayz.create_wizardry.client.pipe.ArcanePipeAttachmentModel;
import net.ttzplayz.create_wizardry.client.rendering.BlazeCasterRenderer;
import net.ttzplayz.create_wizardry.client.rendering.BlazeCasterVisual;
import net.ttzplayz.create_wizardry.client.rendering.ManaSiphonRenderer;
import net.ttzplayz.create_wizardry.client.rendering.ManaSiphonVisual;
import net.ttzplayz.create_wizardry.client.rendering.ChannelerRenderer;
import net.ttzplayz.create_wizardry.advancement.CWBuiltInTriggers;
import net.ttzplayz.create_wizardry.effect.CWMobEffects;
import net.ttzplayz.create_wizardry.event.CWEvents;
import net.ttzplayz.create_wizardry.fluids.CWEffectHandlers;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;
import net.ttzplayz.create_wizardry.item.CWItems;
import net.ttzplayz.create_wizardry.particle.CWParticles;
import net.ttzplayz.create_wizardry.client.particle.RuneParticle;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.slf4j.Logger;

import java.util.UUID;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.ttzplayz.create_wizardry.block.blaze_caster.BlazeCasterBlockEntity;

import static com.simibubi.create.AllBlocks.BLAZE_BURNER;
import static com.simibubi.create.AllBlocks.STEAM_WHISTLE;
import static io.redspace.ironsspellbooks.registries.CreativeTabRegistry.MATERIALS_TAB;
import static io.redspace.ironsspellbooks.registries.FluidRegistry.BLOOD;
import static io.redspace.ironsspellbooks.registries.FluidRegistry.ICE_VENOM_FLUID;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.RAW_MITHRIL;
import static net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
import static net.ttzplayz.create_wizardry.block.CWBlocks.BLAZE_CASTER;
import static net.ttzplayz.create_wizardry.block.CWBlocks.CHANNELER;
import static net.ttzplayz.create_wizardry.fluids.CWFluidRegistry.*;
import static net.ttzplayz.create_wizardry.item.CWItems.*;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CreateWizardry.MOD_ID)
public class CreateWizardry {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "create_wizardry";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CW_CREEPER_CHARGE_COUNT = "cw_charged_creepers";
    private static final int MAX_CHARGED_CREEPERS_PER_BOLT = 4;

    public CreateWizardry(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(CWEvents::registerCapabilities);
        modEventBus.addListener(CreateWizardry::modifyEntityAttributes);
        modEventBus.addListener(CreateWizardry::addPackFinders);

        NeoForge.EVENT_BUS.register(this);

        // register custom gamerules early
        CWGameRules.register();

        CWFluidRegistry.register(modEventBus);
        CWBlocks.register(modEventBus);
        CWBlockEntities.register(modEventBus);
        CWItems.register(modEventBus);
        CWMobEffects.register(modEventBus);
        CWParticles.register(modEventBus);
        CWCreativeTabs.register(modEventBus);
        CWBuiltInTriggers.register(modEventBus);
        CWAdvancements.registerTriggers();

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, CWConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> OpenPipeEffectHandler.REGISTRY.register(MANA.get(), new CWEffectHandlers.ManaEffectHandler()));
        event.enqueueWork(() -> OpenPipeEffectHandler.REGISTRY.register(LIGHTNING.get(), new CWEffectHandlers.LightningEffectHandler()));
        event.enqueueWork(() -> OpenPipeEffectHandler.REGISTRY.register(FIRE_ALE_FLUID.get(), new CWEffectHandlers.FireAleEffectHandler()));
        event.enqueueWork(() -> OpenPipeEffectHandler.REGISTRY.register(NETHERWARD_TINCTURE_FLUID.get(), new CWEffectHandlers.NetherwardEffectHandler()));
        event.enqueueWork(() -> OpenPipeEffectHandler.REGISTRY.register(ICE_VENOM_FLUID.get(), new CWEffectHandlers.IceVenomEffectHandler()));
        event.enqueueWork(() -> OpenPipeEffectHandler.REGISTRY.register(BLOOD.get(), new CWEffectHandlers.BloodEffectHandler()));

        // Register the arcane pipe as encasable into the arcane encased pipe (mirrors Create's copper casing flow)
        event.enqueueWork(() -> EncasingRegistry.addVariant(
                CWBlocks.ARCANE_PIPE.get(), CWBlocks.ENCASED_ARCANE_PIPE.get()));

        // lava + blood: flowing lava -> crimsite, source lava -> obsidian
        event.enqueueWork(() -> {
            BlockState crimsite = BuiltInRegistries.BLOCK
                    .getOptional(ResourceLocation.fromNamespaceAndPath("create", "crimsite"))
                    .map(Block::defaultBlockState)
                    .orElse(Blocks.OBSIDIAN.defaultBlockState());
            FluidInteractionRegistry.addInteraction(
                    NeoForgeMod.LAVA_TYPE.value(),
                    new FluidInteractionRegistry.InteractionInformation(
                            io.redspace.ironsspellbooks.registries.FluidRegistry.BLOOD_TYPE.value(),
                            lavaState -> lavaState.isSource()
                                    ? Blocks.OBSIDIAN.defaultBlockState()
                                    : crimsite));
        });

        event.enqueueWork(() -> {
            Holder<PoiType> lightningRod =
                    BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(PoiTypes.LIGHTNING_ROD);

            for (BlockState state : CHANNELER.get().getStateDefinition().getPossibleStates()) {
                // Optional: guard against conflicts
                Holder<PoiType> old = GameData.getBlockStatePointOfInterestTypeMap().put(state, lightningRod);
                if (old != null && old != lightningRod) {
                    throw new IllegalStateException("Channeler state already assigned to POI: " + old);
                }
            }
        });
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == MATERIALS_TAB.getKey()) {
            event.insertAfter(RAW_MITHRIL.get().getDefaultInstance(), CRUSHED_MITHRIL.get().getDefaultInstance(), PARENT_AND_SEARCH_TABS);
            event.insertAfter(MITHRIL_SCRAP.get().getDefaultInstance(), MITHRIL_NUGGET.get().getDefaultInstance(), PARENT_AND_SEARCH_TABS);
            event.accept(MANA_BUCKET.get(), PARENT_AND_SEARCH_TABS);
            event.accept(LIGHTNING_BUCKET.get(),  PARENT_AND_SEARCH_TABS);
            event.accept(BLOOD_BUCKET.get(),  PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey() == AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey()) {
            event.insertAfter(STEAM_WHISTLE.asStack(), CHANNELER.toStack(), PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey() == AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey()) {
            event.insertAfter(BLAZE_BURNER.asStack(), BLAZE_CASTER.toStack(), PARENT_AND_SEARCH_TABS);
        }
        if (event.getTabKey() == CWCreativeTabs.CREATE_WIZARDRY_TAB.getKey()) {
            event.accept(BLAZE_CASTER.get());
            event.accept(CHANNELER.get());
            event.accept(CWBlocks.MANA_SIPHON.get());
            event.accept(CWBlocks.ARCANE_BLOCK.get());
            event.accept(CWBlocks.ARCANE_CASING.get());
            event.accept(CWBlocks.ARCANE_PIPE.get());
            event.accept(CWBlocks.SMART_ARCANE_PIPE.get());
            event.accept(CWBlocks.ARCANE_PUMP.get());
            event.accept(ARCANE_SHEET.get());
            event.accept(CASTERS_SCONE.get());
            event.accept(CRUSHED_MITHRIL.get());
            event.accept(MITHRIL_NUGGET.get());
            event.accept(MANA_BUCKET.get());
            event.accept(LIGHTNING_BUCKET.get());
            event.accept(BLOOD_BUCKET.get());
        }
    }

    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.ARMOR_STAND, Attributes.ATTACK_DAMAGE);
    }

    // registers the opt-in arcane_alloy resource pack (renames arcane ingot/block)
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "arcane_alloy"),
                    PackType.CLIENT_RESOURCES,
                    Component.literal("Create Wizardry: Arcane Alloy"),
                    PackSource.BUILT_IN,
                    false,
                    Pack.Position.TOP);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof Mob mob)) return;
        // spellbook on a mana-exposed mob arms a caster transformation
        if (CWManaTransformations.tryConvertViaInteract(event.getEntity(), mob, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        // drive the admire delay and finish armed transformations
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide()) {
            CWManaTransformations.tickPendingConversion(mob);
        }
        // low mana (< 5) applies depletion: slowness and slowed regen, self-clears once mana recovers
        if (CWConfig.manaDepletionEnabled
                && event.getEntity() instanceof Player player && !player.level().isClientSide()
                && !player.isCreative() && !player.isSpectator()) {
            float mana = MagicData.getPlayerMagicData(player).getMana();
            if (mana < 5) {
                MobEffectInstance current = player.getEffect(CWMobEffects.DEPLETION);
                if (current == null || current.getDuration() < 30) {
                    player.addEffect(new MobEffectInstance(CWMobEffects.DEPLETION, 60, 0, false, true, true));
                }
            }
        }
    }

    @SubscribeEvent
    public void onSpellPreCast(SpellPreCastEvent event) {
        // Depleted players (fully drained by a Mana Siphon) cannot cast spells. (This event only
        // fires for players; drained mobs are suppressed via SIPHON_LOCK in onEntityTick.)
        if (event.getEntity() != null && event.getEntity().hasEffect(CWMobEffects.DEPLETION)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;

        // Synchronous cast protection (covers direct spell damage during onCast())
        UUID immuneUuid = BlazeCasterBlockEntity.ACTIVE_PLACER.get();
        if (immuneUuid != null && player.getUUID().equals(immuneUuid)) {
            event.setCanceled(true);
            return;
        }

        // persistent entity protection (black hole, summoned mobs)
        net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
        if (attacker == null) attacker = event.getSource().getDirectEntity();
        if (attacker == null) return;
        UUID spawnedByPlacer = BlazeCasterBlockEntity.SPAWNED_ENTITY_PLACER.get(attacker.getUUID());
        if (spawnedByPlacer != null && player.getUUID().equals(spawnedByPlacer))
            event.setCanceled(true);
    }
    // Mob-effect removals aren't synced to tracking clients, so when SIPHON_LOCK ends server-side we
    // broadcast the removal ourselves; otherwise the drained mob keeps shaking forever on the client.
    @SubscribeEvent
    public void onSiphonLockExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().is(CWMobEffects.SIPHON_LOCK)) {
            CWMobEffects.broadcastSiphonLockRemoval(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onSiphonLockRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().is(CWMobEffects.SIPHON_LOCK)) {
            CWMobEffects.broadcastSiphonLockRemoval(event.getEntity());
        }
    }

    public static void onRegister(final RegisterEvent event) {
        if (event.getRegistry() == BuiltInRegistries.TRIGGER_TYPES) {
            CWAdvancements.registerTriggers();
            CWTriggers.register();
        }
    }

    @SubscribeEvent
    public void onLightningStrike(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof Creeper)) {
            return;
        }
        var data = event.getLightning().getPersistentData();
        int charged = data.getInt(CW_CREEPER_CHARGE_COUNT);
        if (charged >= MAX_CHARGED_CREEPERS_PER_BOLT) {
            event.setCanceled(true);
            return;
        }
        data.putInt(CW_CREEPER_CHARGE_COUNT, charged + 1);
    }
    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            CWPartialModels.register();
            CWSpriteShifts.register();
            ArcanePartialModels.register();
            event.enqueueWork(() -> {
                SimpleBlockEntityVisualizer.builder(CWBlockEntities.BLAZE_CASTER_BE.get())
                        .factory(BlazeCasterVisual::new)
                        .skipVanillaRender(be -> true)
                        .apply();
                // flywheel; ber is fallback
                SimpleBlockEntityVisualizer.builder(CWBlockEntities.MANA_SIPHON_BE.get())
                        .factory(ManaSiphonVisual::new)
                        .skipVanillaRender(be -> true)
                        .apply();
                // glass pipe fluid via flywheel; ber is fallback
                SimpleBlockEntityVisualizer.builder(CWBlockEntities.GLASS_ARCANE_PIPE.get())
                        .factory(GlassPipeVisual::new)
                        .skipVanillaRender(be -> true)
                        .apply();

                SimpleBlockEntityVisualizer.builder(CWBlockEntities.ARCANE_PUMP.get())
                        .factory(SingleAxisRotatingVisual.ofZ(AllPartialModels.MECHANICAL_PUMP_COG))
                        .skipVanillaRender(be -> true)
                        .apply();
                CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                        .register(CreateWizardry.id("arcane_casing"),
                                model -> new CTModel(model, new SimpleCTBehaviour(CWSpriteShifts.ARCANE_CASING)));


                CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                        .register(CreateWizardry.id("arcane_pipe"), ArcanePipeAttachmentModel::withAO);
                CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                        .register(CreateWizardry.id("smart_arcane_pipe"), ArcanePipeAttachmentModel::withAO);

                CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                        .register(CreateWizardry.id("glass_arcane_pipe"), ArcanePipeAttachmentModel::withAO);
                CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                        .register(CreateWizardry.id("encased_arcane_pipe"), ArcanePipeAttachmentModel::withAO);

                CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                        .register(CreateWizardry.id("arcane_pump"), ArcanePipeAttachmentModel::withAO);
            });
        }

        @SubscribeEvent
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            event.registerFluidType(new SimpleClientFluidType(CreateWizardry.id("block/mana_still")), CWFluidRegistry.MANA_TYPE);
            event.registerFluidType(new SimpleClientFluidType(CreateWizardry.id("block/lightning")), CWFluidRegistry.LIGHTNING_TYPE);
            event.registerFluidType(new SimpleTintedClientFluidType(ResourceLocation.withDefaultNamespace("block/water_still"), 0x00831312), FIRE_ALE_TYPE);
            event.registerFluidType(new SimpleTintedClientFluidType(ResourceLocation.fromNamespaceAndPath("neoforge", "block/milk_still"), 0x00D69D84), NETHERWARD_TINCTURE_TYPE);
        }

        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            CWParticles.RUNES.forEach(holder ->
                    event.registerSpriteSet(holder.get(), RuneParticle.Provider::new));
        }

        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(CWBlockEntities.CHANNELER_BE.get(), ChannelerRenderer::new);
            event.registerBlockEntityRenderer(CWBlockEntities.BLAZE_CASTER_BE.get(), BlazeCasterRenderer::new);
            event.registerBlockEntityRenderer(CWBlockEntities.MANA_SIPHON_BE.get(), ManaSiphonRenderer::new);
            event.registerBlockEntityRenderer(CWBlockEntities.SMART_ARCANE_PIPE.get(), SmartBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(CWBlockEntities.GLASS_ARCANE_PIPE.get(), TransparentStraightPipeRenderer::new);
            event.registerBlockEntityRenderer(CWBlockEntities.ARCANE_PUMP.get(), PumpRenderer::new);
        }

    }
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
