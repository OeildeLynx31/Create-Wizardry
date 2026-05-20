package net.ttzplayz.create_wizardry.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.ttzplayz.create_wizardry.CreateWizardry;

public class CWPartialModels {
    public static final PartialModel ELECTROMANCER_HAT = block("blaze_caster/electromancer_hat");
    public static final PartialModel ELECTROMANCER_HAT_SMALL = block("blaze_caster/electromancer_hat_small");
    public static final PartialModel BLAZE_CASTER_INERT = block("blaze_caster/blaze/inert");

    public static void register() {}

    private static PartialModel block(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateWizardry.MOD_ID, "block/" + path));
    }
}
