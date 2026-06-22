package net.ttzplayz.create_wizardry.client.pipe;

import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes.ComponentPartials;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.ttzplayz.create_wizardry.CreateWizardry;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// arcane pipe partials
public class ArcanePartialModels {

    public static final PartialModel FLUID_PIPE_CASING = block("arcane_pipe/casing");

    public static final Map<ComponentPartials, Map<Direction, PartialModel>> PIPE_ATTACHMENTS =
            new EnumMap<>(ComponentPartials.class);

    static {
        for (ComponentPartials type : ComponentPartials.values()) {
            Map<Direction, PartialModel> map = new HashMap<>();
            for (Direction d : Iterate.directions) {
                String asId = type.name().toLowerCase(Locale.ROOT);
                map.put(d, block("arcane_pipe/" + asId + "/" + d.getSerializedName()));
            }
            PIPE_ATTACHMENTS.put(type, map);
        }
    }

    // force classload
    public static void register() {}

    private static PartialModel block(String path) {
        return PartialModel.of(CreateWizardry.id("block/" + path));
    }
}
