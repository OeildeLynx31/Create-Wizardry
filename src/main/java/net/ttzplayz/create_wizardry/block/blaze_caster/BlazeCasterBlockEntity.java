package net.ttzplayz.create_wizardry.block.blaze_caster;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.ttzplayz.create_wizardry.particle.CWParticles;
import io.redspace.ironsspellbooks.particle.ZapParticleOption;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ttzplayz.create_wizardry.advancement.CWAdvancements;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ttzplayz.create_wizardry.CreateWizardry;
import net.ttzplayz.create_wizardry.CWConfig;
import net.ttzplayz.create_wizardry.block.CWBlockEntities;
import net.ttzplayz.create_wizardry.block.pipe.ManaPipeTransport;
import net.ttzplayz.create_wizardry.client.CWPartialModels;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import com.mojang.authlib.GameProfile;

public class BlazeCasterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final ThreadLocal<UUID> ACTIVE_PLACER = new ThreadLocal<>();
    // Maps entity UUID → placer UUID for persistent spell entities (Black Hole, summons, etc.)
    public static final java.util.concurrent.ConcurrentHashMap<UUID, UUID> SPAWNED_ENTITY_PLACER
            = new java.util.concurrent.ConcurrentHashMap<>();

    // Spells whose getRecastCount() > 0 for reasons unrelated to multi-target firing
    // (e.g. raise_dead uses recastCount to raise multiple corpses per cast, not to target extra mobs)
    private static final Set<String> NO_EXTRA_RECAST_SPELLS = Set.of(
        "raise_dead"
    );

    private static final Set<String> SPELL_BLACKLIST = Set.of(
        // Melee spells
        "echoing_strikes", "flaming_strike", "shadow_slash",
        "volt_strike", "divine_smite", "touch_dig", "heartstop", "wall_of_fire",
        // Movement spells
        "teleport", "recall", "blood_step", "frost_step", "burning_dash",
        "thunder_step", "evasion", "charge", "ascension", "angel_wing", "portal",
        // Inventory/utility spells
        "summon_ender_chest", "summon_horse", "summon_polar_bear",
        // Self-effect spells
        "sacrifice", "invisibility", "haste", "spider_aspect",
        // Healing spells
        "heal", "greater_heal", "ice_tomb", "healing_circle", "fortify"
    );

    public static boolean isSpellBlacklisted(AbstractSpell spell) {
        return SPELL_BLACKLIST.contains(spell.getSpellResource().getPath());
    }

    private static final Map<String, String> HAT_TO_SCHOOL = Map.ofEntries(
        Map.entry("pyromancer_helmet",       "fire"),
        Map.entry("electromancer_helmet",    "lightning"),
        Map.entry("cryomancer_helmet",       "ice"),
        Map.entry("archevoker_helmet",       "evocation"),
        Map.entry("cultist_helmet",          "blood"),
        Map.entry("plagued_helmet",          "nature"),
        Map.entry("priest_helmet",           "holy"),
        Map.entry("shadowwalker_helmet",     "ender")
    );
    protected ItemStack heldItem = ItemStack.EMPTY;
    protected ItemStack heldHat = ItemStack.EMPTY;
    public SmartFluidTankBehaviour internalTank;
    // lazy caps
    private final Map<Direction, IFluidHandler> decayingCaps = new HashMap<>();
    public final LerpedFloat headAnimation = LerpedFloat.linear();
    public final LerpedFloat headAngle = LerpedFloat.angular();

    protected boolean creative = false;
    // Permanent superheat from the Creative Blaze Cake (the second step of its toggle cycle).
    protected boolean creativeSuperheat = false;
    protected int castTicksRemaining = 0;
    protected int cooldownTicksRemaining = 0;
    protected int superheatTicksRemaining = 0;
    @Nullable protected UUID placerUuid;
    protected boolean wasPowered = false;
    protected boolean lockedHead = false;
    protected float lockedYaw = 0f;
    protected final Set<UUID> trackedSummons = new HashSet<>();
    @Nullable private ArmorStand channelProxy;
    @Nullable private MagicData channelMagicData;
    @Nullable private AbstractSpell channelSpell;
    private int channelSpellLevel;
    private int channelTicksRemaining;
    // Client-synced channel state (for the goggle "Casting..." line and the Ray of Siphoning beam)
    private int clientChannelProxyId;
    private String clientChannelSpellPath = "";
    private int beamProxyIdInSet; // client only: proxy id currently registered in ClientBlazeBeams

    public boolean isActive() {
        return castTicksRemaining > 0 || channelTicksRemaining > 0;
    }

    public boolean isCreative() {
        return creative;
    }

    public boolean isSuperheated() {
        return superheatTicksRemaining > 0 || creativeSuperheat;
    }

    public int getSuperheatTicksRemaining() {
        return superheatTicksRemaining;
    }

    /** Superheats the caster (or refreshes the timer) for the configured duration. */
    public void applySuperheat() {
        superheatTicksRemaining = CWConfig.blazeCasterSuperheatDuration;
        updateBlockState();
        notifyUpdate();
    }

    private int superheatCooldown(int base) {
        if (!isSuperheated()) return base;
        return Math.max(1, (int) (base * CWConfig.blazeCasterSuperheatCooldownMult));
    }

    public BlazeBurnerBlock.HeatLevel getHeatLevel() {
        if (castTicksRemaining > 0 || channelTicksRemaining > 0) return BlazeBurnerBlock.HeatLevel.FADING;
        boolean hasMana = creative || (internalTank != null
                && internalTank.getPrimaryHandler().getFluidInTank(0).getAmount() > 0);
        return hasMana ? BlazeBurnerBlock.HeatLevel.SMOULDERING : BlazeBurnerBlock.HeatLevel.NONE;
    }

    /**
     * Cycles the Creative Blaze Cake state, mirroring Create's Blaze Burner:
     * inert -> creative (no mana drain) -> creative + permanently superheated -> inert.
     */
    public void toggleCreativeHeat() {
        if (!creative) {                       // inert -> creative
            creative = true;
            creativeSuperheat = false;
        } else if (!creativeSuperheat) {       // creative -> creative + superheated
            creativeSuperheat = true;
        } else {                               // creative + superheated -> inert
            creative = false;
            creativeSuperheat = false;
        }
        updateBlockState();
        notifyUpdate();
    }

    public BlazeCasterBlockEntity(BlockPos pos, BlockState state) {
        super(CWBlockEntities.BLAZE_CASTER_BE.get(), pos, state);
        headAngle.startWithValue((AngleHelper.horizontalAngle(state.getOptionalValue(BlazeBurnerBlock.FACING)
                .orElse(Direction.SOUTH)) + 180) % 360);
    }

    @OnlyIn(Dist.CLIENT)
    public PartialModel getBlazeModel(BlazeBurnerBlock.HeatLevel heatLevel, boolean active) {
        if (!heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING))
            return CWPartialModels.BLAZE_CASTER_INERT;
        String element = getElementId();
        if (isSuperheated()) {
            if (active && CWPartialModels.SUPERHEAT_BLAZE_ACTIVE_BY_ELEMENT.containsKey(element))
                return CWPartialModels.SUPERHEAT_BLAZE_ACTIVE_BY_ELEMENT.get(element);
            if (CWPartialModels.SUPERHEAT_BLAZE_BY_ELEMENT.containsKey(element))
                return CWPartialModels.SUPERHEAT_BLAZE_BY_ELEMENT.get(element);
            // No supercharged art for this element yet: keep the regular element head instead of the
            // magenta/black placeholder. The empty caster keeps the null (void) supercharged head.
            if ("none".equals(element)) return CWPartialModels.SUPERHEAT_BLAZE_NULL;
        }
        return CWPartialModels.BLAZE_BY_ELEMENT.getOrDefault(element, CWPartialModels.BLAZE_CASTER_NONE);
    }

    // Rod models mirror getBlazeModel: superheated rods use the supercharged caster texture
    // (null rod for unmapped/none elements), otherwise the regular per-element rod.
    @OnlyIn(Dist.CLIENT)
    public PartialModel getRodSmallModel() {
        String element = getElementId();
        if (isSuperheated()) {
            if (CWPartialModels.SUPERHEAT_ROD_SMALL_BY_ELEMENT.containsKey(element))
                return CWPartialModels.SUPERHEAT_ROD_SMALL_BY_ELEMENT.get(element);
            // No supercharged rod art yet: keep the regular element rod; null void rod for the empty caster.
            if ("none".equals(element)) return CWPartialModels.ROD_SMALL_SUPERHEAT_NULL;
        }
        return CWPartialModels.ROD_SMALL_BY_ELEMENT.getOrDefault(element, com.simibubi.create.AllPartialModels.BLAZE_BURNER_RODS);
    }

    @OnlyIn(Dist.CLIENT)
    public PartialModel getRodLargeModel() {
        String element = getElementId();
        if (isSuperheated()) {
            if (CWPartialModels.SUPERHEAT_ROD_LARGE_BY_ELEMENT.containsKey(element))
                return CWPartialModels.SUPERHEAT_ROD_LARGE_BY_ELEMENT.get(element);
            if ("none".equals(element)) return CWPartialModels.ROD_LARGE_SUPERHEAT_NULL;
        }
        return CWPartialModels.ROD_LARGE_BY_ELEMENT.getOrDefault(element, com.simibubi.create.AllPartialModels.BLAZE_BURNER_RODS_2);
    }

    // Emissive "black hole" glow overlay shown only while superheated. Ender has art (purple idle,
    // pink/white while casting); other elements have no glow yet (null).
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getSuperheatGlowModel(BlazeBurnerBlock.HeatLevel heatLevel, boolean active) {
        if (!isSuperheated() || !heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING)) return null;
        if (!"ender".equals(getElementId())) return null;
        return active ? CWPartialModels.SUPERHEAT_GLOW_ENDER_ACTIVE : CWPartialModels.SUPERHEAT_GLOW_ENDER;
    }

    // Glow-outline shells for the rods, matching the head glow (ender only).
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getRodSmallGlowModel(BlazeBurnerBlock.HeatLevel heatLevel, boolean active) {
        if (!isSuperheated() || !heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING)) return null;
        if (!"ender".equals(getElementId())) return null;
        return active ? CWPartialModels.ROD_SMALL_GLOW_ENDER_ACTIVE : CWPartialModels.ROD_SMALL_GLOW_ENDER;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getRodLargeGlowModel(BlazeBurnerBlock.HeatLevel heatLevel, boolean active) {
        if (!isSuperheated() || !heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING)) return null;
        if (!"ender".equals(getElementId())) return null;
        return active ? CWPartialModels.ROD_LARGE_GLOW_ENDER_ACTIVE : CWPartialModels.ROD_LARGE_GLOW_ENDER;
    }

    public String getElementId() {
        if (heldItem.isEmpty()) return "none";
        ISpellContainer container = ISpellContainer.get(heldItem);
        if (container == null || container.isEmpty()) return "none";
        SpellData sd = container.getSpellAtIndex(0);
        if (sd == null || sd == SpellData.EMPTY) return "none";
        AbstractSpell spell = sd.getSpell();
        if (spell == null) return "none";
        SchoolType school = spell.getSchoolType();
        if (school == null) return "none";
        String path = school.getId().getPath();
        return CWPartialModels.BLAZE_BY_ELEMENT.containsKey(path) ? path : "none";
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getHatModel(BlazeBurnerBlock.HeatLevel heatLevel) {
        if (heldHat.isEmpty()) return null;
        String itemPath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(heldHat.getItem()).getPath();
        if ("wizard_helmet".equals(itemPath)) {
            @SuppressWarnings("unchecked")
            DataComponentType<String> variantType = (DataComponentType<String>)
                (DataComponentType<?>) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                    .get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "clothing_variant"));
            if (variantType != null && "hat".equals(heldHat.get(variantType)))
                return CWPartialModels.ISS_WIZARD_HAT;
            return CWPartialModels.ISS_WIZARD_HOOD;
        }
        return CWPartialModels.HAT_BY_ITEM.get(itemPath);
    }

    // Non-dyeable "base" parts (metal buckle, helmet) rendered untinted alongside the dyeable hat model
    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getHatBaseModel(BlazeBurnerBlock.HeatLevel heatLevel) {
        if (heldHat.isEmpty()) return null;
        String itemPath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(heldHat.getItem()).getPath();
        if ("wizard_helmet".equals(itemPath)) {
            @SuppressWarnings("unchecked")
            DataComponentType<String> variantType = (DataComponentType<String>)
                (DataComponentType<?>) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                    .get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "clothing_variant"));
            if (variantType != null && "hat".equals(heldHat.get(variantType)))
                return CWPartialModels.ISS_WIZARD_HAT_BASE;
            return null;
        }
        return CWPartialModels.HAT_BASE_BY_ITEM.get(itemPath);
    }

    public int getHatDyeColor() {
        DyedItemColor dyed = heldHat.get(DataComponents.DYED_COLOR);
        if (dyed != null) return dyed.rgb();
        if (heldHat.isEmpty()) return 0xFFFFFF;
       //dye layer tinting
        String itemPath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(heldHat.getItem()).getPath();
        return CWPartialModels.DEFAULT_HAT_COLOR.getOrDefault(itemPath, 0xFFFFFF);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getGogglesModel(BlazeBurnerBlock.HeatLevel heatLevel) {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public PartialModel getEyesModel(BlazeBurnerBlock.HeatLevel heatLevel) {
        if (!heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING)) return null;
        // The superheated head has its own glow; hide the normal blaze eyes while superheated.
        if (isSuperheated()) return null;
        return heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)
                ? CWPartialModels.BLAZE_CASTER_ACTIVE_EYES
                : CWPartialModels.BLAZE_CASTER_IDLE_EYES;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean showed = false;
        if (internalTank != null)
            showed = containedFluidTooltip(tooltip, isPlayerSneaking, internalTank.getPrimaryHandler());

        if (!heldItem.isEmpty()) {
            ISpellContainer container = ISpellContainer.get(heldItem);
            if (container != null && !container.isEmpty()) {
                SpellData sd = container.getSpellAtIndex(0);
                if (sd != null && sd != SpellData.EMPTY) {
                    tooltip.add(Component.translatable("create_wizardry.tooltip.spell",
                            Component.translatable(sd.getSpell().getComponentId()))
                            .withStyle(ChatFormatting.GRAY));
                    SchoolType school = sd.getSpell().getSchoolType();
                    if (!isSuperheated() && school != null && "eldritch".equals(school.getId().getPath())) {
                        tooltip.add(Component.translatable("create_wizardry.tooltip.must_be_superheated")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    } else if (SPELL_BLACKLIST.contains(sd.getSpell().getSpellResource().getPath())) {
                        tooltip.add(Component.translatable("create_wizardry.tooltip.spell_incompatible")
                                .withStyle(ChatFormatting.RED));
                    }
                    showed = true;
                }
            }
        } else {
            tooltip.add(Component.translatable("create_wizardry.tooltip.no_scroll")
                    .withStyle(ChatFormatting.DARK_GRAY));
            showed = true;
        }

        if (!heldHat.isEmpty()) {
            tooltip.add(Component.translatable("create_wizardry.tooltip.hat",
                    heldHat.getHoverName()).withStyle(ChatFormatting.GRAY));
            appendHatBonusLines(tooltip);
            showed = true;
        }

        CasterMode mode = getBlockState().getValue(BlazeCasterBlock.MODE);
        if (mode == CasterMode.IMPULSE) {
            String lockKey = lockedHead
                    ? "create_wizardry.tooltip.mode.impulse.locked"
                    : "create_wizardry.tooltip.mode.impulse.unlocked";
            tooltip.add(Component.translatable(lockKey).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("create_wizardry.tooltip.mode." + mode.getSerializedName())
                    .withStyle(ChatFormatting.AQUA));
        }

        if (castTicksRemaining > 0 || channelTicksRemaining > 0) {
            tooltip.add(Component.translatable("create_wizardry.tooltip.casting")
                    .withStyle(ChatFormatting.GOLD));
            showed = true;
        } else if (cooldownTicksRemaining > 0) {
            float seconds = cooldownTicksRemaining / 20f;
            tooltip.add(Component.translatable("create_wizardry.tooltip.cooldown",
                    String.format("%.1f", seconds))
                    .withStyle(ChatFormatting.YELLOW));
            showed = true;
        } else if (!heldItem.isEmpty()) {
            if (hasEnoughManaFor(heldItem)) {
                tooltip.add(Component.translatable("create_wizardry.tooltip.ready")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.translatable("create_wizardry.tooltip.not_enough_mana")
                        .withStyle(ChatFormatting.RED));
            }
            showed = true;
        }

        if (isSuperheated()) {
            if (superheatTicksRemaining > 0) {
                float seconds = superheatTicksRemaining / 20f;
                tooltip.add(Component.translatable("create_wizardry.tooltip.superheated",
                        String.format("%.0f", seconds))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                // Permanent superheat from the Creative Blaze Cake has no timer.
                tooltip.add(Component.translatable("create_wizardry.tooltip.superheated_permanent")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            showed = true;
        }

        if (creative) {
            tooltip.add(Component.translatable("create_wizardry.tooltip.creative_mode")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            showed = true;
        }
        return showed;
    }

    @OnlyIn(Dist.CLIENT)
    protected boolean shouldTickAnimation() {
        return !VisualizationManager.supportsVisualization(level);
    }

    @OnlyIn(Dist.CLIENT)
    public void tickAnimation() {
        boolean active = getHeatLevelFromBlock().isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) && isActive();

        float targetAngle = 0;
        boolean foundMob = false;

        // In impulse mode with locked head, snap to the locked angle instead of tracking
        if (!isVirtual() && level != null) {
            CasterMode mode = getBlockState().getValue(BlazeCasterBlock.MODE);
            if (mode == CasterMode.IMPULSE && lockedHead) {
                float lockedHeadAngle = -lockedYaw - 180f;
                float adjusted = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), lockedHeadAngle);
                headAngle.chase(adjusted, .25f, LerpedFloat.Chaser.exp(5));
                headAngle.tickChaser();
                headAnimation.chase(active ? 1 : 0, .25f, LerpedFloat.Chaser.exp(.25f));
                headAnimation.tickChaser();
                return;
            }
        }

        // In sentry mode, track the nearest non-player entity in the world
        if (!isVirtual() && level != null) {
            CasterMode mode = getBlockState().getValue(BlazeCasterBlock.MODE);
            if (mode == CasterMode.SENTRY) {
                AABB box = new AABB(worldPosition).inflate(16.0);
                LivingEntity nearest = level.getEntitiesOfClass(LivingEntity.class, box, e ->
                        e.isAlive() && !(e instanceof Player) && !(e instanceof ArmorStand)
                        && !trackedSummons.contains(e.getUUID()))
                    .stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(
                            worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)))
                    .orElse(null);
                if (nearest != null) {
                    double dx = nearest.getX() - (getBlockPos().getX() + 0.5);
                    double dz = nearest.getZ() - (getBlockPos().getZ() + 0.5);
                    targetAngle = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
                    foundMob = true;
                }
            }
        }

        // Fall back to tracking the local player
        if (!foundMob) {
            double x, z;
            if (isVirtual()) {
                x = -4;
                z = -10;
            } else {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null && !player.isInvisible()) {
                    x = player.getX();
                    z = player.getZ();
                } else {
                    x = getBlockPos().getX() + 0.5;
                    z = getBlockPos().getZ() + 0.5;
                }
            }
            double dx = x - (getBlockPos().getX() + 0.5);
            double dz = z - (getBlockPos().getZ() + 0.5);
            targetAngle = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
        }

        float adjustedTarget = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), targetAngle);
        float chaseSpeed = active ? .125f : .25f;
        LerpedFloat.Chaser chaser = active ? LerpedFloat.Chaser.EXP : LerpedFloat.Chaser.exp(5);
        headAngle.chase(adjustedTarget, chaseSpeed, chaser);
        headAngle.tickChaser();

        headAnimation.chase(active ? 1 : 0, .25f, LerpedFloat.Chaser.exp(.25f));
        headAnimation.tickChaser();
    }

    @Override
    public void tick() {
        super.tick();
        assert level != null;
        if (level.isClientSide) {
            if (shouldTickAnimation())
                tickAnimation();
            if (!isVirtual())
                spawnParticles(getHeatLevelFromBlock());
            return;
        }

        // Cooldown ticks down unconditionally; sync every second so goggle tooltip stays accurate
        if (cooldownTicksRemaining > 0) {
            cooldownTicksRemaining--;
            if (cooldownTicksRemaining == 0 || cooldownTicksRemaining % 20 == 0)
                notifyUpdate();
        }

        // Superheat (from a Caster's Scone) ticks down; sync so the goggle tooltip and the
        // supercharged head/glow rendering stay accurate, and once more when it expires.
        if (superheatTicksRemaining > 0) {
            superheatTicksRemaining--;
            if (superheatTicksRemaining == 0 || superheatTicksRemaining % 20 == 0)
                notifyUpdate();
        }

        // Resolve spell from held scroll
        if (heldItem.isEmpty()) { cancelCast(); return; }
        ISpellContainer container = ISpellContainer.get(heldItem);
        if (container == null || container.isEmpty()) { cancelCast(); return; }
        SpellData sd = container.getSpellAtIndex(0);
        if (sd == null || sd == SpellData.EMPTY) { cancelCast(); return; }
        AbstractSpell spell = sd.getSpell();
        int spellLevel = sd.getLevel();

        CasterMode mode = getBlockState().getValue(BlazeCasterBlock.MODE);

        // Handle active continuous channel (before mode-specific logic)
        if (channelProxy != null && channelSpell != null) {
            LivingEntity chTarget = (mode == CasterMode.SENTRY) ? findTarget(16.0) : null;
            if (chTarget != null) {
                double cdx = chTarget.getX() - channelProxy.getX();
                double cdy = chTarget.getEyeY() - channelProxy.getEyeY();
                double cdz = chTarget.getZ() - channelProxy.getZ();
                channelProxy.setYRot((float)(Mth.atan2(cdz, cdx) * 180.0/Math.PI) - 90f);
                channelProxy.setXRot((float)(-Mth.atan2(cdy, Math.sqrt(cdx*cdx+cdz*cdz)) * 180.0/Math.PI));
            }
            if (level.getGameTime() % 10 == 0) retargetSummons(chTarget);
            // Drive the cast state like ISS's MagicManager continuous branch: tick the
            // duration, re-pulse onCast every CONTINUOUS_CAST_TICK_INTERVAL (10) ticks (re-arms
            // Electrocute's cone; safe no-op for Starfall/BlazeStorm which guard on cast data),
            // then run onServerCastTick each tick (which reads getCastDurationRemaining()).
            channelMagicData.handleCastDuration();
            if ((channelMagicData.getCastDurationRemaining() + 1) % 10 == 0) {
                if (placerUuid != null) ACTIVE_PLACER.set(placerUuid);
                try {
                    channelSpell.onCast(level, channelSpellLevel, channelProxy, CastSource.MOB, channelMagicData);
                } finally {
                    ACTIVE_PLACER.remove();
                }
            }
            channelSpell.onServerCastTick(level, channelSpellLevel, channelProxy, channelMagicData);
            if (--channelTicksRemaining <= 0) {
                int chCooldown = channelSpell.getSpellCooldown();
                channelSpell.onServerCastComplete(level, channelSpellLevel, channelProxy, channelMagicData, false);
                endChannel();
                cooldownTicksRemaining = superheatCooldown(chCooldown);
                if (mode == CasterMode.SENTRY && chTarget != null)
                    tryStartCast(spell, spellLevel);
            }
            updateBlockState();
            return;
        }

        if (mode == CasterMode.IMPULSE) {
            boolean powered = level.hasNeighborSignal(worldPosition);
            boolean risingEdge = powered && !wasPowered;
            wasPowered = powered;
            if (castTicksRemaining > 0) {
                castTicksRemaining--;
                if (castTicksRemaining == 0) {
                    executeCast(spell, spellLevel, null);
                    // continuous spells cool down when the channel ends, not now
                    if (channelProxy == null)
                        cooldownTicksRemaining = superheatCooldown(spell.getSpellCooldown());
                }
            } else if (risingEdge) {
                tryStartCast(spell, spellLevel);
            }
            updateBlockState();
            return;
        }

        // Sentry mode
        LivingEntity target = findTarget(16.0);
        if (level.getGameTime() % 10 == 0)
            retargetSummons(target);
        if (castTicksRemaining > 0) {
            castTicksRemaining--;
            if (castTicksRemaining == 0) {
                executeCast(spell, spellLevel, target);
                // continuous spells cool down when the channel ends, not now
                if (channelProxy == null)
                    // min 1-tick cooldown to prevent spam
                    cooldownTicksRemaining = Math.max(1, superheatCooldown(spell.getSpellCooldown()));
            }
        } else if (target != null) {
            tryStartCast(spell, spellLevel);
        }
        updateBlockState();
    }

    private void cancelCast() {
        endChannel();
        if (castTicksRemaining > 0) {
            castTicksRemaining = 0;
            updateBlockState();
        }
    }

    private void endChannel() {
        boolean wasChanneling = channelProxy != null || channelTicksRemaining > 0;
        if (channelProxy != null) { channelProxy.discard(); channelProxy = null; }
        channelMagicData = null;
        channelSpell = null;
        channelTicksRemaining = 0;
        // Sync the cleared channel state so the client drops the "Casting..." line and beam.
        if (wasChanneling && level != null && !level.isClientSide)
            notifyUpdate();
    }

    private void tryStartCast(AbstractSpell spell, int spellLevel) {
        if (cooldownTicksRemaining > 0) return;
        SchoolType school = spell.getSchoolType();
        // Eldritch spells are normally refused; superheating the caster unlocks them.
        if (!isSuperheated() && school != null && "eldritch".equals(school.getId().getPath())) return;
        if (SPELL_BLACKLIST.contains(spell.getSpellResource().getPath())) return;
        int manaCost = computeManaCost(spell, spellLevel);
        IFluidHandler handler = internalTank.getPrimaryHandler();
        if (!creative && handler.getFluidInTank(0).getAmount() < manaCost) return;
        if (!creative)
            handler.drain(manaCost, IFluidHandler.FluidAction.EXECUTE);
        // continuous spells channel for the cast time, so use a minimal windup here
        castTicksRemaining = spell.getCastType() == CastType.CONTINUOUS
                ? 1
                : Math.max(1, spell.getCastTime(spellLevel));
    }

    // mana cost
    private int computeManaCost(AbstractSpell spell, int spellLevel) {
        int manaCost = spell.getManaCost(spellLevel) * 10;
        if (!heldHat.isEmpty()) {
            String hatPath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(heldHat.getItem()).getPath();
            if ("tarnished_helmet".equals(hatPath))
                manaCost = (int) (manaCost * 0.75);
        }
        return manaCost;
    }

    // enough mana
    private boolean hasEnoughManaFor(ItemStack stack) {
        if (creative || internalTank == null) return true;
        ISpellContainer container = ISpellContainer.get(stack);
        if (container == null || container.isEmpty()) return true;
        SpellData sd = container.getSpellAtIndex(0);
        if (sd == null || sd == SpellData.EMPTY) return true;
        AbstractSpell spell = sd.getSpell();
        SchoolType school = spell.getSchoolType();
        if (!isSuperheated() && school != null && "eldritch".equals(school.getId().getPath())) return true;
        if (SPELL_BLACKLIST.contains(spell.getSpellResource().getPath())) return true;
        int manaCost = computeManaCost(spell, sd.getLevel());
        return internalTank.getPrimaryHandler().getFluidInTank(0).getAmount() >= manaCost;
    }

    @Nullable
    private LivingEntity findTarget(double range) {
        if (!(level instanceof ServerLevel)) return null;
        AABB box = new AABB(worldPosition).inflate(range);
        return level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive()
                && !(e instanceof ArmorStand)
                && !(e instanceof Player p && placerUuid != null && p.getUUID().equals(placerUuid))
                && !trackedSummons.contains(e.getUUID())
        ).stream()
         .filter(this::hasLineOfSight)
         .min(Comparator.comparingDouble(e -> e.distanceToSqr(
                 worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)))
         .orElse(null);
    }

    private boolean hasLineOfSight(LivingEntity target) {
        if (level == null) return false;
        Vec3 from = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5);
        Vec3 to = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        HitResult result = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return result.getType() == HitResult.Type.MISS;
    }

    private void executeCast(AbstractSpell spell, int spellLevel, @Nullable LivingEntity target) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ArmorStand proxy = new ArmorStand(EntityType.ARMOR_STAND, serverLevel);
        proxy.setPos(worldPosition.getX() + 0.5, worldPosition.getY() - 0.25, worldPosition.getZ() + 0.5);
        proxy.setCustomName(Component.translatable("block.create_wizardry.blaze_caster"));
        proxy.setNoGravity(true);
        proxy.setSilent(true);
        proxy.setInvisible(true);
        if (target != null) {
            double dx = target.getX() - proxy.getX();
            double dy = target.getEyeY() - proxy.getEyeY();
            double dz = target.getZ() - proxy.getZ();
            proxy.setYRot((float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f);
            proxy.setXRot((float) (-Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI)));
        } else if (lockedHead) {
            proxy.setYRot(lockedYaw);
            proxy.setXRot(0f);
        } else {
            Direction facing = getBlockState().getValue(BlazeCasterBlock.FACING);
            proxy.setYRot(switch (facing) {
                case NORTH -> 180f;
                case SOUTH -> 0f;
                case EAST -> -90f;
                default -> 90f;
            });
            proxy.setXRot(0f);
        }
        serverLevel.addFreshEntity(proxy);
        applyHatSpellPowerBoost(proxy, spell);
        applySuperheatBoost(proxy);

        MagicData magicData = new MagicData(true);
        if (target != null)
            magicData.setAdditionalCastData(new TargetEntityCastData(target));

        // Snapshot all nearby entities before the cast so we can identify newly spawned ones
        Set<UUID> preCastEntities = serverLevel.getEntitiesOfClass(
                net.minecraft.world.entity.Entity.class, new AABB(worldPosition).inflate(64.0))
                .stream().map(net.minecraft.world.entity.Entity::getUUID)
                .collect(Collectors.toSet());

        boolean keepAlive = spell.getCastType() == CastType.CONTINUOUS;

        if (placerUuid != null) ACTIVE_PLACER.set(placerUuid);
        try {
            spell.onCast(serverLevel, spellLevel, proxy, CastSource.MOB, magicData);

            if ("black_hole".equals(spell.getSpellResource().getPath()) && placerUuid != null) {
                ServerPlayer caster = serverLevel.getServer().getPlayerList().getPlayer(placerUuid);
                if (caster != null) CWAdvancements.MEGA_LASER.awardTo(caster);
            }

            if (keepAlive) {
                // start continuous channel; prime cast state so per-tick spell logic runs
                int castDuration = spell.getCastTime(spellLevel);
                // force lazy init of SyncedSpellData or initiateCast NPEs

                magicData.getSyncedData();
                magicData.initiateCast(spell, spellLevel, castDuration, CastSource.MOB, "mainhand");
                channelProxy = proxy;
                channelMagicData = magicData;
                channelSpell = spell;
                channelSpellLevel = spellLevel;
                channelTicksRemaining = castDuration;
                // sync channel state now so the client shows casting, raised pose, and the beam
                notifyUpdate();
            } else {
                // Multi-targeting for burst/barrage spells (e.g. Flame Barrage)
                int recastCount = spell.getRecastCount(spellLevel, proxy);
                if (recastCount > 0 && !NO_EXTRA_RECAST_SPELLS.contains(spell.getSpellResource().getPath())) {
                    List<LivingEntity> extraTargets = level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(worldPosition).inflate(32.0), e ->
                            e.isAlive()
                            && !(e instanceof ArmorStand)
                            && !(e instanceof Player p2 && placerUuid != null && p2.getUUID().equals(placerUuid))
                            && !trackedSummons.contains(e.getUUID())
                            && !e.equals(target)
                    ).stream()
                     .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(
                             worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5)))
                     .limit(recastCount)
                     .toList();

                    for (LivingEntity extra : extraTargets) {
                        double exdx = extra.getX() - proxy.getX();
                        double exdy = extra.getEyeY() - proxy.getEyeY();
                        double exdz = extra.getZ() - proxy.getZ();
                        proxy.setYRot((float)(Mth.atan2(exdz, exdx) * (180.0/Math.PI)) - 90f);
                        proxy.setXRot((float)(-Mth.atan2(exdy, Math.sqrt(exdx*exdx+exdz*exdz)) * (180.0/Math.PI)));
                        magicData.setAdditionalCastData(new TargetEntityCastData(extra));
                        spell.onCast(serverLevel, spellLevel, proxy, CastSource.MOB, magicData);
                    }
                }
            }

            // Spectral Hammer: inject a FakePlayer owner so entity.tick() doesn't NPE on null owner
            if ("spectral_hammer".equals(spell.getSpellResource().getPath())) {
                FakePlayer fp = FakePlayerFactory.get(serverLevel,
                    new GameProfile(placerUuid != null ? placerUuid : UUID.randomUUID(), "BlazeCaster"));
                fp.setPos(worldPosition.getX() + 0.5, worldPosition.getY() - 0.25, worldPosition.getZ() + 0.5);
                serverLevel.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                    new AABB(worldPosition).inflate(16))
                    .stream()
                    .filter(e -> e.getClass().getSimpleName().equals("SpectralHammer"))
                    .forEach(e -> {
                        try {
                            java.lang.reflect.Field f = e.getClass().getDeclaredField("owner");
                            f.setAccessible(true);
                            if (f.get(e) == null) f.set(e, fp); // only fix null owners (ArmorStand-cast)
                        } catch (Exception ex) {
                            org.slf4j.LoggerFactory.getLogger(BlazeCasterBlockEntity.class)
                                .warn("SpectralHammer owner inject failed: {}", ex.getMessage());
                        }
                    });
            }

            // Track newly spawned entities (summons, projectiles, Black Hole, etc.)
            final LivingEntity finalTarget = target;
            final UUID capturedPlacer = placerUuid;
            final BlockPos capturedPos = worldPosition;
            trackNewEntities(serverLevel, preCastEntities, finalTarget, capturedPlacer);

            // next-tick scan; iss may spawn with a 1-tick delay
            serverLevel.getServer().execute(() -> {
                if (level == null || level.isClientSide) return;
                trackNewEntities((ServerLevel) level, preCastEntities, finalTarget, capturedPlacer);
            });
        } finally {
            if (!keepAlive) proxy.discard();
            ACTIVE_PLACER.remove();
        }
    }

    private void applyHatSpellPowerBoost(ArmorStand proxy, AbstractSpell spell) {
        if (heldHat.isEmpty()) return;

        String hatItemPath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(heldHat.getItem()).getPath();

        // Tarnished Crown: -15% spell power, no school boost
        if ("tarnished_helmet".equals(hatItemPath)) {
            AttributeInstance generalAttr = proxy.getAttribute(AttributeRegistry.SPELL_POWER);
            if (generalAttr != null)
                generalAttr.addTransientModifier(new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(CreateWizardry.MOD_ID, "hat_spell_power_general"),
                    -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            return;
        }

        // 5% general boost for all spells whenever a hat is equipped
        AttributeInstance generalAttr = proxy.getAttribute(AttributeRegistry.SPELL_POWER);
        if (generalAttr != null)
            generalAttr.addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(CreateWizardry.MOD_ID, "hat_spell_power_general"),
                0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

        // Extra 10% when the hat's school matches the spell's school
        String hatSchool = HAT_TO_SCHOOL.get(hatItemPath);
        if (hatSchool == null) return;
        SchoolType spellSchool = spell.getSchoolType();
        if (spellSchool == null || !hatSchool.equals(spellSchool.getId().getPath())) return;
        Holder<Attribute> schoolAttr = getSchoolSpellPowerAttribute(hatSchool);
        if (schoolAttr == null) return;
        AttributeInstance schoolAttrInstance = proxy.getAttribute(schoolAttr);
        if (schoolAttrInstance == null) return;
        schoolAttrInstance.addTransientModifier(new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(CreateWizardry.MOD_ID, "hat_spell_power_school"),
            0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    // Superheat (Caster's Scone) grants a flat extra spell power boost on top of any hat bonus.
    private void applySuperheatBoost(ArmorStand proxy) {
        if (!isSuperheated()) return;
        AttributeInstance generalAttr = proxy.getAttribute(AttributeRegistry.SPELL_POWER);
        if (generalAttr != null)
            generalAttr.addTransientModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(CreateWizardry.MOD_ID, "superheat_spell_power"),
                CWConfig.blazeCasterSuperheatDamageBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    @Nullable
    private static Holder<Attribute> getSchoolSpellPowerAttribute(String schoolPath) {
        return switch (schoolPath) {
            case "fire"      -> AttributeRegistry.FIRE_SPELL_POWER;
            case "ice"       -> AttributeRegistry.ICE_SPELL_POWER;
            case "lightning" -> AttributeRegistry.LIGHTNING_SPELL_POWER;
            case "holy"      -> AttributeRegistry.HOLY_SPELL_POWER;
            case "ender"     -> AttributeRegistry.ENDER_SPELL_POWER;
            case "blood"     -> AttributeRegistry.BLOOD_SPELL_POWER;
            case "evocation" -> AttributeRegistry.EVOCATION_SPELL_POWER;
            case "nature"    -> AttributeRegistry.NATURE_SPELL_POWER;
            default          -> null;
        };
    }

    public void updateBlockState() {
        setBlockHeat(getHeatLevel());
    }

    private int getEffectiveTankCapacity() {
        if (heldHat.isEmpty()) return 4000;
        String hatPath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(heldHat.getItem()).getPath();
        return "tarnished_helmet".equals(hatPath) ? 5500 : 5250;
    }

    public void updateTankCapacity() {
        if (internalTank == null) return;
        IFluidHandler h = internalTank.getPrimaryHandler();
        if (h instanceof FluidTank ft) {
            ft.setCapacity(getEffectiveTankCapacity());
        }
    }

    protected void setBlockHeat(BlazeBurnerBlock.HeatLevel newHeat) {
        if (level == null) return;
        BlockState currentState = level.getBlockState(worldPosition);
        BlazeBurnerBlock.HeatLevel currentHeat = BlazeCasterBlock.getHeatLevelOf(currentState);
        if (currentHeat == newHeat)
            return;
        onHeatChange(currentHeat, newHeat);
        level.setBlockAndUpdate(worldPosition, currentState.setValue(BlazeCasterBlock.HEAT_LEVEL, newHeat));
        notifyUpdate();
    }

    protected void onHeatChange(BlazeBurnerBlock.HeatLevel currentHeat, BlazeBurnerBlock.HeatLevel newHeat) {}

    public BlazeBurnerBlock.HeatLevel getHeatLevelFromBlock() {
        if (level != null) return BlazeCasterBlock.getHeatLevelOf(level.getBlockState(worldPosition));
        return BlazeCasterBlock.getHeatLevelOf(getBlockState());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        internalTank = SmartFluidTankBehaviour.single(this, 4000)
                .allowInsertion()
                .allowExtraction()
                .whenFluidUpdates(() -> {
                    IFluidHandler h = internalTank.getPrimaryHandler();
                    FluidStack current = h.getFluidInTank(0);
                    if (!current.isEmpty() && current.getFluid().getFluidType() != CWFluidRegistry.MANA_TYPE.get())
                        h.drain(current.getAmount(), IFluidHandler.FluidAction.EXECUTE);
                    updateBlockState();
                });
        behaviours.add(internalTank);
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putBoolean("Creative", creative);
        compound.putBoolean("CreativeSuperheat", creativeSuperheat);
        if (!heldItem.isEmpty())
            compound.put("HeldItem", heldItem.save(registries));
        if (!heldHat.isEmpty())
            compound.put("HeldHat", heldHat.save(registries));
        compound.putInt("CastTicks", castTicksRemaining);
        compound.putInt("CooldownTicks", cooldownTicksRemaining);
        compound.putInt("SuperheatTicks", superheatTicksRemaining);
        // Channel state is sync-only (never persisted): on a mid-channel save/reload the proxy
        // is gone, so restoring channelTicksRemaining would leave the caster stuck "casting".
        if (clientPacket) {
            compound.putInt("ChannelTicks", channelTicksRemaining);
            compound.putInt("ChannelProxyId", channelProxy != null ? channelProxy.getId() : 0);
            compound.putString("ChannelSpell",
                    channelSpell != null ? channelSpell.getSpellResource().getPath() : "");
        }
        if (placerUuid != null)
            compound.putUUID("PlacerUuid", placerUuid);
        compound.putBoolean("WasPowered", wasPowered);
        compound.putBoolean("LockedHead", lockedHead);
        compound.putFloat("LockedYaw", lockedYaw);
        net.minecraft.nbt.ListTag summonList = new net.minecraft.nbt.ListTag();
        for (UUID id : trackedSummons) {
            net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
            entry.putUUID("UUID", id);
            summonList.add(entry);
        }
        compound.put("TrackedSummons", summonList);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        creative = compound.getBoolean("Creative");
        creativeSuperheat = compound.getBoolean("CreativeSuperheat");
        heldItem = compound.contains("HeldItem")
                ? ItemStack.parseOptional(registries, compound.getCompound("HeldItem"))
                : ItemStack.EMPTY;
        heldHat = compound.contains("HeldHat")
                ? ItemStack.parseOptional(registries, compound.getCompound("HeldHat"))
                : ItemStack.EMPTY;
        castTicksRemaining = compound.getInt("CastTicks");
        cooldownTicksRemaining = compound.getInt("CooldownTicks");
        superheatTicksRemaining = compound.getInt("SuperheatTicks");
        // channel state is sync-only, never read from disk on the server
        if (clientPacket) {
            channelTicksRemaining = compound.getInt("ChannelTicks");
            clientChannelProxyId = compound.getInt("ChannelProxyId");
            clientChannelSpellPath = compound.getString("ChannelSpell");
            updateClientBeam();
        }
        placerUuid = compound.hasUUID("PlacerUuid") ? compound.getUUID("PlacerUuid") : null;
        wasPowered = compound.getBoolean("WasPowered");
        lockedHead = compound.getBoolean("LockedHead");
        lockedYaw = compound.getFloat("LockedYaw");
        trackedSummons.clear();
        if (compound.contains("TrackedSummons")) {
            net.minecraft.nbt.ListTag summonList = compound.getList("TrackedSummons", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < summonList.size(); i++)
                trackedSummons.add(summonList.getCompound(i).getUUID("UUID"));
        }
        super.read(compound, registries, clientPacket);
        updateTankCapacity();
    }

    @Override
    public void destroy() {
        endChannel();
        despawnTrackedSummons();
        // Remove any remaining caster-spawned entities from the protection map
        if (placerUuid != null)
            SPAWNED_ENTITY_PLACER.values().removeIf(placer -> placer.equals(placerUuid));
        super.destroy();
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), heldItem);
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), heldHat);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        // Drop any beam this caster registered on the client (chunk unload mid-channel, etc.)
        if (level != null && level.isClientSide && beamProxyIdInSet != 0) {
            net.ttzplayz.create_wizardry.client.ClientBlazeBeams.remove(beamProxyIdInSet);
            beamProxyIdInSet = 0;
        }
    }

    // sync beam
    private void updateClientBeam() {
        boolean shouldBeam = clientChannelProxyId != 0 && "ray_of_siphoning".equals(clientChannelSpellPath);
        int desired = shouldBeam ? clientChannelProxyId : 0;
        if (desired == beamProxyIdInSet) return;
        if (beamProxyIdInSet != 0)
            net.ttzplayz.create_wizardry.client.ClientBlazeBeams.remove(beamProxyIdInSet);
        if (desired != 0)
            net.ttzplayz.create_wizardry.client.ClientBlazeBeams.put(desired);
        beamProxyIdInSet = desired;
    }

    private void appendHatBonusLines(List<Component> tooltip) {
        if (heldHat.isEmpty()) return;
        String hatPath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(heldHat.getItem()).getPath();
        if ("tarnished_helmet".equals(hatPath)) {
            tooltip.add(Component.translatable("create_wizardry.tooltip.hat.tarnished_penalty")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("create_wizardry.tooltip.hat.tarnished_mana")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("create_wizardry.tooltip.hat.mana_cost")
                    .withStyle(ChatFormatting.GREEN));
            return;
        }
        tooltip.add(Component.translatable("create_wizardry.tooltip.hat.spell_power_boost")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("create_wizardry.tooltip.hat.mana_boost")
                .withStyle(ChatFormatting.AQUA));
        String school = HAT_TO_SCHOOL.get(hatPath);
        if (school != null) {
            String schoolDisplay = school.substring(0, 1).toUpperCase() + school.substring(1);
            tooltip.add(Component.translatable("create_wizardry.tooltip.hat.school_power_boost",
                    schoolDisplay).withStyle(ChatFormatting.GREEN));
        }
    }

    private void trackNewEntities(ServerLevel sl, Set<UUID> preCastEntities,
                                   @Nullable LivingEntity target, @Nullable UUID placer) {
        sl.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                new AABB(worldPosition).inflate(64.0)).forEach(e -> {
            if (preCastEntities.contains(e.getUUID())) return;
            // Register in placer-protection map for ALL new entities
            if (placer != null) SPAWNED_ENTITY_PLACER.put(e.getUUID(), placer);
            // Track mobs as summons for despawn-on-break and retargeting
            if (e instanceof Mob mob) {
                trackedSummons.add(mob.getUUID());
                if (target != null) mob.setTarget(target);
            }
        });
    }

    private void despawnTrackedSummons() {
        if (!(level instanceof ServerLevel sl)) return;
        for (UUID id : trackedSummons) {
            net.minecraft.world.entity.Entity e = sl.getEntity(id);
            if (e instanceof LivingEntity le) le.kill();
            else if (e != null) e.discard();
            SPAWNED_ENTITY_PLACER.remove(id);
        }
        trackedSummons.clear();
    }

    private void retargetSummons(@Nullable LivingEntity target) {
        if (!(level instanceof ServerLevel sl)) return;
        if (target instanceof Player p && placerUuid != null && p.getUUID().equals(placerUuid))
            target = null;
        trackedSummons.removeIf(id -> {
            net.minecraft.world.entity.Entity e = sl.getEntity(id);
            return e == null || !e.isAlive();
        });
        final LivingEntity safeTarget = target;
        for (UUID id : trackedSummons) {
            net.minecraft.world.entity.Entity e = sl.getEntity(id);
            if (e instanceof Mob mob) mob.setTarget(safeTarget);
        }
    }

    protected void spawnParticles(BlazeBurnerBlock.HeatLevel heatLevel) {
        assert level != null;
        if (heatLevel == BlazeBurnerBlock.HeatLevel.NONE)
            return;

        RandomSource random = level.getRandom();
        Vec3 center = VecHelper.getCenterOf(worldPosition);

        if (random.nextInt(4) != 0)
            return;

        boolean empty = level.getBlockState(worldPosition.above())
                .getCollisionShape(level, worldPosition.above())
                .isEmpty();

        double yMotion = empty ? .0625f : random.nextDouble() * .0125f;
        Vec3 pos = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, .5f)
                        .multiply(1, .25f, 1)
                        .normalize()
                        .scale((empty ? .25f : .5) + random.nextDouble() * .125f))
                .add(0, .5, 0);

        // Particle type is keyed off element + superheat only (not casting state): a normal caster
        // always emits its element's rune, a superheated one always emits its element's special
        // particle — both passively, like smoke. ("none" / no scroll falls back to arcane runes.)
        String element = getElementId();
        if (isSuperheated()) {
            spawnSpecialElementParticle(element, pos, yMotion, random);
        } else {
            level.addParticle(CWParticles.runeFor(element), pos.x, pos.y, pos.z, 0, yMotion, 0);
        }
    }

    /** School-specific particle emitted by a superheated caster. Easily tweakable. */
    @OnlyIn(Dist.CLIENT)
    private void spawnSpecialElementParticle(String element, Vec3 pos, double yMotion, RandomSource random) {
        assert level != null;
        switch (element) {
            case "lightning" -> {
                // A real "zap" arc to a nearby random point.
                Vec3 dest = pos.add((random.nextDouble() - .5) * .6, (random.nextDouble() - .5) * .6,
                        (random.nextDouble() - .5) * .6);
                level.addParticle(new ZapParticleOption(dest), pos.x, pos.y, pos.z, 0, 0, 0);
            }
            case "ender" ->
                level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z,
                        (random.nextDouble() - .5), (random.nextDouble() - .5), (random.nextDouble() - .5));
            case "blood" ->
                level.addParticle(ParticleHelper.BLOOD, pos.x, pos.y, pos.z, 0, yMotion, 0);
            case "fire" ->
                level.addParticle(ParticleHelper.FIRE_EMITTER, pos.x, pos.y, pos.z, 0, yMotion, 0);
            case "ice" ->
                level.addParticle(ParticleHelper.SNOWFLAKE, pos.x, pos.y, pos.z, 0, yMotion, 0);
            case "holy" ->
                level.addParticle(ParticleHelper.CLEANSE_PARTICLE, pos.x, pos.y, pos.z, 0, yMotion, 0);
            case "nature" ->
                level.addParticle(ParticleHelper.FIREFLY, pos.x, pos.y, pos.z, 0, yMotion, 0);
            case "evocation" ->
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 0, yMotion, 0);
            default -> {
                ParticleOptions rune = CWParticles.runeFor(element);
                level.addParticle(rune, pos.x, pos.y, pos.z, 0, yMotion, 0);
            }
        }
    }

    protected void spawnParticleBurst(boolean soul) {
        assert level != null;
        Vec3 c = VecHelper.getCenterOf(worldPosition);
        RandomSource random = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, .5f)
                    .multiply(1, .25f, 1)
                    .normalize();
            Vec3 pos = c.add(offset.scale(.5 + random.nextDouble() * .125f)).add(0, .125, 0);
            Vec3 motion = offset.scale(1 / 32f);

            level.addParticle(soul ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                    pos.x, pos.y, pos.z,
                    motion.x, motion.y, motion.z);
        }
    }

    @Override
    public <T> boolean hasData(Supplier<AttachmentType<T>> type) {
        return super.hasData(type);
    }

    @Override
    public <T> T getData(Supplier<AttachmentType<T>> type) {
        return super.getData(type);
    }

    @Override
    public @Nullable <T> T setData(Supplier<AttachmentType<T>> type, T data) {
        return super.setData(type, data);
    }

    @Override
    public ModelData getModelData() {
        return super.getModelData();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CWBlockEntities.BLAZE_CASTER_BE.get(),
                (be, context) -> be.decayingCapability(context)
        );
    }

    // decaying cap
    private IFluidHandler decayingCapability(Direction side) {
        if (internalTank == null) return null;
        return decayingCaps.computeIfAbsent(side,
                s -> ManaPipeTransport.decaying(this, s, internalTank.getCapability()));
    }
}
