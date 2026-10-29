package dev.cyberimplants.client.ui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Vector drawing on top of {@link GuiGraphics}: angled lines, rings, arcs and soft glows, as coloured quads in the GUI
 * render type. Open a {@link Batch} with {@link #batch(GuiGraphics)}, draw, then {@link Batch#end()} so the shapes are
 * flushed before any text or item drawn after them. Colours are ARGB.
 */
public final class Draw {
    public static Batch batch(GuiGraphics g) {
        return new Batch(g);
    }

    public static final class Batch {
        private final GuiGraphics g;
        private final Matrix4f matrix;
        private final VertexConsumer vc;

        private Batch(GuiGraphics g) {
            this.g = g;
            this.matrix = g.pose().last().pose();
            this.vc = g.bufferSource().getBuffer(RenderType.gui());
        }

        /** Any quad, drawn from both sides (the GUI render type culls back faces). */
        public Batch quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                          int c0, int c1, int c2, int c3) {
            vc.addVertex(matrix, x0, y0, 0).setColor(c0);
            vc.addVertex(matrix, x1, y1, 0).setColor(c1);
            vc.addVertex(matrix, x2, y2, 0).setColor(c2);
            vc.addVertex(matrix, x3, y3, 0).setColor(c3);
            vc.addVertex(matrix, x3, y3, 0).setColor(c3);
            vc.addVertex(matrix, x2, y2, 0).setColor(c2);
            vc.addVertex(matrix, x1, y1, 0).setColor(c1);
            vc.addVertex(matrix, x0, y0, 0).setColor(c0);
            return this;
        }

        public Batch rect(float x0, float y0, float x1, float y1, int color) {
            return quad(x0, y0, x0, y1, x1, y1, x1, y0, color, color, color, color);
        }

        /** A line of the given width with a colour gradient from {@code c0} to {@code c1}. */
        public Batch line(float x0, float y0, float x1, float y1, float width, int c0, int c1) {
            float dx = x1 - x0;
            float dy = y1 - y0;
            float len = Mth.sqrt(dx * dx + dy * dy);
            if (len < 1e-4f) {
                return this;
            }
            float nx = -dy / len * width / 2;
            float ny = dx / len * width / 2;
            return quad(x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1 + nx, y1 + ny, c0, c0, c1, c1);
        }

        public Batch line(float x0, float y0, float x1, float y1, float width, int color) {
            return line(x0, y0, x1, y1, width, color, color);
        }

        /** A dashed line: {@code dash} on, {@code gap} off, starting {@code offset} along it (animate it to march). */
        public Batch dashed(float x0, float y0, float x1, float y1, float width, float dash, float gap, float offset, int color) {
            float dx = x1 - x0;
            float dy = y1 - y0;
            float len = Mth.sqrt(dx * dx + dy * dy);
            if (len < 1e-4f) {
                return this;
            }
            float period = dash + gap;
            for (float s = -(offset % period); s < len; s += period) {
                float a = Math.max(0, s);
                float b = Math.min(len, s + dash);
                if (b > a) {
                    line(x0 + dx * a / len, y0 + dy * a / len, x0 + dx * b / len, y0 + dy * b / len, width, color);
                }
            }
            return this;
        }

        /** An arc of a ring: {@code start} and {@code sweep} in radians, 0 = right, clockwise on screen. */
        public Batch arc(float cx, float cy, float radius, float thickness, float start, float sweep, int color) {
            return arc(cx, cy, radius, thickness, start, sweep, color, color);
        }

        public Batch arc(float cx, float cy, float radius, float thickness, float start, float sweep, int inner, int outer) {
            int segments = Math.max(3, (int) Math.ceil(Math.abs(sweep) * radius / 3f));
            float r0 = radius - thickness / 2;
            float r1 = radius + thickness / 2;
            for (int i = 0; i < segments; i++) {
                float a0 = start + sweep * i / segments;
                float a1 = start + sweep * (i + 1) / segments;
                float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                quad(cx + c0 * r0, cy + s0 * r0, cx + c0 * r1, cy + s0 * r1, cx + c1 * r1, cy + s1 * r1, cx + c1 * r0, cy + s1 * r0,
                        inner, outer, outer, inner);
            }
            return this;
        }

        public Batch ring(float cx, float cy, float radius, float thickness, int color) {
            return arc(cx, cy, radius, thickness, 0, Mth.PI * 2, color);
        }

        /** A filled disc that fades from {@code center} to {@code edge}. */
        public Batch disc(float cx, float cy, float radius, int center, int edge) {
            int segments = Math.max(8, (int) (radius * 1.5f));
            for (int i = 0; i < segments; i++) {
                float a0 = Mth.PI * 2 * i / segments;
                float a1 = Mth.PI * 2 * (i + 1) / segments;
                quad(cx, cy, cx + Mth.cos(a0) * radius, cy + Mth.sin(a0) * radius,
                        cx + Mth.cos(a1) * radius, cy + Mth.sin(a1) * radius, cx, cy, center, edge, edge, center);
            }
            return this;
        }

        /** A soft neon glow: a bright core fading to transparent. */
        public Batch glow(float cx, float cy, float radius, int color) {
            disc(cx, cy, radius, Anim.fade(color, 0.55f), color & 0x00FFFFFF);
            return disc(cx, cy, radius * 0.35f, Anim.fade(color, 0.9f), Anim.fade(color, 0.4f));
        }

        /** A regular polygon outline, e.g. a hexagon for slot nodes. */
        public Batch polygon(float cx, float cy, float radius, int sides, float rotation, float width, int color) {
            for (int i = 0; i < sides; i++) {
                float a0 = rotation + Mth.PI * 2 * i / sides;
                float a1 = rotation + Mth.PI * 2 * (i + 1) / sides;
                line(cx + Mth.cos(a0) * radius, cy + Mth.sin(a0) * radius, cx + Mth.cos(a1) * radius, cy + Mth.sin(a1) * radius, width, color);
            }
            return this;
        }

        public Batch filledPolygon(float cx, float cy, float radius, int sides, float rotation, int color) {
            for (int i = 0; i < sides; i++) {
                float a0 = rotation + Mth.PI * 2 * i / sides;
                float a1 = rotation + Mth.PI * 2 * (i + 1) / sides;
                quad(cx, cy, cx + Mth.cos(a0) * radius, cy + Mth.sin(a0) * radius,
                        cx + Mth.cos(a1) * radius, cy + Mth.sin(a1) * radius, cx, cy, color, color, color, color);
            }
            return this;
        }

        /** Draws everything so far, so later text and items land on top. */
        public void end() {
            g.flush();
        }
    }

    private Draw() {
    }
}
