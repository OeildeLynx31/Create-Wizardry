package net.ttzplayz.create_wizardry;

import net.minecraft.world.level.GameRules;

public final class CWGameRules {

    private CWGameRules() {}

    // toggles the Mana Siphon dealing damage when it reverts a mob; default off
    public static GameRules.Key<GameRules.BooleanValue> SIPHON_TRANSFORMATION_DAMAGE;

    public static void register() {
        SIPHON_TRANSFORMATION_DAMAGE = GameRules.register(
                "siphonTransformationDamage",
                GameRules.Category.MOBS,
                GameRules.BooleanValue.create(false));
    }
}
