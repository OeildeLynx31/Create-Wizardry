package net.ttzplayz.create_wizardry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;


@EventBusSubscriber(modid = CreateWizardry.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Mana Siphon
    private static final ModConfigSpec.IntValue MANA_SIPHON_SMALL_RADIUS;
    private static final ModConfigSpec.IntValue MANA_SIPHON_LARGE_RADIUS;
    private static final ModConfigSpec.IntValue MANA_SIPHON_DRAIN_PER_OP;
    private static final ModConfigSpec.IntValue PLAYER_MANA_PER_MB;

    // Mana Pipe Leaking
    private static final ModConfigSpec.DoubleValue MANA_PIPE_LOSS_RATE;
    private static final ModConfigSpec.BooleanValue MANA_LEAKING_ENABLED;

    // Channeler
    private static final ModConfigSpec.IntValue CHANNELER_CREEPER_RANGE;
    private static final ModConfigSpec.IntValue CHANNELER_LIGHTNING_RANGE;

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
                .defineInRange("manaSiphonDrainPerOp", 5, 1, 1000);

        PLAYER_MANA_PER_MB = BUILDER
                .comment("Player mana spent per 1 mB the Mana Siphon stores (only applies to players).")
                .defineInRange("playerManaPerMb", 5, 1, 1000);

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
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int manaSiphonSmallRadius;
    public static int manaSiphonLargeRadius;
    public static int manaSiphonDrainPerOp;
    public static int playerManaPerMb;

    public static double manaPipeLossRate;
    public static boolean manaLeakingEnabled;

    public static int channelerCreeperRange;
    public static int channelerLightningRange;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        manaSiphonSmallRadius = MANA_SIPHON_SMALL_RADIUS.get();
        manaSiphonLargeRadius = MANA_SIPHON_LARGE_RADIUS.get();
        manaSiphonDrainPerOp = MANA_SIPHON_DRAIN_PER_OP.get();
        playerManaPerMb = PLAYER_MANA_PER_MB.get();

        manaPipeLossRate = MANA_PIPE_LOSS_RATE.get();
        manaLeakingEnabled = MANA_LEAKING_ENABLED.get();

        channelerCreeperRange = CHANNELER_CREEPER_RANGE.get();
        channelerLightningRange = CHANNELER_LIGHTNING_RANGE.get();
    }
}
