package net.ttzplayz.create_wizardry.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.ttzplayz.create_wizardry.CreateWizardry;

import java.util.Map;

public class CWPartialModels {
    public static final PartialModel ELECTROMANCER_HAT = block("blaze_caster/electromancer_hat");
    public static final PartialModel ELECTROMANCER_HAT_SMALL = block("blaze_caster/electromancer_hat_small");
    public static final PartialModel BLAZE_CASTER_INERT = block("blaze_caster/blaze/inert");

    public static final PartialModel BLAZE_CASTER_NONE      = block("blaze_caster/blaze/none");
    public static final PartialModel BLAZE_CASTER_FIRE      = block("blaze_caster/blaze/fire");
    public static final PartialModel BLAZE_CASTER_LIGHTNING = block("blaze_caster/blaze/lightning");
    public static final PartialModel BLAZE_CASTER_HOLY      = block("blaze_caster/blaze/holy");
    public static final PartialModel BLAZE_CASTER_ENDER     = block("blaze_caster/blaze/ender");
    public static final PartialModel BLAZE_CASTER_ICE       = block("blaze_caster/blaze/ice");
    public static final PartialModel BLAZE_CASTER_BLOOD     = block("blaze_caster/blaze/blood");
    public static final PartialModel BLAZE_CASTER_EVOCATION = block("blaze_caster/blaze/evocation");
    public static final PartialModel BLAZE_CASTER_EYES      = block("blaze_caster/blaze/eyes");

    public static final Map<String, PartialModel> BLAZE_BY_ELEMENT = Map.of(
        "none",      BLAZE_CASTER_NONE,
        "fire",      BLAZE_CASTER_FIRE,
        "lightning", BLAZE_CASTER_LIGHTNING,
        "holy",      BLAZE_CASTER_HOLY,
        "ender",     BLAZE_CASTER_ENDER,
        "ice",       BLAZE_CASTER_ICE,
        "blood",     BLAZE_CASTER_BLOOD,
        "evocation", BLAZE_CASTER_EVOCATION
    );

    public static void register() {}

    private static PartialModel block(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateWizardry.MOD_ID, "block/" + path));
    }
}
