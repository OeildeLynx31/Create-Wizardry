package net.ttzplayz.create_wizardry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Create Wizardry common config. Currently all entries configure the Mana Siphon.
 */
@EventBusSubscriber(modid = CreateWizardry.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---- Mana Siphon ----
    private static final ModConfigSpec.IntValue MANA_SIPHON_SMALL_RADIUS;
    private static final ModConfigSpec.IntValue MANA_SIPHON_LARGE_RADIUS;
    private static final ModConfigSpec.IntValue MANA_SIPHON_DRAIN_PER_OP;
    private static final ModConfigSpec.IntValue PLAYER_MANA_PER_MB;

    static {
        BUILDER.push("mana_siphon");

        MANA_SIPHON_SMALL_RADIUS = BUILDER
                .comment("Block radius the Mana Siphon drains within in its small mode (1 = a 3x3 area).")
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
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int manaSiphonSmallRadius;
    public static int manaSiphonLargeRadius;
    public static int manaSiphonDrainPerOp;
    public static int playerManaPerMb;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        manaSiphonSmallRadius = MANA_SIPHON_SMALL_RADIUS.get();
        manaSiphonLargeRadius = MANA_SIPHON_LARGE_RADIUS.get();
        manaSiphonDrainPerOp = MANA_SIPHON_DRAIN_PER_OP.get();
        playerManaPerMb = PLAYER_MANA_PER_MB.get();
    }
}
