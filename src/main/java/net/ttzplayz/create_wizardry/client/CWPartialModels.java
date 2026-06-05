package net.ttzplayz.create_wizardry.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.ttzplayz.create_wizardry.CreateWizardry;

import java.util.Map;

public class CWPartialModels {
    public static final PartialModel ISS_ELECTROMANCER_HAT   = block("blaze_caster/hats/electromancer");
    public static final PartialModel ISS_ARCHEVOKER_HAT        = block("blaze_caster/hats/archevoker");
    public static final PartialModel ISS_CRYOMANCER_HAT        = block("blaze_caster/hats/cryomancer");
    public static final PartialModel ISS_CULTIST_HAT           = block("blaze_caster/hats/cultist");
    public static final PartialModel ISS_PLAGUED_HAT           = block("blaze_caster/hats/plagued");
    public static final PartialModel ISS_PRIEST_HAT            = block("blaze_caster/hats/priest");
    public static final PartialModel ISS_PYROMANCER_HAT        = block("blaze_caster/hats/pyromancer");
    public static final PartialModel ISS_SHADOWWALKER_HAT      = block("blaze_caster/hats/shadowwalker");
    public static final PartialModel ISS_TARNISHED_HAT         = block("blaze_caster/hats/tarnished");
    public static final PartialModel ISS_WANDERING_MAGICIAN_HAT= block("blaze_caster/hats/wandering_magician");
    public static final PartialModel ISS_PUMPKIN_HAT           = block("blaze_caster/hats/pumpkin");
    public static final PartialModel ISS_WIZARD_HAT            = block("blaze_caster/hats/wizard");
    public static final PartialModel ISS_WIZARD_HOOD           = block("blaze_caster/hats/wizard_hood");
    public static final PartialModel ISS_NETHERITE_MAGE_HAT      = block("blaze_caster/hats/netherite_mage");

    public static final PartialModel BLAZE_CASTER_INERT = block("blaze_caster/blaze/inert");
    public static final PartialModel BLAZE_CASTER_NONE      = block("blaze_caster/blaze/none");
    public static final PartialModel BLAZE_CASTER_FIRE      = block("blaze_caster/blaze/fire");
    public static final PartialModel BLAZE_CASTER_LIGHTNING = block("blaze_caster/blaze/lightning");
    public static final PartialModel BLAZE_CASTER_HOLY      = block("blaze_caster/blaze/holy");
    public static final PartialModel BLAZE_CASTER_ENDER     = block("blaze_caster/blaze/ender");
    public static final PartialModel BLAZE_CASTER_ICE       = block("blaze_caster/blaze/ice");
    public static final PartialModel BLAZE_CASTER_BLOOD     = block("blaze_caster/blaze/blood");
    public static final PartialModel BLAZE_CASTER_EVOCATION = block("blaze_caster/blaze/evocation");
    public static final PartialModel BLAZE_CASTER_NATURE    = block("blaze_caster/blaze/nature");
    public static final PartialModel BLAZE_CASTER_IDLE_EYES   = block("blaze_caster/blaze/idle_eyes");
    public static final PartialModel BLAZE_CASTER_ACTIVE_EYES = block("blaze_caster/blaze/active_eyes");

    public static final PartialModel ROD_SMALL_NONE      = block("blaze_caster/rods/none_small");
    public static final PartialModel ROD_SMALL_FIRE      = block("blaze_caster/rods/fire_small");
    public static final PartialModel ROD_SMALL_LIGHTNING = block("blaze_caster/rods/lightning_small");
    public static final PartialModel ROD_SMALL_HOLY      = block("blaze_caster/rods/holy_small");
    public static final PartialModel ROD_SMALL_ENDER     = block("blaze_caster/rods/ender_small");
    public static final PartialModel ROD_SMALL_ICE       = block("blaze_caster/rods/ice_small");
    public static final PartialModel ROD_SMALL_BLOOD     = block("blaze_caster/rods/blood_small");
    public static final PartialModel ROD_SMALL_EVOCATION = block("blaze_caster/rods/evocation_small");
    public static final PartialModel ROD_SMALL_NATURE    = block("blaze_caster/rods/nature_small");

    public static final PartialModel ROD_LARGE_NONE      = block("blaze_caster/rods/none_large");
    public static final PartialModel ROD_LARGE_FIRE      = block("blaze_caster/rods/fire_large");
    public static final PartialModel ROD_LARGE_LIGHTNING = block("blaze_caster/rods/lightning_large");
    public static final PartialModel ROD_LARGE_HOLY      = block("blaze_caster/rods/holy_large");
    public static final PartialModel ROD_LARGE_ENDER     = block("blaze_caster/rods/ender_large");
    public static final PartialModel ROD_LARGE_ICE       = block("blaze_caster/rods/ice_large");
    public static final PartialModel ROD_LARGE_BLOOD     = block("blaze_caster/rods/blood_large");
    public static final PartialModel ROD_LARGE_EVOCATION = block("blaze_caster/rods/evocation_large");
    public static final PartialModel ROD_LARGE_NATURE    = block("blaze_caster/rods/nature_large");

    public static final Map<String, PartialModel> BLAZE_BY_ELEMENT = Map.of(
        "none",      BLAZE_CASTER_NONE,
        "fire",      BLAZE_CASTER_FIRE,
        "lightning", BLAZE_CASTER_LIGHTNING,
        "holy",      BLAZE_CASTER_HOLY,
        "ender",     BLAZE_CASTER_ENDER,
        "ice",       BLAZE_CASTER_ICE,
        "blood",     BLAZE_CASTER_BLOOD,
        "evocation", BLAZE_CASTER_EVOCATION,
        "nature",    BLAZE_CASTER_NATURE
    );

    public static final Map<String, PartialModel> ROD_SMALL_BY_ELEMENT = Map.of(
        "none",      ROD_SMALL_NONE,
        "fire",      ROD_SMALL_FIRE,
        "lightning", ROD_SMALL_LIGHTNING,
        "holy",      ROD_SMALL_HOLY,
        "ender",     ROD_SMALL_ENDER,
        "ice",       ROD_SMALL_ICE,
        "blood",     ROD_SMALL_BLOOD,
        "evocation", ROD_SMALL_EVOCATION,
        "nature",    ROD_SMALL_NATURE
    );

    public static final Map<String, PartialModel> ROD_LARGE_BY_ELEMENT = Map.of(
        "none",      ROD_LARGE_NONE,
        "fire",      ROD_LARGE_FIRE,
        "lightning", ROD_LARGE_LIGHTNING,
        "holy",      ROD_LARGE_HOLY,
        "ender",     ROD_LARGE_ENDER,
        "ice",       ROD_LARGE_ICE,
        "blood",     ROD_LARGE_BLOOD,
        "evocation", ROD_LARGE_EVOCATION,
        "nature",    ROD_LARGE_NATURE
    );

    public static final Map<String, PartialModel> HAT_BY_ITEM = Map.ofEntries(
        Map.entry("electromancer_helmet",    ISS_ELECTROMANCER_HAT),
        Map.entry("archevoker_helmet",       ISS_ARCHEVOKER_HAT),
        Map.entry("cryomancer_helmet",       ISS_CRYOMANCER_HAT),
        Map.entry("cultist_helmet",          ISS_CULTIST_HAT),
        Map.entry("plagued_helmet",          ISS_PLAGUED_HAT),
        Map.entry("priest_helmet",           ISS_PRIEST_HAT),
        Map.entry("pyromancer_helmet",       ISS_PYROMANCER_HAT),
        Map.entry("shadowwalker_helmet",     ISS_SHADOWWALKER_HAT),
        Map.entry("tarnished_helmet",        ISS_TARNISHED_HAT),
        Map.entry("wandering_magician_helmet", ISS_WANDERING_MAGICIAN_HAT),
        Map.entry("pumpkin_helmet",          ISS_PUMPKIN_HAT),
        Map.entry("netherite_mage_helmet",   ISS_NETHERITE_MAGE_HAT)
    );

    public static void register() {}

    private static PartialModel block(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateWizardry.MOD_ID, "block/" + path));
    }
}
