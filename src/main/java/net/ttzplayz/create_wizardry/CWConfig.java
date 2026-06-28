package net.ttzplayz.create_wizardry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;


@EventBusSubscriber(modid = CreateWizardry.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CWConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Mana Siphon
    private static final ModConfigSpec.IntValue MANA_SIPHON_SMALL_RADIUS;
    private static final ModConfigSpec.IntValue MANA_SIPHON_LARGE_RADIUS;
    private static final ModConfigSpec.IntValue MANA_SIPHON_DRAIN_PER_OP;
    private static final ModConfigSpec.DoubleValue MANA_SIPHON_SPELL_MANA_PER_DAMAGE;
    private static final ModConfigSpec.DoubleValue MANA_SIPHON_TRANSFORMATION_DAMAGE_PERCENT;
    private static final ModConfigSpec.IntValue PLAYER_MANA_PER_MB;
    private static final ModConfigSpec.BooleanValue MANA_SIPHON_DROP_KEY_ITEMS;
    private static final ModConfigSpec.BooleanValue MANA_SIPHON_DRAIN_ITEMS;
    private static final ModConfigSpec.DoubleValue MANA_SIPHON_SCROLL_DRAIN_PERCENT;
    private static final ModConfigSpec.DoubleValue MANA_SIPHON_ITEM_DRAIN_PERCENT;
    private static final ModConfigSpec.IntValue MANA_SIPHON_TRANSFORM_DELAY_TICKS;
    private static final ModConfigSpec.BooleanValue MANA_SIPHON_SPECIAL_INGOT_DROPS;
    private static final ModConfigSpec.DoubleValue MANA_SIPHON_BRASS_DROP_CHANCE;
    private static final ModConfigSpec.DoubleValue MANA_SIPHON_RARE_DROP_CHANCE;
    private static final ModConfigSpec.BooleanValue MANA_SIPHON_REQUIRES_ROTATION;

    // Mana Pipe Leaking
    private static final ModConfigSpec.DoubleValue MANA_PIPE_LOSS_RATE;
    private static final ModConfigSpec.BooleanValue MANA_LEAKING_ENABLED;

    // Channeler
    private static final ModConfigSpec.IntValue CHANNELER_CREEPER_RANGE;
    private static final ModConfigSpec.IntValue CHANNELER_LIGHTNING_RANGE;

    // Player Mana
    private static final ModConfigSpec.BooleanValue MANA_DEPLETION_ENABLED;

    // Blaze Caster
    private static final ModConfigSpec.IntValue BLAZE_CASTER_SUPERHEAT_DURATION;
    private static final ModConfigSpec.DoubleValue BLAZE_CASTER_SUPERHEAT_COOLDOWN_MULT;
    private static final ModConfigSpec.DoubleValue BLAZE_CASTER_SUPERHEAT_DAMAGE_BONUS;

    static {
        BUILDER.push("mana_siphon");

        MANA_SIPHON_SMALL_RADIUS = BUILDER
                .comment("Block radius the Mana Siphon drains within in its confined mode (1 = a 3x3 area).")
                .defineInRange("manaSiphonSmallRadius", 1, 1, 32);

        MANA_SIPHON_LARGE_RADIUS = BUILDER
                .comment("Block radius the Mana Siphon drains within when expanded with a wrench (3 = a 7x7 area).")
                .defineInRange("manaSiphonLargeRadius", 3, 1, 32);

        MANA_SIPHON_DRAIN_PER_OP = BUILDER
                .comment("Mana (mB) the Mana Siphon pulls from each entity per drain operation.")
                .defineInRange("manaSiphonDrainPerOp", 1, 1, 1000);

        MANA_SIPHON_SPELL_MANA_PER_DAMAGE = BUILDER
                .comment("Mana (mB) the Mana Siphon banks per 1 point of damage of a spell projectile",
                        "pulled in and consumed within its radius (each absorption banks at least 1 mB).")
                .defineInRange("manaSiphonSpellManaPerDamage", 1.25, 0.0, 1000.0);

        MANA_SIPHON_TRANSFORMATION_DAMAGE_PERCENT = BUILDER
                .comment("Damage dealt to a mob when the Mana Siphon reverts it (fraction of the reverted",
                        "mob's max health, dealt once on transformation; 0.25 = 25%). Only applies when the",
                        "siphonTransformationDamage gamerule is enabled.")
                .defineInRange("manaSiphonTransformationDamagePercent", 0.25, 0.0, 1.0);

        PLAYER_MANA_PER_MB = BUILDER
                .comment("Player mana spent per 1 mB the Mana Siphon stores (only applies to players).")
                .defineInRange("playerManaPerMb", 5, 1, 1000);

        MANA_SIPHON_DROP_KEY_ITEMS = BUILDER
                .comment("Whether mobs drop their key item (e.g. tarnished crown, spell book, permafrost shard)",
                        "when the Mana Siphon fully drains and reverts them.")
                .define("manaSiphonDropKeyItems", true);

        MANA_SIPHON_DRAIN_ITEMS = BUILDER
                .comment("Whether the Mana Siphon sucks mana out of dropped items within its radius",
                        "(spell scrolls, arcane ingots/sheets, and magic cloth).")
                .define("manaSiphonDrainItems", true);

        MANA_SIPHON_SCROLL_DRAIN_PERCENT = BUILDER
                .comment("Fraction of a spell scroll's mana cost banked (as mB) when the Siphon drains it.",
                        "0.5 = 50% of the spell's mana cost. The scroll is consumed.")
                .defineInRange("manaSiphonScrollDrainPercent", 0.5, 0.0, 1.0);

        MANA_SIPHON_ITEM_DRAIN_PERCENT = BUILDER
                .comment("Fraction of an arcane material's craft mana (mB) banked when the Siphon drains it.",
                        "0.25 = 25% of the ~500 mB it costs to make an arcane ingot/sheet/magic cloth.",
                        "Arcane ingots/sheets transmute into mundane ingots/sheets; magic cloth becomes wool.")
                .defineInRange("manaSiphonItemDrainPercent", 0.25, 0.0, 1.0);

        MANA_SIPHON_TRANSFORM_DELAY_TICKS = BUILDER
                .comment("Ticks an item must sit in the Mana Siphon's radius (with a rune-line",
                        "tethered to it) before it transforms and banks its mana. 100 ticks = 5s;",
                        "0 = instant. Applies to scrolls, arcane ingots/sheets, and magic cloth.")
                .defineInRange("manaSiphonTransformDelayTicks", 100, 0, 1200);

        MANA_SIPHON_SPECIAL_INGOT_DROPS = BUILDER
                .comment("Whether draining arcane ingots/sheets can yield the rare Brass and",
                        "Netherite/Sturdy Sheet results. When false, only Gold/Iron/Copper variants drop.")
                .define("manaSiphonSpecialIngotDrops", true);

        MANA_SIPHON_BRASS_DROP_CHANCE = BUILDER
                .comment("Chance an arcane ingot/sheet transmutes into Brass (only when special drops are enabled).")
                .defineInRange("manaSiphonBrassDropChance", 0.05, 0.0, 1.0);

        MANA_SIPHON_RARE_DROP_CHANCE = BUILDER
                .comment("Chance an arcane ingot transmutes into a Netherite Ingot, or an arcane sheet into a",
                        "Sturdy Sheet (only when special drops are enabled).")
                .defineInRange("manaSiphonRareDropChance", 0.01, 0.0, 1.0);

        MANA_SIPHON_REQUIRES_ROTATION = BUILDER
                .comment("Whether the Mana Siphon needs rotational power to function. When false, the",
                        "Siphon drains, absorbs spells, and pumps mana even with no rotation (still applies",
                        "stress if connected to a kinetic network).")
                .define("manaSiphonRequiresRotation", true);

        BUILDER.pop();

        BUILDER.push("mana_pipes");

        MANA_LEAKING_ENABLED = BUILDER
                .comment("Whether Mana leaks out of uninsulated (non-arcane) pipes and pumps as it travels.",
                        "When false, ordinary Create pipes carry Mana losslessly and no leak particles spawn.")
                .define("manaLeakingEnabled", true);

        MANA_PIPE_LOSS_RATE = BUILDER
                .comment("Per-block Mana loss rate of uninsulated pipes. The fraction surviving b blocks is e^(-rate*b),",
                        "so 0.05 leaves ~37% after 20 blocks. Only used when manaLeakingEnabled is true.")
                .defineInRange("manaPipeLossRate", 0.05, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.push("channeler");

        CHANNELER_CREEPER_RANGE = BUILDER
                .comment("Block radius within which the Channeler drains nearby charged creepers.")
                .defineInRange("channelerCreeperRange", 2, 1, 32);

        CHANNELER_LIGHTNING_RANGE = BUILDER
                .comment("Block radius within which the Channeler captures lightning bolts.")
                .defineInRange("channelerLightningRange", 8, 1, 64);

        BUILDER.pop();

        BUILDER.push("player_mana");

        MANA_DEPLETION_ENABLED = BUILDER
                .comment("Whether running out of mana inflicts the Depletion effect (slowness and slowed",
                        "mana regen until mana recovers). When false, low mana applies no penalty.")
                .define("manaDepletionEnabled", true);

        BUILDER.pop();

        BUILDER.push("blaze_caster");

        BLAZE_CASTER_SUPERHEAT_DURATION = BUILDER
                .comment("How long (in ticks) a Blaze Caster stays superheated after being fed a",
                        "Caster's Scone. 3600 ticks = 3 minutes. Feeding another scone refreshes the timer.")
                .defineInRange("blazeCasterSuperheatDuration", 3600, 100, 72000);

        BLAZE_CASTER_SUPERHEAT_COOLDOWN_MULT = BUILDER
                .comment("Spell cooldown multiplier while a Blaze Caster is superheated.",
                        "0.5 = half the normal cooldown.")
                .defineInRange("blazeCasterSuperheatCooldownMult", 0.5, 0.0, 1.0);

        BLAZE_CASTER_SUPERHEAT_DAMAGE_BONUS = BUILDER
                .comment("Extra spell power granted while a Blaze Caster is superheated, applied as an",
                        "added fraction of base spell power. 0.10 = +10% damage.")
                .defineInRange("blazeCasterSuperheatDamageBonus", 0.10, 0.0, 10.0);

        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int manaSiphonSmallRadius;
    public static int manaSiphonLargeRadius;
    public static int manaSiphonDrainPerOp;
    public static double manaSiphonSpellManaPerDamage;
    public static double manaSiphonTransformationDamagePercent;
    public static int playerManaPerMb;
    public static boolean manaSiphonDropKeyItems;
    public static boolean manaSiphonDrainItems;
    public static double manaSiphonScrollDrainPercent;
    public static double manaSiphonItemDrainPercent;
    public static int manaSiphonTransformDelayTicks;
    public static boolean manaSiphonSpecialIngotDrops;
    public static double manaSiphonBrassDropChance;
    public static double manaSiphonRareDropChance;
    public static boolean manaSiphonRequiresRotation;

    public static double manaPipeLossRate;
    public static boolean manaLeakingEnabled;

    public static int channelerCreeperRange;
    public static int channelerLightningRange;

    public static boolean manaDepletionEnabled;

    public static int blazeCasterSuperheatDuration;
    public static double blazeCasterSuperheatCooldownMult;
    public static double blazeCasterSuperheatDamageBonus;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        manaSiphonSmallRadius = MANA_SIPHON_SMALL_RADIUS.get();
        manaSiphonLargeRadius = MANA_SIPHON_LARGE_RADIUS.get();
        manaSiphonDrainPerOp = MANA_SIPHON_DRAIN_PER_OP.get();
        manaSiphonSpellManaPerDamage = MANA_SIPHON_SPELL_MANA_PER_DAMAGE.get();
        manaSiphonTransformationDamagePercent = MANA_SIPHON_TRANSFORMATION_DAMAGE_PERCENT.get();
        playerManaPerMb = PLAYER_MANA_PER_MB.get();
        manaSiphonDropKeyItems = MANA_SIPHON_DROP_KEY_ITEMS.get();
        manaSiphonDrainItems = MANA_SIPHON_DRAIN_ITEMS.get();
        manaSiphonScrollDrainPercent = MANA_SIPHON_SCROLL_DRAIN_PERCENT.get();
        manaSiphonItemDrainPercent = MANA_SIPHON_ITEM_DRAIN_PERCENT.get();
        manaSiphonTransformDelayTicks = MANA_SIPHON_TRANSFORM_DELAY_TICKS.get();
        manaSiphonSpecialIngotDrops = MANA_SIPHON_SPECIAL_INGOT_DROPS.get();
        manaSiphonBrassDropChance = MANA_SIPHON_BRASS_DROP_CHANCE.get();
        manaSiphonRareDropChance = MANA_SIPHON_RARE_DROP_CHANCE.get();
        manaSiphonRequiresRotation = MANA_SIPHON_REQUIRES_ROTATION.get();

        manaPipeLossRate = MANA_PIPE_LOSS_RATE.get();
        manaLeakingEnabled = MANA_LEAKING_ENABLED.get();

        channelerCreeperRange = CHANNELER_CREEPER_RANGE.get();
        channelerLightningRange = CHANNELER_LIGHTNING_RANGE.get();

        manaDepletionEnabled = MANA_DEPLETION_ENABLED.get();

        blazeCasterSuperheatDuration = BLAZE_CASTER_SUPERHEAT_DURATION.get();
        blazeCasterSuperheatCooldownMult = BLAZE_CASTER_SUPERHEAT_COOLDOWN_MULT.get();
        blazeCasterSuperheatDamageBonus = BLAZE_CASTER_SUPERHEAT_DAMAGE_BONUS.get();
    }
}
