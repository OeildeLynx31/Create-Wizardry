package net.ttzplayz.create_wizardry.client;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class CWSpriteShifts {
    private static final ResourceLocation FLAME_BASE =
            ResourceLocation.fromNamespaceAndPath("create", "block/blaze_burner_flame");

    public static final SpriteShiftEntry NONE      = get("blaze_caster/scroll/blaze_caster_none_scroll");
    public static final SpriteShiftEntry FIRE      = get("blaze_caster/scroll/blaze_caster_fire_scroll");
    public static final SpriteShiftEntry LIGHTNING = get("blaze_caster/scroll/blaze_caster_lightning_scroll");
    public static final SpriteShiftEntry HOLY      = get("blaze_caster/scroll/blaze_caster_holy_scroll");
    public static final SpriteShiftEntry ENDER     = get("blaze_caster/scroll/blaze_caster_ender_scroll");
    public static final SpriteShiftEntry ICE       = get("blaze_caster/scroll/blaze_caster_ice_scroll");
    public static final SpriteShiftEntry BLOOD     = get("blaze_caster/scroll/blaze_caster_blood_scroll");
    public static final SpriteShiftEntry EVOCATION = get("blaze_caster/scroll/blaze_caster_evocation_scroll");
    public static final SpriteShiftEntry NATURE    = get("blaze_caster/scroll/blaze_caster_nature_scroll");

    public static final Map<String, SpriteShiftEntry> BY_ELEMENT = Map.of(
        "none",      NONE,
        "fire",      FIRE,
        "lightning", LIGHTNING,
        "holy",      HOLY,
        "ender",     ENDER,
        "ice",       ICE,
        "blood",     BLOOD,
        "evocation", EVOCATION,
        "nature",    NATURE
    );

    public static final CTSpriteShiftEntry ARCANE_CASING = CTSpriteShifter.getCT(
            AllCTTypes.OMNIDIRECTIONAL,
            ResourceLocation.fromNamespaceAndPath("create_wizardry", "block/arcane_casing_base"),
            ResourceLocation.fromNamespaceAndPath("create_wizardry", "block/arcane_casing_connected")
    );

    public static void register() {}

    private static SpriteShiftEntry get(String target) {
        return SpriteShifter.get(
                FLAME_BASE,
                ResourceLocation.fromNamespaceAndPath("create_wizardry", "block/" + target));
    }
}
