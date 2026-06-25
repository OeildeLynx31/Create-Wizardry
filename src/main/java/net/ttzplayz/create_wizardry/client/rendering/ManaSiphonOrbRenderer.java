package net.ttzplayz.create_wizardry.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ttzplayz.create_wizardry.block.mana_siphon.ManaSiphonBlock;
import net.ttzplayz.create_wizardry.block.mana_siphon.ManaSiphonBlockEntity;
import net.ttzplayz.create_wizardry.client.ClientManaSiphons;
import net.ttzplayz.create_wizardry.fluids.CWFluidRegistry;
import org.joml.Vector3f;

import java.util.List;

// draws the floating mana orb on top of each loaded siphon, sized by stored mana
public final class ManaSiphonOrbRenderer {

    private static final RenderType ORB_TYPE = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    // distance from block center along the facing direction that the orb floats
    private static final float ORB_OFFSET = 0.75f;
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    private ManaSiphonOrbRenderer() {}

    public static void renderAll(PoseStack ps, Camera camera, float partialTick) {
        List<BlockPos> positions = ClientManaSiphons.snapshot();
        if (positions.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        TextureAtlasSprite sprite = mc.getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(CWFluidRegistry.MANA_TEXTURE);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(ORB_TYPE);
        Vec3 cam = camera.getPosition();

        for (BlockPos pos : positions) {
            if (!(level.getBlockEntity(pos) instanceof ManaSiphonBlockEntity be)) {
                ClientManaSiphons.remove(pos); // stale, pump unloaded
                continue;
            }
            int mana = be.storedMana();
            if (mana <= 0) continue; // hidden when empty
            renderOrb(ps, vc, be, pos, mana, cam, partialTick, sprite);
        }
        buffers.endBatch(ORB_TYPE);
    }

    private static void renderOrb(PoseStack ps, VertexConsumer vc, ManaSiphonBlockEntity be, BlockPos pos,
                                  int mana, Vec3 cam, float partialTick, TextureAtlasSprite sprite) {
        int edgePx = Mth.clamp((mana + 124) / 125, 1, 8); // ceil(mana/125), 1px..8px
        float frac = edgePx / 8f;
        float half = (edgePx / 16f) / 2f;
        float spin = Mth.lerp(partialTick, be.prevOrbSpin, be.orbSpin);

        Direction facing = be.getBlockState().getValue(ManaSiphonBlock.FACING);
        ps.pushPose();
        ps.translate(pos.getX() - cam.x + 0.5 + facing.getStepX() * ORB_OFFSET,
                pos.getY() - cam.y + 0.5 + facing.getStepY() * ORB_OFFSET,
                pos.getZ() - cam.z + 0.5 + facing.getStepZ() * ORB_OFFSET);
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        ps.mulPose(Axis.XP.rotationDegrees(spin * 0.66f));
        ps.mulPose(Axis.ZP.rotationDegrees(spin * 0.37f));

        // expand the sampled sub-rect of each animated frame as the orb grows
        float du = sprite.getU1() - sprite.getU0();
        float dv = sprite.getV1() - sprite.getV0();
        float inset = (1f - frac) / 2f;
        float u0 = sprite.getU0() + du * inset;
        float u1 = sprite.getU1() - du * inset;
        float v0 = sprite.getV0() + dv * inset;
        float v1 = sprite.getV1() - dv * inset;

        PoseStack.Pose pose = ps.last();
        float h = half;
        // +Y
        quad(pose, vc, new Vector3f(-h, h, -h), new Vector3f(-h, h, h), new Vector3f(h, h, h), new Vector3f(h, h, -h), 0, 1, 0, u0, v0, u1, v1);
        // -Y
        quad(pose, vc, new Vector3f(-h, -h, h), new Vector3f(-h, -h, -h), new Vector3f(h, -h, -h), new Vector3f(h, -h, h), 0, -1, 0, u0, v0, u1, v1);
        // +Z
        quad(pose, vc, new Vector3f(-h, -h, h), new Vector3f(h, -h, h), new Vector3f(h, h, h), new Vector3f(-h, h, h), 0, 0, 1, u0, v0, u1, v1);
        // -Z
        quad(pose, vc, new Vector3f(h, -h, -h), new Vector3f(-h, -h, -h), new Vector3f(-h, h, -h), new Vector3f(h, h, -h), 0, 0, -1, u0, v0, u1, v1);
        // +X
        quad(pose, vc, new Vector3f(h, -h, h), new Vector3f(h, -h, -h), new Vector3f(h, h, -h), new Vector3f(h, h, h), 1, 0, 0, u0, v0, u1, v1);
        // -X
        quad(pose, vc, new Vector3f(-h, -h, -h), new Vector3f(-h, -h, h), new Vector3f(-h, h, h), new Vector3f(-h, h, -h), -1, 0, 0, u0, v0, u1, v1);

        ps.popPose();
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer vc, Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                             float nx, float ny, float nz, float u0, float v0, float u1, float v1) {
        vertex(pose, vc, a, u0, v0, nx, ny, nz);
        vertex(pose, vc, b, u1, v0, nx, ny, nz);
        vertex(pose, vc, c, u1, v1, nx, ny, nz);
        vertex(pose, vc, d, u0, v1, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, Vector3f p, float u, float v,
                               float nx, float ny, float nz) {
        vc.addVertex(pose, p.x, p.y, p.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }
}
