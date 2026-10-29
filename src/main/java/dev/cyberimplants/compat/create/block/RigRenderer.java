package dev.cyberimplants.compat.create.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.cyberimplants.config.ClientConfig;
import dev.cyberimplants.content.RigSeatEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the Implantation Rig's moving parts: the shaft half going into the gearbox, a faint ready ring over the cushion
 * while the Rig turns, and during an operation a scanner ring that sweeps the patient, laser beams from the four corner
 * emitters and a progress arc around the base. The holograms use the additive lightning render type.
 */
public class RigRenderer extends SafeBlockEntityRenderer<RigBlockEntity> {
    private static final int CYAN = 0x19D9FF;
    private static final int MAGENTA = 0xFF2BD6;
    private static final int GREEN = 0x39FF88;
    private static final float CUSHION = (float) RigSeatEntity.SEAT_HEIGHT;
    /** Laser emitters on the corners of the seat pan, in block coordinates (identical under every rotation). */
    private static final float[][] EMITTERS = {{1.5f / 16, 14f / 16, 1.5f / 16}, {14.5f / 16, 14f / 16, 1.5f / 16},
            {1.5f / 16, 14f / 16, 14.5f / 16}, {14.5f / 16, 14f / 16, 14.5f / 16}};

    public RigRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(RigBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        KineticBlockEntityRenderer.renderRotatingBuffer(be,
                CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, facing.getOpposite()),
                ms, buffer.getBuffer(RenderType.solid()), light);

        RigView view = be.view();
        if (!view.powered() || be.getLevel() == null) {
            return;
        }
        float time = be.getLevel().getGameTime() + partialTicks;
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = ms.last();
        RigView.Operation op = view.operation();
        boolean calm = ClientConfig.REDUCE_SCREEN_EFFECTS.get();
        if (op == null) {
            float pulse = 0.35f + 0.15f * Mth.sin(time * 0.12f);
            ring(vc, pose, CUSHION + 0.04f, 0.012f, 0.47f, 0f, 1f, CYAN, pulse * 0.6f, time * 0.02f, 24);
            return;
        }

        float progress = op.progress(partialTicks);
        int color = lerpColor(CYAN, MAGENTA, progress);
        // The scanner sweeps from the cushion to above the patient's head and back.
        float sweep = calm ? 0.5f : 0.5f + 0.5f * Mth.sin(time * 0.09f);
        float ringY = CUSHION + 0.1f + sweep * 1.7f;
        ring(vc, pose, ringY, 0.035f, 0.52f, 0f, 1f, color, 0.55f, time * 0.05f, 32);
        ring(vc, pose, ringY + 0.06f, 0.008f, 0.55f, 0f, 1f, color, 0.35f, -time * 0.05f, 32);
        for (float[] e : EMITTERS) {
            Vec3 from = new Vec3(e[0], e[1], e[2]);
            Vec3 dir = new Vec3(e[0] - 0.5, 0, e[2] - 0.5).normalize().scale(0.52);
            Vec3 to = new Vec3(0.5 + dir.x, ringY, 0.5 + dir.z);
            float flicker = calm ? 0.4f : 0.3f + 0.2f * Mth.sin(time * 0.7f + e[0] * 9 + e[2] * 5);
            beam(vc, pose, from, to, 0.012f, color, flicker);
        }
        // Progress arc around the base, plus a full dim track under it.
        ring(vc, pose, 0.02f, 0.02f, 0.62f, 0f, 1f, CYAN, 0.12f, 0f, 48);
        ring(vc, pose, 0.025f, 0.03f, 0.62f, 0f, progress, GREEN, 0.6f, 0f, 48);
    }

    /**
     * A vertical band around the Rig's centre: a ring of radius {@code r} from {@code y} to {@code y + h}, covering the
     * fraction {@code from}..{@code to} of the circle, turned by {@code spin} turns.
     */
    private static void ring(VertexConsumer vc, PoseStack.Pose pose, float y, float h, float r, float from, float to,
                             int rgb, float alpha, float spin, int segments) {
        int count = Math.max(1, Math.round(segments * (to - from)));
        for (int i = 0; i < count; i++) {
            float a0 = (float) ((from + (to - from) * i / count + spin) * Math.PI * 2);
            float a1 = (float) ((from + (to - from) * (i + 1) / count + spin) * Math.PI * 2);
            float x0 = 0.5f + r * Mth.cos(a0), z0 = 0.5f + r * Mth.sin(a0);
            float x1 = 0.5f + r * Mth.cos(a1), z1 = 0.5f + r * Mth.sin(a1);
            quad(vc, pose, x0, y, z0, x1, y, z1, x1, y + h, z1, x0, y + h, z0, rgb, alpha);
        }
    }

    /** A thin glowing beam: two crossed quads along the line. */
    private static void beam(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, float width, int rgb, float alpha) {
        Vec3 d = b.subtract(a);
        Vec3 side = d.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-6) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize().scale(width);
        Vec3 up = d.cross(side).normalize().scale(width);
        for (Vec3 o : new Vec3[]{side, up}) {
            quad(vc, pose, (float) (a.x - o.x), (float) (a.y - o.y), (float) (a.z - o.z), (float) (a.x + o.x), (float) (a.y + o.y), (float) (a.z + o.z),
                    (float) (b.x + o.x), (float) (b.y + o.y), (float) (b.z + o.z), (float) (b.x - o.x), (float) (b.y - o.y), (float) (b.z - o.z), rgb, alpha);
        }
    }

    /** A double-sided quad, so the hologram reads from inside the ring too. */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255), 0, 255);
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        vc.addVertex(pose, x0, y0, z0).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        vc.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
        vc.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
        vc.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        vc.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        vc.addVertex(pose, x0, y0, z0).setColor(r, g, b, a);
    }

    private static int lerpColor(int from, int to, float t) {
        int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return (r << 16) | (g << 8) | b;
    }
}
