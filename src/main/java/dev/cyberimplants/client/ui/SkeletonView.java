package dev.cyberimplants.client.ui;

import dev.cyberimplants.body.SlotId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The Rig's body hologram: a wireframe skeleton inside the ghost of a Minecraft player model, in 3D, slowly swaying or
 * turned by dragging. Every body slot has an anchor on it (eyes for Ocular, the knees for Legs, beads along the spine for
 * deck sockets ...), projected to screen space so the screen can draw and hit-test slot nodes.
 *
 * <p>Model space: y up from the feet (0) to the top of the head (2), x towards the patient's left, z towards the viewer.
 * The proportions follow the player model (a pixel is 1/16 of a unit).
 */
public final class SkeletonView {
    /** A slot node after projection. {@code depth} is -1 (far) .. 1 (near). */
    public record Node(SlotId slot, float x, float y, float depth) {
    }

    private float cx;
    private float cy;
    private float scale;
    private float yaw;
    private float pitch = -0.12f;
    private float dragYaw;
    private boolean userTurned;
    private final Anim.Smooth turn = new Anim.Smooth(0, 6);

    // ---------------------------------------------------------------- anchors

    /** Where a slot sits on the body, in model space. */
    public static Vector3f anchor(SlotId slot) {
        int i = slot.index();
        return switch (slot.region()) {
            case CORE -> new Vector3f(0, 1.36f, 0.06f);
            case CORTEX -> new Vector3f(i == 0 ? 0.09f : -0.09f, 1.86f, -0.02f);
            case OCULAR -> new Vector3f(i == 0 ? 0.09f : -0.09f, 1.72f, 0.2f);
            case SPINE -> new Vector3f(0, 1.22f, -0.12f);
            case DECK -> new Vector3f(0, 1.46f - i * 0.075f, -0.2f);
            case SKELETON -> switch (i) {
                case 0 -> new Vector3f(-0.15f, 1.47f, 0.04f);
                case 1 -> new Vector3f(0, 0.8f, 0.06f);
                default -> new Vector3f(0.125f, 0.58f, 0.04f);
            };
            case LEFT_ARM -> new Vector3f(0.375f, 1.14f, 0);
            case RIGHT_ARM -> new Vector3f(-0.375f, 1.14f, 0);
            case LEFT_SOCKET -> new Vector3f(0.375f, 1.0f - i * 0.09f, 0.14f);
            case RIGHT_SOCKET -> new Vector3f(-0.375f, 1.0f - i * 0.09f, 0.14f);
            case CIRCULATORY -> switch (i) {
                case 0 -> new Vector3f(0.08f, 1.27f, 0.09f);
                case 1 -> new Vector3f(-0.08f, 1.02f, 0.09f);
                default -> new Vector3f(0.08f, 1.02f, 0.09f);
            };
            case INTEGUMENT -> new Vector3f(i == 0 ? 0.25f : -0.25f, 0.9f, 0.13f);
            case LEGS -> new Vector3f(i == 0 ? 0.125f : -0.125f, 0.4f, 0.12f);
        };
    }

    // ---------------------------------------------------------------- camera

    /** Places the hologram: {@code height} pixels from feet to head, centred on ({@code centerX}, {@code centerY}). */
    public void layout(float centerX, float centerY, float height) {
        this.cx = centerX;
        this.cy = centerY;
        this.scale = height / 2.1f;
    }

    /** Sways gently unless the player has turned it by hand. */
    public void update(float time) {
        float target = userTurned ? dragYaw : (Anim.enabled() ? 0.45f * Mth.sin(time * 0.35f) : 0.3f);
        yaw = turn.update(target);
    }

    public void drag(float dx) {
        if (!userTurned) {
            dragYaw = yaw;
            userTurned = true;
        }
        dragYaw += dx * 0.02f;
    }

    /** Back to the automatic sway. */
    public void resetTurn() {
        userTurned = false;
    }

    public float scale() {
        return scale;
    }

    /** Projects a model-space point to screen space; z of the result is the depth (-1 far .. 1 near). */
    public Vector3f project(float x, float y, float z) {
        float c = Mth.cos(yaw);
        float s = Mth.sin(yaw);
        float rx = x * c + z * s;
        float rz = -x * s + z * c;
        float cp = Mth.cos(pitch);
        float sp = Mth.sin(pitch);
        float ry = (y - 1.05f) * cp - rz * sp;
        float rz2 = (y - 1.05f) * sp + rz * cp;
        // A touch of perspective so near parts read as near.
        float persp = 1 + rz2 * 0.12f;
        return new Vector3f(cx + rx * scale * persp, cy - ry * scale * persp, Mth.clamp(rz2 / 0.45f, -1, 1));
    }

    public List<Node> nodes(List<SlotId> slots) {
        List<Node> out = new ArrayList<>(slots.size());
        for (SlotId slot : slots) {
            Vector3f a = anchor(slot);
            Vector3f p = project(a.x, a.y, a.z);
            out.add(new Node(slot, p.x, p.y, p.z));
        }
        return out;
    }

    // ---------------------------------------------------------------- drawing

    /**
     * Draws the ghost body and the skeleton. {@code reveal} (0..1) draws the hologram in from the feet up, for the boot
     * sequence; {@code scan} (0..1, or negative for none) is the height of a scanner sweep.
     */
    public void render(GuiGraphics g, int accent, int boneColor, float reveal, float scan, float time) {
        Draw.Batch b = Draw.batch(g);
        Vector3f floor = project(0, 0, 0);
        b.disc(floor.x, floor.y, scale * 0.3f, Anim.fade(accent, 0.14f), Anim.fade(accent, 0f));
        ellipse(b, 0, 0.002f, 0, 0.5f, 0.5f, Anim.fade(accent, 0.45f), 1f, reveal, 36);
        ellipse(b, 0, 0.002f, 0, 0.62f, 0.62f, Anim.fade(accent, 0.18f), 1f, reveal, 36);

        int ghost = Anim.fade(accent, 0.22f);
        // The Minecraft body: head, torso, arms and legs as boxes.
        box(b, -0.25f, 1.5f, -0.25f, 0.25f, 2.0f, 0.25f, ghost, reveal);
        box(b, -0.25f, 0.75f, -0.125f, 0.25f, 1.5f, 0.125f, ghost, reveal);
        box(b, 0.25f, 0.75f, -0.125f, 0.5f, 1.5f, 0.125f, ghost, reveal);
        box(b, -0.5f, 0.75f, -0.125f, -0.25f, 1.5f, 0.125f, ghost, reveal);
        box(b, 0f, 0f, -0.125f, 0.25f, 0.75f, 0.125f, ghost, reveal);
        box(b, -0.25f, 0f, -0.125f, 0f, 0.75f, 0.125f, ghost, reveal);

        float w = Math.max(1.2f, scale * 0.022f);
        // Skull: two loops, the eye sockets and the jaw.
        ellipseXY(b, 0, 1.76f, 0, 0.17f, 0.19f, boneColor, w, reveal);
        ellipseZY(b, 0, 1.76f, 0, 0.16f, 0.19f, Anim.fade(boneColor, 0.6f), w * 0.8f, reveal);
        bone(b, -0.1f, 1.6f, 0.12f, 0.1f, 1.6f, 0.12f, boneColor, w, reveal);
        bone(b, -0.1f, 1.6f, 0.12f, -0.06f, 1.55f, 0.08f, boneColor, w, reveal);
        bone(b, 0.1f, 1.6f, 0.12f, 0.06f, 1.55f, 0.08f, boneColor, w, reveal);
        // Spine: neck to tailbone with vertebra ticks.
        bone(b, 0, 1.56f, -0.04f, 0, 0.82f, -0.06f, boneColor, w, reveal);
        for (int v = 0; v < 12; v++) {
            float vy = 1.52f - v * 0.06f;
            bone(b, -0.03f, vy, -0.07f, 0.03f, vy, -0.07f, Anim.fade(boneColor, 0.7f), w * 0.8f, reveal);
        }
        // Collarbones, shoulder blades, ribs, sternum.
        bone(b, 0, 1.5f, 0.04f, 0.32f, 1.48f, 0, boneColor, w, reveal);
        bone(b, 0, 1.5f, 0.04f, -0.32f, 1.48f, 0, boneColor, w, reveal);
        bone(b, 0, 1.47f, 0.09f, 0, 1.16f, 0.1f, boneColor, w, reveal);
        for (int r = 0; r < 5; r++) {
            float ry = 1.43f - r * 0.065f;
            float rx = 0.17f + 0.012f * Math.min(r, 3);
            ellipse(b, 0, ry, 0, rx, 0.1f, Anim.fade(boneColor, 0.85f - r * 0.08f), w * 0.8f, reveal, 18);
        }
        // Pelvis.
        ellipse(b, 0, 0.84f, 0, 0.16f, 0.08f, boneColor, w, reveal, 18);
        bone(b, -0.16f, 0.84f, 0, -0.125f, 0.74f, 0, boneColor, w, reveal);
        bone(b, 0.16f, 0.84f, 0, 0.125f, 0.74f, 0, boneColor, w, reveal);
        // Arms: humerus, elbow, radius and ulna, hand.
        for (int side = -1; side <= 1; side += 2) {
            float ax = 0.375f * side;
            bone(b, 0.32f * side, 1.48f, 0, ax, 1.14f, 0, boneColor, w, reveal);
            joint(b, ax, 1.14f, 0, boneColor, w, reveal);
            bone(b, ax - 0.02f * side, 1.12f, 0.02f, ax - 0.02f * side, 0.83f, 0.03f, boneColor, w * 0.85f, reveal);
            bone(b, ax + 0.02f * side, 1.12f, -0.02f, ax + 0.02f * side, 0.83f, -0.01f, boneColor, w * 0.85f, reveal);
            ellipseXY(b, ax, 0.79f, 0.01f, 0.04f, 0.04f, boneColor, w * 0.8f, reveal);
            // Legs: femur, knee, tibia, foot.
            float lx = 0.125f * side;
            bone(b, lx, 0.74f, 0, lx, 0.42f, 0.02f, boneColor, w, reveal);
            joint(b, lx, 0.4f, 0.03f, boneColor, w, reveal);
            bone(b, lx - 0.015f * side, 0.38f, 0.02f, lx - 0.015f * side, 0.06f, 0, boneColor, w * 0.85f, reveal);
            bone(b, lx + 0.02f * side, 0.38f, 0, lx + 0.02f * side, 0.07f, -0.01f, boneColor, w * 0.7f, reveal);
            bone(b, lx, 0.05f, -0.02f, lx, 0.02f, 0.13f, boneColor, w, reveal);
        }
        if (scan >= 0) {
            float sy = scan * 2.05f;
            ellipse(b, 0, sy, 0, 0.42f, 0.32f, Anim.fade(0xFFFFFFFF, 0.8f), w, 1, 36);
            ellipse(b, 0, sy, 0, 0.46f, 0.36f, Anim.fade(accent, 0.5f), w * 2.5f, 1, 36);
        }
        b.end();
    }

    // ---------------------------------------------------------------- primitives in model space

    /** Depth-cued line: far parts fade. Hidden above the reveal height. */
    private void bone(Draw.Batch b, float x0, float y0, float z0, float x1, float y1, float z1, int color, float width, float reveal) {
        float limit = reveal * 2.05f;
        if (y0 > limit && y1 > limit) {
            return;
        }
        if (y0 > limit || y1 > limit) { // clip the segment at the reveal height
            float t = (limit - y0) / (y1 - y0);
            if (y0 > limit) {
                x0 = x0 + (x1 - x0) * t;
                z0 = z0 + (z1 - z0) * t;
                y0 = limit;
            } else {
                x1 = x0 + (x1 - x0) * t;
                z1 = z0 + (z1 - z0) * t;
                y1 = limit;
            }
        }
        Vector3f a = project(x0, y0, z0);
        Vector3f c = project(x1, y1, z1);
        b.line(a.x, a.y, c.x, c.y, width, depthFade(color, a.z), depthFade(color, c.z));
    }

    private void joint(Draw.Batch b, float x, float y, float z, int color, float width, float reveal) {
        if (y > reveal * 2.05f) {
            return;
        }
        Vector3f p = project(x, y, z);
        b.ring(p.x, p.y, Math.max(1.5f, scale * 0.03f), width * 0.8f, depthFade(color, p.z));
    }

    private void box(Draw.Batch b, float x0, float y0, float z0, float x1, float y1, float z1, int color, float reveal) {
        float[][] c = {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}, {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            float[] p = c[e[0]];
            float[] q = c[e[1]];
            bone(b, p[0], p[1], p[2], q[0], q[1], q[2], color, 1f, reveal);
        }
    }

    /** A horizontal ellipse (in the xz plane) at height y. */
    private void ellipse(Draw.Batch b, float x, float y, float z, float rx, float rz, int color, float width, float reveal, int segments) {
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.PI * 2 * i / segments;
            float a1 = Mth.PI * 2 * (i + 1) / segments;
            bone(b, x + Mth.cos(a0) * rx, y, z + Mth.sin(a0) * rz, x + Mth.cos(a1) * rx, y, z + Mth.sin(a1) * rz, color, width, reveal);
        }
    }

    /** An upright ellipse facing the viewer (xy plane). */
    private void ellipseXY(Draw.Batch b, float x, float y, float z, float rx, float ry, int color, float width, float reveal) {
        for (int i = 0; i < 20; i++) {
            float a0 = Mth.PI * 2 * i / 20;
            float a1 = Mth.PI * 2 * (i + 1) / 20;
            bone(b, x + Mth.cos(a0) * rx, y + Mth.sin(a0) * ry, z, x + Mth.cos(a1) * rx, y + Mth.sin(a1) * ry, z, color, width, reveal);
        }
    }

    /** An upright ellipse seen side-on (zy plane). */
    private void ellipseZY(Draw.Batch b, float x, float y, float z, float rz, float ry, int color, float width, float reveal) {
        for (int i = 0; i < 20; i++) {
            float a0 = Mth.PI * 2 * i / 20;
            float a1 = Mth.PI * 2 * (i + 1) / 20;
            bone(b, x, y + Mth.sin(a0) * ry, z + Mth.cos(a0) * rz, x, y + Mth.sin(a1) * ry, z + Mth.cos(a1) * rz, color, width, reveal);
        }
    }

    private static int depthFade(int color, float depth) {
        return Anim.fade(color, 0.45f + 0.55f * (depth + 1) / 2);
    }
}
