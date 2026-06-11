package net.ttzplayz.create_wizardry.client.pipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes.ComponentPartials;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Arcane copy of Create's {@code PipeAttachmentModel}, dynamically adding connection rims, casings, drains and
 * brackets using {@link ArcanePartialModels} so the geometry matches the arcane pipe textures.
 */
public class ArcanePipeAttachmentModel extends BakedModelWrapperWithData {

    private static final ModelProperty<PipeModelData> PIPE_PROPERTY = new ModelProperty<>();
    private final boolean ao;

    public static ArcanePipeAttachmentModel withAO(BakedModel template) {
        return new ArcanePipeAttachmentModel(template, true);
    }

    public ArcanePipeAttachmentModel(BakedModel template, boolean ao) {
        super(template);
        this.ao = ao;
    }

    @Override
    protected ModelData.Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos,
        BlockState state, ModelData blockEntityData) {
        PipeModelData data = new PipeModelData();
        FluidTransportBehaviour transport = BlockEntityBehaviour.get(world, pos, FluidTransportBehaviour.TYPE);
        BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);

        if (transport != null)
            for (Direction d : Iterate.directions)
                data.putAttachment(d, transport.getRenderedRimAttachment(world, pos, state, d));
        if (bracket != null)
            data.putBracket(bracket.getBracket());

        data.setEncased(FluidPipeBlock.shouldDrawCasing(world, pos, state));
        return builder.with(PIPE_PROPERTY, data);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand,
        @NotNull ModelData data) {
        List<ChunkRenderTypeSet> set = new ArrayList<>();

        set.add(super.getRenderTypes(state, rand, data));
        set.add(ArcanePartialModels.FLUID_PIPE_CASING.get().getRenderTypes(state, rand, data));

        if (data.has(PIPE_PROPERTY)) {
            PipeModelData pipeData = data.get(PIPE_PROPERTY);
            for (Direction d : Iterate.directions) {
                AttachmentTypes type = pipeData.getAttachment(d);
                for (ComponentPartials partial : type.partials) {
                    set.add(ArcanePartialModels.PIPE_ATTACHMENTS.get(partial).get(d).get()
                        .getRenderTypes(state, rand, data));
                }
            }
        }

        return ChunkRenderTypeSet.union(set);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
        RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();
        // Only emit the base model's quads into the render layers it actually declares. For the glass
        // window (cutout_mipped) this keeps the transparent tube out of the solid pass, which would
        // otherwise draw its see-through pixels as opaque black. Opaque pipes only declare solid, so
        // this guard is a no-op for them.
        if (super.getRenderTypes(state, rand, data).contains(renderType))
            quads.addAll(super.getQuads(state, side, rand, data, renderType));
        if (data.has(PIPE_PROPERTY)) {
            PipeModelData pipeData = data.get(PIPE_PROPERTY);
            addQuads(quads, state, side, rand, data, pipeData, renderType);
        }
        return quads;
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        return ao ? TriState.TRUE : TriState.FALSE;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return ao;
    }

    private void addQuads(List<BakedQuad> quads, BlockState state, Direction side, RandomSource rand, ModelData data,
        PipeModelData pipeData, RenderType renderType) {
        BakedModel bracket = pipeData.getBracket();
        if (bracket != null)
            addModelQuads(quads, bracket, state, side, rand, data, renderType);
        for (Direction d : Iterate.directions) {
            AttachmentTypes type = pipeData.getAttachment(d);
            for (ComponentPartials partial : type.partials) {
                addModelQuads(quads, ArcanePartialModels.PIPE_ATTACHMENTS.get(partial).get(d).get(),
                    state, side, rand, data, renderType);
            }
        }
        if (pipeData.isEncased())
            addModelQuads(quads, ArcanePartialModels.FLUID_PIPE_CASING.get(), state, side, rand, data, renderType);
    }

    /** Adds a sub-model's quads only for the render layers it declares, so e.g. solid rims never bleed
     *  into the cutout pass (and the cutout glass never bleeds into the solid pass). */
    private static void addModelQuads(List<BakedQuad> quads, BakedModel model, BlockState state, Direction side,
        RandomSource rand, ModelData data, RenderType renderType) {
        if (model.getRenderTypes(state, rand, data).contains(renderType))
            quads.addAll(model.getQuads(state, side, rand, data, renderType));
    }

    private static class PipeModelData {
        private final AttachmentTypes[] attachments;
        private boolean encased;
        private BakedModel bracket;

        public PipeModelData() {
            attachments = new AttachmentTypes[6];
            Arrays.fill(attachments, AttachmentTypes.NONE);
        }

        public void putBracket(BlockState state) {
            if (state != null)
                this.bracket = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        }

        public BakedModel getBracket() {
            return bracket;
        }

        public void putAttachment(Direction face, AttachmentTypes rim) {
            attachments[face.get3DDataValue()] = rim;
        }

        public AttachmentTypes getAttachment(Direction face) {
            return attachments[face.get3DDataValue()];
        }

        public void setEncased(boolean encased) {
            this.encased = encased;
        }

        public boolean isEncased() {
            return encased;
        }
    }
}
