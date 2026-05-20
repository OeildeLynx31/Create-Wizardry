package net.ttzplayz.create_wizardry.client.rendering;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.apache.commons.lang3.ArrayUtils;

public interface PartialModelBlockEntityRenderer {
    RenderType[] REVERSED_CHUNK_BUFFER_LAYERS = Util.make(
            RenderType.chunkBufferLayers().toArray(RenderType[]::new),
            ArrayUtils::reverse);

    default RenderType getRenderType(BlockState blockState, PartialModel model) {
        ChunkRenderTypeSet types = model.get().getRenderTypes(blockState, RandomSource.create(42L), ModelData.EMPTY);
        for (RenderType type : REVERSED_CHUNK_BUFFER_LAYERS)
            if (types.contains(type))
                return type;
        return RenderType.cutoutMipped();
    }
}
