package dev.cyberimplants.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Map;

/**
 * A small looping animation that shows what an implant does: a slow-time bubble for the Sandevistan, a radar sweep for
 * the Entity Scanner, an ECG trace for the Biomonitor, and so on. Implants share a dozen scenes; the tier makes the
 * effect visibly bigger or faster. Pure vector drawing, no textures.
 */
public final class EffectPreview {
    private enum Scene {
        TIME_FIELD, DODGE, PREDICT, RADAR, THERMAL, CAMO, CODE, REACTOR, VITALS, DEFIB, SHIELD, SHOCK, JUMP, DASH, WALL_WALK,
        STRIKE, BLADE, PROJECTILE, REACH
    }

    private static final Map<String, Scene> SCENES = Map.ofEntries(
            Map.entry("cybernetic_core", Scene.REACTOR), Map.entry("energy_buffer", Scene.REACTOR),
            Map.entry("sandevistan", Scene.TIME_FIELD), Map.entry("kerenzikov", Scene.DODGE),
            Map.entry("automatic_evasion_system", Scene.DODGE), Map.entry("reflex_booster", Scene.PREDICT),
            Map.entry("combat_predictor", Scene.PREDICT), Map.entry("deck_chassis", Scene.CODE),
            Map.entry("deck_processor", Scene.CODE), Map.entry("deck_ram", Scene.CODE), Map.entry("deck_storage", Scene.CODE),
            Map.entry("deck_coprocessor", Scene.CODE), Map.entry("deck_machine_interface", Scene.CODE),
            Map.entry("reinforced_skeleton", Scene.SHIELD), Map.entry("power_actuators", Scene.STRIKE),
            Map.entry("kinetic_tendons", Scene.JUMP), Map.entry("micro_actuators", Scene.DASH),
            Map.entry("impact_dampeners", Scene.SHIELD), Map.entry("inertia_compensation", Scene.STRIKE),
            Map.entry("cybernetic_arm", Scene.STRIKE), Map.entry("remote_manipulator", Scene.REACH),
            Map.entry("retractable_blade", Scene.BLADE), Map.entry("microgrip", Scene.WALL_WALK),
            Map.entry("ballistic_module", Scene.PROJECTILE), Map.entry("entity_scanner", Scene.RADAR),
            Map.entry("thermal_vision", Scene.THERMAL), Map.entry("machine_analyzer", Scene.RADAR),
            Map.entry("ballistic_visor", Scene.PREDICT), Map.entry("optical_camouflage", Scene.CAMO),
            Map.entry("target_tracking", Scene.PREDICT), Map.entry("biomonitor", Scene.VITALS),
            Map.entry("regenerator", Scene.VITALS), Map.entry("adrenaline_pump", Scene.VITALS),
            Map.entry("emergency_defibrillator", Scene.DEFIB), Map.entry("emergency_response_system", Scene.DEFIB),
            Map.entry("armor_plating", Scene.SHIELD), Map.entry("nanocoating", Scene.SHIELD),
            Map.entry("shock_circuit", Scene.SHOCK), Map.entry("kinetic_shield", Scene.SHIELD),
            Map.entry("emergency_shell", Scene.SHIELD), Map.entry("enhanced_tendons", Scene.JUMP),
            Map.entry("hydraulic_legs", Scene.JUMP), Map.entry("silent_actuators", Scene.DASH),
            Map.entry("magnetic_soles", Scene.WALL_WALK), Map.entry("inertial_stabilizer", Scene.JUMP),
            Map.entry("momentum_booster", Scene.DASH));

    /**
     * Draws the preview of {@code implant} at {@code tier} into the box. {@code accent} is the implant's category colour.
     */
    public static void render(GuiGraphics g, int x, int y, int w, int h, ResourceLocation implant, int tier, int accent) {
        g.fill(x, y, x + w, y + h, 0xC0050810);
        g.enableScissor(x, y, x + w, y + h);
        float t = Anim.enabled() ? Anim.now() : 1.3f;
        float power = 0.55f + 0.09f * Mth.clamp(tier, 1, 5); // 0.64 .. 1.0
        Scene scene = SCENES.getOrDefault(implant.getPath(), Scene.REACTOR);
        Draw.Batch b = Draw.batch(g);
        grid(b, x, y, w, h, accent);
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        float ground = y + h - 6;
        switch (scene) {
            case TIME_FIELD -> timeField(b, cx, ground, h, t, power, accent);
            case DODGE -> dodge(b, x, w, ground, h, t, power, accent);
            case PREDICT -> predict(b, x, y, w, h, t, power, accent);
            case RADAR -> radar(b, cx, cy, Math.min(w, h) / 2f - 4, t, power, accent);
            case THERMAL -> thermal(b, x, y, w, h, ground, t, power);
            case CAMO -> camo(b, cx, ground, h, t, power, accent);
            case CODE -> code(b, x, y, w, h, t, power, accent);
            case REACTOR -> reactor(b, cx, cy, Math.min(w, h) / 2f - 4, t, power, accent);
            case VITALS -> vitals(b, x, y, w, h, t, power, accent, false);
            case DEFIB -> vitals(b, x, y, w, h, t, power, accent, true);
            case SHIELD -> shield(b, cx, ground, h, t, power, accent);
            case SHOCK -> shock(b, cx, ground, h, t, power, accent);
            case JUMP -> jump(b, x, w, ground, h, t, power, accent);
            case DASH -> dash(b, x, w, ground, h, t, power, accent);
            case WALL_WALK -> wallWalk(b, x, y, w, ground, h, t, power, accent);
            case STRIKE -> strike(b, cx, ground, h, t, power, accent, false);
            case BLADE -> strike(b, cx, ground, h, t, power, accent, true);
            case PROJECTILE -> projectile(b, x, w, ground, h, t, power, accent);
            case REACH -> reach(b, x, w, ground, h, t, power, accent);
        }
        b.end();
        g.disableScissor();
        // Scanline sheen and a frame on top.
        int sweep = (int) ((t * 22) % (h + 12)) - 6;
        g.fillGradient(x, y + Math.max(0, sweep - 6), x + w, y + Math.min(h, sweep), 0x00FFFFFF, 0x18FFFFFF);
        g.renderOutline(x, y, w, h, Anim.fade(accent, 0.45f));
    }

    // ---------------------------------------------------------------- shared bits

    private static void grid(Draw.Batch b, int x, int y, int w, int h, int accent) {
        int c = Anim.fade(accent, 0.08f);
        for (int gx = x + 8; gx < x + w; gx += 8) {
            b.rect(gx, y, gx + 1, y + h, c);
        }
        for (int gy = y + 8; gy < y + h; gy += 8) {
            b.rect(x, gy, x + w, gy + 1, c);
        }
        b.rect(x, y + h - 6, x + w, y + h - 5, Anim.fade(accent, 0.35f));
    }

    /** A stick figure standing on {@code ground}, {@code size} tall, leaning by {@code lean} (-1..1). */
    private static void figure(Draw.Batch b, float fx, float ground, float size, float lean, int color) {
        float head = size * 0.16f;
        float hip = ground - size * 0.45f;
        float shoulder = ground - size * 0.78f;
        float sx = fx + lean * size * 0.15f;
        b.line(fx - size * 0.13f, ground, fx, hip, 1.5f, color);
        b.line(fx + size * 0.13f, ground, fx, hip, 1.5f, color);
        b.line(fx, hip, sx, shoulder, 1.5f, color);
        b.line(sx, shoulder + 1, sx - size * 0.16f, hip + size * 0.05f, 1.2f, color);
        b.line(sx, shoulder + 1, sx + size * 0.16f, hip + size * 0.05f, 1.2f, color);
        b.ring(sx, shoulder - head, head, 1.3f, color);
    }

    private static float frac(float v) {
        return v - Mth.floor(v);
    }

    // ---------------------------------------------------------------- scenes

    private static void timeField(Draw.Batch b, float cx, float ground, int h, float t, float power, int accent) {
        float r = h * 0.42f * power;
        float cy = ground - h * 0.38f;
        float breathe = 1 + 0.04f * Mth.sin(t * 3);
        b.disc(cx, cy, r * breathe, Anim.fade(accent, 0.12f), Anim.fade(accent, 0.32f));
        b.ring(cx, cy, r * breathe, 1.2f, Anim.fade(accent, 0.9f));
        b.arc(cx, cy, r * 0.55f, 1f, t * 0.4f, Mth.PI * 1.2f, Anim.fade(accent, 0.6f));
        figure(b, cx, ground, h * 0.5f, 0, 0xFFE6F4FF);
        // Bullets crawl inside the field and race outside it.
        for (int i = 0; i < 4; i++) {
            float lane = cy - r * 0.6f + i * r * 0.4f;
            float speedOut = 90;
            float p = frac(t * 0.35f + i * 0.27f);
            float px = cx - h * 1.2f + p * h * 2.4f;
            boolean inside = Math.abs(px - cx) < r;
            if (inside) {
                px = cx - r + frac(t * 0.05f + i * 0.31f) * 2 * r;
            }
            float tail = inside ? 3 : speedOut * 0.08f;
            b.line(px - tail, lane, px, lane, 1.4f, Anim.fade(0xFFFFB830, 0.1f), 0xFFFFB830);
        }
    }

    private static void dodge(Draw.Batch b, int x, int w, float ground, int h, float t, float power, int accent) {
        float cycle = frac(t * 0.5f);
        float cx = x + w * 0.6f;
        float shift = cycle > 0.45f ? Math.min(1, (cycle - 0.45f) / 0.08f) : 0;
        float figX = cx - shift * w * 0.22f * power;
        for (int i = 1; i <= 3 && shift > 0; i++) { // afterimages
            float ghost = cx - shift * w * 0.22f * power * (1 - i * 0.28f);
            figure(b, ghost, ground, h * 0.55f, -0.6f, Anim.fade(accent, 0.5f - i * 0.13f));
        }
        figure(b, figX, ground, h * 0.55f, shift > 0 ? -0.6f : 0, 0xFFE6F4FF);
        float px = x + w + 10 - cycle * (w + 30);
        float py = ground - h * 0.32f;
        b.line(px, py, px + 10, py, 1.6f, 0xFFFF3A5C, Anim.fade(0xFFFF3A5C, 0f));
        if (cycle > 0.4f && cycle < 0.5f) {
            b.ring(cx, py, 6 + (cycle - 0.4f) * 120, 1f, Anim.fade(accent, 1 - (cycle - 0.4f) * 10));
        }
    }

    private static void predict(Draw.Batch b, int x, int y, int w, int h, float t, float power, int accent) {
        float cycle = t * 0.6f;
        float tx = x + w / 2f + Mth.cos(cycle) * w * 0.32f;
        float ty = y + h / 2f + Mth.sin(cycle * 2) * h * 0.22f;
        // Predicted path: where the target will be over the next moments.
        float lead = 0.5f + power;
        float px = tx;
        float py = ty;
        for (int i = 1; i <= 8; i++) {
            float c2 = cycle + lead * i / 8f;
            float nx = x + w / 2f + Mth.cos(c2) * w * 0.32f;
            float ny = y + h / 2f + Mth.sin(c2 * 2) * h * 0.22f;
            if (i % 2 == 0) {
                b.line(px, py, nx, ny, 1.1f, Anim.fade(accent, 1 - i / 9f));
            }
            px = nx;
            py = ny;
        }
        b.glow(tx, ty, 6, 0xFFFF3A5C);
        b.ring(px, py, 5, 1.1f, accent);
        b.line(px - 8, py, px - 3, py, 1, accent).line(px + 3, py, px + 8, py, 1, accent)
                .line(px, py - 8, px, py - 3, 1, accent).line(px, py + 3, px, py + 8, 1, accent);
    }

    private static void radar(Draw.Batch b, float cx, float cy, float r, float t, float power, int accent) {
        b.ring(cx, cy, r, 1.2f, Anim.fade(accent, 0.8f));
        b.ring(cx, cy, r * 0.66f, 1f, Anim.fade(accent, 0.35f));
        b.ring(cx, cy, r * 0.33f, 1f, Anim.fade(accent, 0.35f));
        float angle = t * 2.2f * power;
        for (int i = 0; i < 14; i++) { // the fading wedge behind the beam
            float a = angle - i * 0.06f;
            b.line(cx, cy, cx + Mth.cos(a) * r, cy + Mth.sin(a) * r, 1.6f, Anim.fade(accent, 0.6f * (1 - i / 14f)));
        }
        float[][] blips = {{0.7f, 0.5f}, {0.45f, 2.2f}, {0.85f, 3.6f}, {0.3f, 4.8f}, {0.6f, 5.6f}};
        for (float[] blip : blips) {
            float since = frac((angle - blip[1]) / (Mth.PI * 2));
            if (since < 0.6f) {
                int color = blip[1] > 4 ? 0xFFFF3A5C : 0xFF39FF88;
                b.glow(cx + Mth.cos(blip[1]) * r * blip[0], cy + Mth.sin(blip[1]) * r * blip[0], 4 * (1 - since), color);
            }
        }
    }

    private static void thermal(Draw.Batch b, int x, int y, int w, int h, float ground, float t, float power) {
        b.rect(x, y, x + w, y + h, 0x30FF6A00);
        // A wall with heat signatures behind it.
        float wall = x + w * 0.45f;
        b.rect(wall - 3, y + 4, wall + 3, ground, 0xC01C2640);
        for (int i = 0; i < 3; i++) {
            float bx = x + w * (0.62f + 0.13f * i) + Mth.sin(t + i) * 3;
            float by = ground - h * (0.25f + 0.08f * (i % 2));
            float r = h * 0.14f * (0.8f + 0.2f * power);
            b.disc(bx, by, r * 1.6f, 0x90FFB830, 0x00FF2A00);
            b.disc(bx, by, r * 0.7f, 0xE0FFFFFF, 0x60FFB830);
        }
        figure(b, x + w * 0.2f, ground, h * 0.55f, 0, 0xFFFFB830);
    }

    private static void camo(Draw.Batch b, float cx, float ground, int h, float t, float power, int accent) {
        float cycle = frac(t * 0.3f);
        float alpha = cycle < 0.3f ? 1 - cycle / 0.3f * power : cycle > 0.85f ? (cycle - 0.85f) / 0.15f : 1 - power;
        alpha = Mth.clamp(alpha + (Mth.sin(t * 40) > 0.6f ? 0.15f : 0), 0.05f, 1f);
        figure(b, cx, ground, h * 0.6f, 0, Anim.fade(0xFFE6F4FF, alpha));
        float fy = ground - h * 0.6f + frac(t * 0.9f) * h * 0.6f;
        b.line(cx - h * 0.2f, fy, cx + h * 0.2f, fy, 1, Anim.fade(accent, 0.7f * (1 - alpha)));
    }

    private static void code(Draw.Batch b, int x, int y, int w, int h, float t, float power, int accent) {
        float scroll = t * 14 * power;
        for (int row = -1; row < h / 6 + 1; row++) {
            float ry = y + 3 + row * 6 - frac(scroll / 6) * 6;
            int seed = (int) Math.floor(scroll / 6) + row;
            float indent = ((seed * 37) & 3) * 5;
            float len = 12 + ((seed * 91) & 31);
            int color = ((seed * 13) & 7) == 0 ? 0xFFFF2BD6 : Anim.fade(accent, 0.75f);
            b.rect(x + 4 + indent, ry, Math.min(x + w * 0.62f, x + 4 + indent + len), ry + 2, color);
        }
        // CPU load bars on the right.
        for (int i = 0; i < 3; i++) {
            float bx = x + w * 0.7f + i * 8;
            float level = 0.3f + 0.6f * power * (0.5f + 0.5f * Mth.sin(t * (2 + i) + i));
            b.rect(bx, y + h - 6, bx + 5, y + h - 6 - (h - 12) * level, Anim.fade(accent, 0.85f));
        }
    }

    private static void reactor(Draw.Batch b, float cx, float cy, float r, float t, float power, int accent) {
        b.glow(cx, cy, r * 0.55f * power, accent);
        b.arc(cx, cy, r * 0.9f, 2f, t * 1.4f, Mth.PI * 1.3f, accent);
        b.arc(cx, cy, r * 0.7f, 1.5f, -t * 2f, Mth.PI * 0.9f, Anim.fade(accent, 0.7f));
        b.arc(cx, cy, r * 0.5f, 1f, t * 3f, Mth.PI * 1.6f, Anim.fade(0xFFFFFFFF, 0.6f));
        for (int i = 0; i < 6; i++) {
            float a = t * 0.8f + i * Mth.PI / 3;
            b.line(cx + Mth.cos(a) * r * 0.95f, cy + Mth.sin(a) * r * 0.95f, cx + Mth.cos(a) * r, cy + Mth.sin(a) * r, 2, accent);
        }
    }

    private static void vitals(Draw.Batch b, int x, int y, int w, int h, float t, float power, int accent, boolean defib) {
        float mid = y + h * 0.55f;
        float speed = 30;
        float prevX = x;
        float prevY = mid;
        int color = defib ? 0xFF39FF88 : accent;
        for (float px = x; px <= x + w; px += 1.5f) {
            float phase = frac((px - x + t * speed) / 48f);
            float v;
            if (defib && frac(t * 0.25f) < 0.45f) {
                v = 0; // flatline ...
            } else if (phase < 0.08f) {
                v = Mth.sin(phase / 0.08f * Mth.PI) * 0.15f;
            } else if (phase < 0.12f) {
                v = -(phase - 0.08f) / 0.04f * 0.25f;
            } else if (phase < 0.16f) {
                v = 1.0f * power;
            } else if (phase < 0.2f) {
                v = -0.35f;
            } else {
                v = 0;
            }
            float py = mid - v * h * 0.4f;
            float fade = 0.25f + 0.75f * (px - x) / w;
            b.line(prevX, prevY, px, py, 1.3f, Anim.fade(color, fade));
            prevX = px;
            prevY = py;
        }
        if (defib) {
            float cycle = frac(t * 0.25f);
            if (cycle > 0.45f && cycle < 0.55f) {
                b.rect(x, y, x + w, y + h, Anim.fade(0xFFFFFFFF, (0.55f - cycle) * 4));
            }
        } else {
            float beat = Anim.easeOutCubic(1 - frac(t * 0.8f));
            b.glow(x + w - 10, y + 10, 4 + 3 * beat, 0xFFFF2A6D);
        }
    }

    private static void shield(Draw.Batch b, float cx, float ground, int h, float t, float power, int accent) {
        float cy = ground - h * 0.32f;
        float r = h * 0.4f;
        figure(b, cx, ground, h * 0.5f, 0, 0xFFE6F4FF);
        float hit = frac(t * 0.7f);
        for (int i = 0; i < 6; i++) { // a hexagonal plate pattern around the body
            float a = i * Mth.PI / 3 + Mth.PI / 6;
            b.polygon(cx + Mth.cos(a) * r * 0.62f, cy + Mth.sin(a) * r * 0.62f, r * 0.36f, 6, 0, 1f,
                    Anim.fade(accent, 0.25f + 0.35f * power));
        }
        b.ring(cx, cy, r, 1.2f, Anim.fade(accent, 0.8f));
        float ix = cx + r * 2.2f - hit * r * 1.25f;
        if (hit < 0.8f) {
            b.line(ix, cy - 2, ix + 8, cy - 2, 1.6f, 0xFFFF3A5C, Anim.fade(0xFFFF3A5C, 0f));
        } else {
            float ripple = (hit - 0.8f) / 0.2f;
            b.arc(cx, cy, r + ripple * 6, 2f * (1 - ripple) + 0.5f, -0.9f, 1.8f, Anim.fade(0xFFFFFFFF, 1 - ripple));
        }
    }

    private static void shock(Draw.Batch b, float cx, float ground, int h, float t, float power, int accent) {
        figure(b, cx, ground, h * 0.55f, 0, 0xFFE6F4FF);
        float cy = ground - h * 0.3f;
        int seed = (int) (t * 12);
        for (int arc = 0; arc < 5; arc++) {
            float a = arc * Mth.PI * 2 / 5 + seed * 0.7f;
            float px = cx;
            float py = cy;
            float len = h * 0.45f * power;
            for (int s = 1; s <= 4; s++) {
                float jitter = (((seed * 31 + arc * 17 + s * 7) & 15) - 7.5f) / 7.5f;
                float nx = cx + Mth.cos(a + jitter * 0.35f) * len * s / 4;
                float ny = cy + Mth.sin(a + jitter * 0.35f) * len * s / 4;
                b.line(px, py, nx, ny, 1.2f, s % 2 == 0 ? 0xFFFFFFFF : accent);
                px = nx;
                py = ny;
            }
        }
    }

    private static void jump(Draw.Batch b, int x, int w, float ground, int h, float t, float power, int accent) {
        float cycle = frac(t * 0.45f);
        float fx = x + w * 0.15f + cycle * w * 0.7f;
        // Two hops: the second starts at the top of the first (an air jump).
        float height = h * 0.3f * power;
        float hop = cycle < 0.5f ? Mth.sin(cycle / 0.5f * Mth.PI) : Mth.sin((cycle - 0.25f) / 0.75f * Mth.PI) * 1.2f;
        float fy = ground - Math.max(0, hop) * height;
        for (int i = 1; i <= 10; i++) { // trail
            float c = cycle - i * 0.015f;
            if (c < 0) {
                break;
            }
            float th = c < 0.5f ? Mth.sin(c / 0.5f * Mth.PI) : Mth.sin((c - 0.25f) / 0.75f * Mth.PI) * 1.2f;
            b.glow(x + w * 0.15f + c * w * 0.7f, ground - Math.max(0, th) * height - h * 0.12f, 2, Anim.fade(accent, 0.6f - i * 0.05f));
        }
        figure(b, fx, fy, h * 0.36f, 0.3f, 0xFFE6F4FF);
        if (cycle > 0.48f && cycle < 0.56f) {
            b.ring(fx, fy, 4 + (cycle - 0.48f) * 100, 1, Anim.fade(accent, 1 - (cycle - 0.48f) * 12));
        }
    }

    private static void dash(Draw.Batch b, int x, int w, float ground, int h, float t, float power, int accent) {
        float cycle = frac(t * 0.5f);
        float speed = 0.5f + power;
        float fx = x - 10 + frac(cycle * speed) * (w + 20);
        for (int i = 0; i < 6; i++) {
            float ly = ground - h * (0.12f + i * 0.08f);
            b.line(fx - 8 - i * 4 - 20 * power, ly, fx - 6, ly, 1, Anim.fade(accent, 0f), Anim.fade(accent, 0.8f));
        }
        figure(b, fx, ground, h * 0.5f, 0.8f, 0xFFE6F4FF);
    }

    private static void wallWalk(Draw.Batch b, int x, int y, int w, float ground, int h, float t, float power, int accent) {
        float wall = x + w * 0.7f;
        b.rect(wall, y + 2, wall + 6, ground, 0xC01C2640);
        b.rect(wall, y + 2, wall + 1, ground, Anim.fade(accent, 0.8f));
        float climb = frac(t * 0.3f * (0.6f + power * 0.4f));
        float fy = ground - climb * (h - 12);
        // The climber lies against the wall: draw them rotated by placing the figure's "ground" on the wall.
        float body = h * 0.35f;
        b.line(wall - 1, fy, wall - body * 0.45f, fy - body * 0.1f, 1.5f, 0xFFE6F4FF);
        b.line(wall - body * 0.45f, fy - body * 0.1f, wall - body * 0.8f, fy - body * 0.05f, 1.5f, 0xFFE6F4FF);
        b.ring(wall - body * 0.95f, fy - body * 0.05f, body * 0.14f, 1.2f, 0xFFE6F4FF);
        b.line(wall - body * 0.6f, fy - body * 0.08f, wall - 1, fy - body * 0.45f, 1.2f, 0xFFE6F4FF);
        b.glow(wall, fy, 3, accent);
        b.glow(wall, fy - body * 0.45f, 3, accent);
    }

    private static void strike(Draw.Batch b, float cx, float ground, int h, float t, float power, int accent, boolean blade) {
        float cycle = frac(t * 0.8f);
        float fx = cx - h * 0.25f;
        figure(b, fx, ground, h * 0.55f, 0.4f, 0xFFE6F4FF);
        float sy = ground - h * 0.35f;
        float swing = Anim.easeOutCubic(Math.min(1, cycle / 0.3f));
        float start = -1.6f;
        float sweep = 2.2f * swing;
        float r = h * (blade ? 0.42f : 0.3f);
        if (cycle < 0.6f) {
            b.arc(fx + 4, sy, r, blade ? 2.5f : 4f, start, sweep, Anim.fade(accent, 0.15f), Anim.fade(accent, 0.9f * (1 - cycle)));
        }
        if (blade) {
            float a = start + sweep;
            b.line(fx + 4, sy, fx + 4 + Mth.cos(a) * r, sy + Mth.sin(a) * r, 1.8f, 0xFFFFFFFF, accent);
        }
        if (cycle > 0.25f && cycle < 0.5f) {
            float burst = (cycle - 0.25f) / 0.25f;
            float bx = cx + h * 0.2f;
            for (int i = 0; i < 8; i++) {
                float a = i * Mth.PI / 4;
                float r0 = 3 + burst * 8 * power;
                b.line(bx + Mth.cos(a) * r0, sy + Mth.sin(a) * r0, bx + Mth.cos(a) * (r0 + 4), sy + Mth.sin(a) * (r0 + 4), 1.2f,
                        Anim.fade(0xFFFFB830, 1 - burst));
            }
        }
    }

    private static void projectile(Draw.Batch b, int x, int w, float ground, int h, float t, float power, int accent) {
        float fx = x + w * 0.15f;
        figure(b, fx, ground, h * 0.55f, 0, 0xFFE6F4FF);
        float sy = ground - h * 0.35f;
        for (int i = 0; i < 3; i++) {
            float p = frac(t * (0.8f + power * 0.6f) + i / 3f);
            float px = fx + 8 + p * (w * 0.8f);
            b.line(px - 6, sy, px, sy, 1.4f, Anim.fade(accent, 0f), accent);
        }
        float tx = x + w * 0.9f;
        b.ring(tx, sy, 5, 1.2f, 0xFFFF3A5C);
        b.ring(tx, sy, 2, 1, 0xFFFF3A5C);
    }

    private static void reach(Draw.Batch b, int x, int w, float ground, int h, float t, float power, int accent) {
        float fx = x + w * 0.2f;
        figure(b, fx, ground, h * 0.55f, 0, 0xFFE6F4FF);
        float hand = ground - h * 0.38f;
        float lx = x + w * 0.85f;
        float cycle = frac(t * 0.4f);
        float extend = Math.min(1, cycle / 0.4f);
        b.dashed(fx + 6, hand, fx + 6 + (lx - fx - 6) * extend, hand, 1.2f, 3, 2, t * 20, accent);
        boolean on = cycle > 0.45f;
        b.rect(lx - 3, hand + 4, lx + 3, hand + 8, 0xFF2C3856);
        float angle = on ? -0.6f : -2.5f;
        b.line(lx, hand + 4, lx + Mth.cos(angle) * 7, hand + 4 + Mth.sin(angle) * 7, 1.6f, on ? 0xFF39FF88 : 0xFFFF3A5C);
        if (on) {
            b.glow(lx, hand - 6, 4 * power, 0xFFFFE066);
        }
    }

    private EffectPreview() {
    }
}
