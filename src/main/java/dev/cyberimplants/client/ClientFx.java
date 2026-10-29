package dev.cyberimplants.client;

import dev.cyberimplants.client.ui.Anim;
import dev.cyberimplants.client.ui.Draw;
import dev.cyberimplants.config.ClientConfig;
import dev.cyberimplants.fx.ScreenFx;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Screen-space implant effects for the local player: the green time-dilation wash of a Sandevistan field, static while
 * cloaked, the Defibrillator's flash, a radar ring for the Entity Scanner, screen shake and FOV kicks. The server starts
 * them with {@link dev.cyberimplants.network.ScreenFxPayload}; the effects in the world are in
 * {@link dev.cyberimplants.fx.ImplantFx}.
 *
 * <p>{@code hud.reduceScreenEffects} keeps only thin borders and labels: no flashes, static, shake or FOV changes.
 */
public final class ClientFx {
    private record Active(long start, long durationMs, float strength) {
        float progress(long now) {
            return durationMs <= 0 ? 1f : Mth.clamp((now - start) / (float) durationMs, 0f, 1f);
        }
    }

    private static final Map<ScreenFx, Active> ACTIVE = new EnumMap<>(ScreenFx.class);

    public static void trigger(String name, int ticks, float strength) {
        ScreenFx fx = ScreenFx.byName(name);
        if (fx == null) {
            return;
        }
        if (ticks <= 0) {
            ACTIVE.remove(fx);
        } else {
            ACTIVE.put(fx, new Active(Util.getMillis(), ticks * 50L, Mth.clamp(strength, 0f, 1f)));
        }
    }

    private static boolean reduced() {
        try {
            return ClientConfig.REDUCE_SCREEN_EFFECTS.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /** Fades in over the first {@code in} and out over the last {@code out} of the effect (both fractions). */
    private static float envelope(float t, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(t / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((1 - t) / out, 0, 1);
        return Math.min(a, b);
    }

    // ---------------------------------------------------------------- tick: local particles and sounds

    private static int lastLeapStep;

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            ACTIVE.clear();
            return;
        }
        long now = Util.getMillis();
        ACTIVE.values().removeIf(a -> now - a.start > a.durationMs);
        if (mc.isPaused()) {
            return;
        }
        // Hydraulic Legs build pressure: steam at the feet and a rising hiss at each quarter.
        float leap = ClientMovement.leapProgress();
        if (leap > 0) {
            if (p.tickCount % 3 == 0) {
                for (int i = 0; i < 2; i++) {
                    double a = p.getRandom().nextDouble() * Math.PI * 2;
                    mc.level.addParticle(ParticleTypes.CLOUD, p.getX() + Math.cos(a) * 0.3, p.getY() + 0.05,
                            p.getZ() + Math.sin(a) * 0.3, Math.cos(a) * 0.03, 0.02 + 0.04 * leap, Math.sin(a) * 0.03);
                }
            }
            int step = (int) (leap * 4);
            if (step != lastLeapStep && step > 0) {
                mc.level.playLocalSound(p.getX(), p.getY(), p.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS,
                        0.35f, 0.6f + 0.25f * step, false);
            }
            lastLeapStep = step;
        } else {
            lastLeapStep = 0;
        }
    }

    // ---------------------------------------------------------------- camera

    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ACTIVE.isEmpty() || reduced()) {
            return;
        }
        long now = Util.getMillis();
        float amount = 0;
        for (Map.Entry<ScreenFx, Active> e : ACTIVE.entrySet()) {
            Active a = e.getValue();
            float fade = 1 - a.progress(now);
            amount = Math.max(amount, switch (e.getKey()) {
                case DEFIB -> 3.0f * fade * fade;
                case GLITCH -> 1.6f * a.strength * fade;
                case STRIKE -> 1.2f * fade;
                case LANDING -> 2.4f * a.strength * fade;
                case SHIELD_BREAK -> 1.6f * fade;
                case SHOCK -> 0.9f * fade;
                case LEAP -> 0.7f * a.strength * fade;
                default -> 0f;
            });
        }
        if (amount <= 0) {
            return;
        }
        float t = now / 1000f;
        event.setYaw(event.getYaw() + amount * (Mth.sin(t * 61f) * 0.6f + Mth.sin(t * 37f) * 0.4f));
        event.setPitch(event.getPitch() + amount * (Mth.sin(t * 53f + 1.3f) * 0.6f + Mth.sin(t * 29f) * 0.4f));
        event.setRoll(event.getRoll() + amount * 0.5f * Mth.sin(t * 43f + 0.7f));
    }

    static void onFov(ComputeFovModifierEvent event) {
        if (ACTIVE.isEmpty() || reduced()) {
            return;
        }
        long now = Util.getMillis();
        float factor = 1f;
        for (Map.Entry<ScreenFx, Active> e : ACTIVE.entrySet()) {
            Active a = e.getValue();
            float t = a.progress(now);
            float kick = Anim.easeOutCubic(Mth.clamp(t * 5, 0, 1)) * (1 - t);
            factor *= switch (e.getKey()) {
                case SANDEVISTAN -> 1f - 0.08f * envelope(t, 0.08f, 0.15f);
                case DASH -> 1f + 0.16f * Math.max(0.4f, a.strength) * kick;
                case LEAP -> 1f + 0.10f * Math.max(0.4f, a.strength) * kick;
                case KERENZIKOV -> 1f + 0.07f * kick;
                case EVADE -> 1f + 0.04f * kick;
                default -> 1f;
            };
        }
        event.setNewFovModifier(event.getNewFovModifier() * factor);
    }

    // ---------------------------------------------------------------- overlay

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (ACTIVE.isEmpty() || mc.player == null || mc.options.hideGui) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        long now = Util.getMillis();
        boolean calm = reduced();
        Font font = mc.font;

        // Shapes first, in one batch, then plain fills and text on top (the batch must not be interleaved with them).
        Draw.Batch b = Draw.batch(g);
        for (Map.Entry<ScreenFx, Active> e : ACTIVE.entrySet()) {
            shapes(b, e.getKey(), e.getValue(), now, w, h, calm);
        }
        b.end();
        for (Map.Entry<ScreenFx, Active> e : ACTIVE.entrySet()) {
            overlay(g, font, e.getKey(), e.getValue(), now, w, h, calm);
        }
    }

    private static void shapes(Draw.Batch b, ScreenFx fx, Active a, long now, int w, int h, boolean calm) {
        float t = a.progress(now);
        float time = (now % 100_000L) / 1000f;
        float cx = w / 2f;
        float cy = h / 2f;
        float diag = Mth.sqrt(w * w + h * h) / 2f;
        switch (fx) {
            case SANDEVISTAN -> {
                float e = envelope(t, 0.06f, 0.12f);
                vignette(b, w, h, 0xFF39FF88, 0.5f * e, h * 0.3f);
                if (!calm) {
                    // Chromatic fringes on the screen edges, shivering.
                    float jitter = 2 + 2 * Mth.sin(time * 23);
                    b.quad(0, 0, 0, h, 6 + jitter, h, 6 + jitter, 0, Anim.fade(0xFFFF2BD6, 0.3f * e), Anim.fade(0xFFFF2BD6, 0.3f * e),
                            0x00FF2BD6, 0x00FF2BD6);
                    b.quad(w, 0, w - 6 - jitter, 0, w - 6 - jitter, h, w, h, Anim.fade(0xFF19D9FF, 0.3f * e), 0x0019D9FF,
                            0x0019D9FF, Anim.fade(0xFF19D9FF, 0.3f * e));
                    // Echo rings breathing out from the centre: time stretching.
                    for (int i = 0; i < 3; i++) {
                        float p = (time * 0.35f + i / 3f) % 1f;
                        b.ring(cx, cy, diag * (0.2f + 0.9f * p), 1.5f, Anim.fade(0xFF39FF88, 0.18f * (1 - p) * e));
                    }
                }
                // Remaining time as a draining bar under the label.
                float left = 1 - t;
                b.rect(cx - 50, 26, cx + 50, 28, Anim.fade(0xFF0A0E18, 0.7f * e));
                b.rect(cx - 50, 26, cx - 50 + 100 * left, 28, Anim.fade(0xFF39FF88, 0.9f * e));
            }
            case KERENZIKOV, DASH, LEAP -> {
                float fade = 1 - t;
                int color = fx == ScreenFx.KERENZIKOV ? 0xFFB45CFF : fx == ScreenFx.DASH ? 0xFFFF2BD6 : 0xFFFFB830;
                vignette(b, w, h, color, 0.35f * fade, h * 0.18f);
                if (!calm) {
                    speedLines(b, cx, fx == ScreenFx.LEAP ? h * 0.8f : cy, diag, color, fade * Math.max(0.5f, a.strength), time);
                }
            }
            case EVADE -> vignette(b, w, h, 0xFF19D9FF, 0.4f * (1 - t), h * 0.2f);
            case CAMO -> {
                float e = envelope(t, 0.1f, 0.05f);
                // Near the end the cloak starts to fail and flickers.
                if (t > 0.8f && Mth.sin(time * 40) > 0.3f) {
                    e *= 0.4f;
                }
                vignette(b, w, h, 0xFF8FB4E0, 0.35f * e, h * 0.22f);
                if (!calm) {
                    // Refraction bands drifting across the view.
                    for (int i = 0; i < 4; i++) {
                        float x = ((time * 40 + i * w / 4f) % (w + 60)) - 30;
                        b.quad(x - 18, 0, x - 18, h, x, h, x, 0, 0x008FB4E0, 0x008FB4E0, Anim.fade(0xFF8FB4E0, 0.07f * e),
                                Anim.fade(0xFF8FB4E0, 0.07f * e));
                        b.quad(x, 0, x, h, x + 18, h, x + 18, 0, Anim.fade(0xFF8FB4E0, 0.07f * e), Anim.fade(0xFF8FB4E0, 0.07f * e),
                                0x008FB4E0, 0x008FB4E0);
                    }
                    noise(b, w, h, 0xFFB8D8FF, 0.25f * e, 50, (int) (time * 20), true);
                }
            }
            case SCAN -> {
                for (int i = 0; i < 2; i++) {
                    float p = Mth.clamp(t * 1.3f - i * 0.25f, 0, 1);
                    if (p <= 0 || p >= 1) {
                        continue;
                    }
                    float r = diag * 1.1f * Anim.easeOutCubic(p);
                    b.ring(cx, cy, r, 3f, Anim.fade(0xFF19D9FF, 0.55f * (1 - p)));
                    b.ring(cx, cy, r - 6, 1f, Anim.fade(0xFF19D9FF, 0.25f * (1 - p)));
                }
                // A radar sweep arm turning once.
                float angle = -(Mth.PI / 2) + t * (Mth.PI * 2);
                b.line(cx, cy, cx + Mth.cos(angle) * diag * 0.35f, cy + Mth.sin(angle) * diag * 0.35f, 1.5f,
                        Anim.fade(0xFF19D9FF, 0.5f * (1 - t)), 0x0019D9FF);
            }
            case TAG -> {
                float s = 40 - 28 * Anim.easeOutCubic(Math.min(1, t * 2));
                int c = Anim.fade(0xFFFF3A5C, 1 - t * t);
                float len = 7;
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        float x = cx + sx * s;
                        float y = cy + sy * s;
                        b.line(x, y, x - sx * len, y, 1.5f, c);
                        b.line(x, y, x, y - sy * len, 1.5f, c);
                    }
                }
                b.ring(cx, cy, 3 + 10 * t, 1f, Anim.fade(0xFFFF3A5C, 0.6f * (1 - t)));
            }
            case DEFIB -> {
                float fade = 1 - t;
                // Two quick heartbeats per second, fading.
                float beat = Mth.abs(Mth.sin(time * Mth.PI * 2.2f));
                vignette(b, w, h, 0xFFFF2A6D, (0.3f + 0.35f * beat) * fade, h * 0.32f);
                ecg(b, w, h * 0.78f, t, fade);
            }
            case ADRENALINE -> {
                float e = envelope(t, 0.05f, 0.2f);
                float beat = (float) Math.pow(Mth.abs(Mth.sin(time * Mth.PI * 1.6f)), 6);
                vignette(b, w, h, 0xFFFF2A6D, (0.18f + 0.3f * beat) * e, h * (0.18f + 0.05f * beat));
            }
            case SHELL -> {
                float e = envelope(t, 0.05f, 0.15f);
                vignette(b, w, h, 0xFFFFB830, 0.3f * e, h * 0.2f);
                // A rim of hex plates around the view.
                float r = 11;
                float step = r * 1.75f;
                int c = Anim.fade(0xFFFFB830, 0.45f * e);
                for (float x = r; x < w; x += step) {
                    b.polygon(x, r * 0.9f, r, 6, Mth.PI / 6, 1f, c);
                    b.polygon(x, h - r * 0.9f, r, 6, Mth.PI / 6, 1f, c);
                }
                for (float y = r + step; y < h - step; y += step) {
                    b.polygon(r * 0.9f, y, r, 6, 0, 1f, c);
                    b.polygon(w - r * 0.9f, y, r, 6, 0, 1f, c);
                }
            }
            case SHIELD_HIT -> {
                float fade = 1 - t;
                vignette(b, w, h, 0xFF39FF88, 0.45f * a.strength * fade, h * 0.2f);
                b.ring(cx, cy, diag * (0.55f + 0.1f * t), 2f, Anim.fade(0xFF39FF88, 0.3f * fade));
            }
            case SHIELD_BREAK -> {
                float fade = 1 - t;
                vignette(b, w, h, 0xFF39FF88, 0.6f * fade, h * 0.3f);
                if (!calm) {
                    cracks(b, w, h, Anim.fade(0xFFE6FFF0, 0.8f * fade), 7, 1234);
                }
            }
            case SHOCK -> {
                float fade = 1 - t;
                vignette(b, w, h, 0xFF8FB4E0, 0.35f * fade, h * 0.15f);
                if (!calm) {
                    cracks(b, w, h, Anim.fade(0xFFFFFFFF, fade), 5, (int) (time * 30));
                }
            }
            case GLITCH -> {
                float fade = 1 - t;
                if (!calm) {
                    int seed = (int) (time * 24);
                    for (int i = 0; i < 9; i++) {
                        int r = hash(seed * 31 + i);
                        float y = (r & 0xFFFF) / 65535f * h;
                        float bh = 2 + (r >>> 16 & 7);
                        float shift = ((r >>> 19 & 31) - 16) * 1.5f;
                        int color = (r & 1) == 0 ? 0xFFFF2BD6 : 0xFF19D9FF;
                        b.rect(Math.max(0, shift), y, Math.min(w, w + shift), y + bh, Anim.fade(color, 0.3f * fade * a.strength));
                    }
                    noise(b, w, h, 0xFFFFFFFF, 0.35f * fade, 160, seed, false);
                }
                vignette(b, w, h, 0xFFFF2BD6, 0.3f * fade, h * 0.15f);
            }
            case ERS -> {
                boolean on = ((int) (t * 6)) % 2 == 0;
                if (on) {
                    vignette(b, w, h, 0xFFFF3A5C, 0.55f * (1 - t * 0.5f), h * 0.16f);
                }
            }
            case STRIKE -> {
                float fade = 1 - t;
                b.ring(cx, cy, 6 + 30 * Anim.easeOutCubic(t), 2f, Anim.fade(0xFFFFFFFF, 0.7f * fade));
                if (!calm) {
                    speedLines(b, cx, cy, diag * 0.6f, 0xFFFFB830, fade * 0.6f, time);
                }
            }
            case LANDING -> {
                float fade = 1 - t;
                b.quad(0, h, w, h, w, h - h * 0.25f, 0, h - h * 0.25f, Anim.fade(0xFFFFB830, 0.35f * fade * a.strength),
                        Anim.fade(0xFFFFB830, 0.35f * fade * a.strength), 0x00FFB830, 0x00FFB830);
            }
            case HEAL -> vignette(b, w, h, 0xFF39FF88, 0.35f * envelope(t, 0.1f, 0.6f), h * 0.25f);
        }
    }

    /** Full-screen washes and labels, drawn after the shapes. */
    private static void overlay(GuiGraphics g, Font font, ScreenFx fx, Active a, long now, int w, int h, boolean calm) {
        float t = a.progress(now);
        int cx = w / 2;
        switch (fx) {
            case SANDEVISTAN -> {
                float e = envelope(t, 0.06f, 0.12f);
                if (!calm) {
                    g.fill(0, 0, w, h, Anim.fade(0xFF39FF88, 0.08f * e));
                    for (int y = 0; y < h; y += 3) {
                        g.fill(0, y, w, y + 1, Anim.fade(0xFF000000, 0.12f * e));
                    }
                }
                label(g, font, Component.translatable("cyberimplants.fx.temporal"), cx, 14, 0xFF39FF88, e);
            }
            case KERENZIKOV -> {
                if (!calm && t < 0.5f) {
                    g.fill(0, 0, w, h, Anim.fade(0xFFB45CFF, 0.3f * (1 - t * 2)));
                }
            }
            case EVADE -> label(g, font, Component.translatable("cyberimplants.fx.evaded"), cx, h / 2 + 14, 0xFF19D9FF, 1 - t);
            case CAMO -> {
                float e = envelope(t, 0.1f, 0.05f);
                if (!calm) {
                    g.fill(0, 0, w, h, Anim.fade(0xFF8FB4E0, 0.06f * e));
                }
                long left = Math.max(0, (a.durationMs - (now - a.start)) / 1000 + 1);
                label(g, font, Component.translatable("cyberimplants.fx.cloak", left), cx, h - 70, 0xFF8FB4E0, e);
            }
            case DEFIB -> {
                if (!calm && t < 0.12f) {
                    g.fill(0, 0, w, h, Anim.fade(0xFFFFFFFF, 0.85f * (1 - t / 0.12f)));
                }
                label(g, font, Component.translatable("cyberimplants.fx.defib"), cx, 14, 0xFF39FF88, 1 - t);
            }
            case SHIELD_BREAK -> {
                if (!calm && t < 0.3f) {
                    g.fill(0, 0, w, h, Anim.fade(0xFFE6FFF0, 0.35f * (1 - t / 0.3f)));
                }
            }
            case SHOCK -> {
                if (!calm && t < 0.4f) {
                    g.fill(0, 0, w, h, Anim.fade(0xFFCFE6FF, 0.2f * (1 - t / 0.4f)));
                }
            }
            case SHELL -> label(g, font, Component.translatable("cyberimplants.fx.shell"), cx, 14, 0xFFFFB830, envelope(t, 0.05f, 0.15f));
            case GLITCH -> {
                if (((int) (t * 10)) % 3 != 2) {
                    int jitter = calm ? 0 : (int) (Mth.sin(now * 0.05f) * 3);
                    label(g, font, Component.translatable("cyberimplants.fx.signal_loss"), cx + jitter, h / 2 - 30, 0xFFFF2BD6, 1 - t);
                }
            }
            case ERS -> label(g, font, Component.translatable("cyberimplants.fx.ers"), cx, 14, 0xFFFF3A5C, 1 - t * 0.6f);
            case HEAL -> {
                if (!calm) {
                    g.fill(0, 0, w, h, Anim.fade(0xFF39FF88, 0.06f * envelope(t, 0.1f, 0.6f)));
                }
            }
            default -> {
            }
        }
    }

    // ---------------------------------------------------------------- pieces

    private static void label(GuiGraphics g, Font font, Component text, int x, int y, int color, float alpha) {
        if (alpha <= 0.05f) {
            return;
        }
        int a = Anim.fade(color, alpha);
        if ((a >>> 24) < 8) {
            return; // the font treats a near-zero alpha as opaque
        }
        int tw = font.width(text);
        g.fill(x - tw / 2 - 5, y - 3, x + tw / 2 + 5, y + 11, Anim.fade(0xFF060912, 0.6f * alpha));
        g.fill(x - tw / 2 - 5, y + 11, x + tw / 2 + 5, y + 12, a);
        g.drawCenteredString(font, text, x, y, a);
    }

    /** Edges of the screen washed with {@code color}, fading inwards over {@code size} pixels. */
    private static void vignette(Draw.Batch b, int w, int h, int color, float alpha, float size) {
        if (alpha <= 0.002f) {
            return;
        }
        int c = Anim.fade(color, alpha);
        int z = color & 0x00FFFFFF;
        b.quad(0, 0, 0, size, w, size, w, 0, c, z, z, c);
        b.quad(0, h - size, 0, h, w, h, w, h - size, z, c, c, z);
        b.quad(0, 0, 0, h, size, h, size, 0, c, c, z, z);
        b.quad(w - size, 0, w - size, h, w, h, w, 0, z, z, c, c);
    }

    /** Streaks rushing out from a point, as when moving fast. */
    private static void speedLines(Draw.Batch b, float cx, float cy, float diag, int color, float alpha, float time) {
        for (int i = 0; i < 28; i++) {
            int r = hash(i * 977 + 13);
            float angle = (r & 0xFFFF) / 65535f * (Mth.PI * 2);
            float phase = ((r >>> 16 & 0xFF) / 255f + time * 2.5f) % 1f;
            float r0 = diag * (0.45f + 0.4f * phase);
            float r1 = r0 + diag * (0.12f + 0.1f * ((r >>> 24) & 0xF) / 15f);
            float dx = Mth.cos(angle);
            float dy = Mth.sin(angle);
            b.line(cx + dx * r0, cy + dy * r0, cx + dx * r1, cy + dy * r1, 1.2f, color & 0x00FFFFFF, Anim.fade(color, 0.5f * alpha));
        }
    }

    /** Sparse static: small bright specks; {@code edges} keeps them near the screen border. */
    private static void noise(Draw.Batch b, int w, int h, int color, float alpha, int count, int seed, boolean edges) {
        for (int i = 0; i < count; i++) {
            int r = hash(seed * 7919 + i * 104729);
            float x = (r & 0xFFFF) / 65535f * w;
            float y = (r >>> 16 & 0xFFFF) / 65535f * h;
            if (edges) {
                float ex = Math.min(x, w - x) / (w * 0.5f);
                float ey = Math.min(y, h - y) / (h * 0.5f);
                if (Math.min(ex, ey) > 0.25f) {
                    continue;
                }
            }
            float s = 1 + (i % 3);
            b.rect(x, y, x + s, y + 1, Anim.fade(color, alpha * (0.4f + 0.6f * ((r >>> 8) & 0xF) / 15f)));
        }
    }

    /** Jagged lines running in from the screen edges: cracks in a shield, or arcs of current. */
    private static void cracks(Draw.Batch b, int w, int h, int color, int count, int seed) {
        for (int i = 0; i < count; i++) {
            int r = hash(seed * 13 + i * 7);
            int side = r & 3;
            float along = ((r >>> 2) & 0xFFFF) / 65535f;
            float x = side == 0 ? along * w : side == 1 ? w : side == 2 ? along * w : 0;
            float y = side == 0 ? 0 : side == 1 ? along * h : side == 2 ? h : along * h;
            float tx = w / 2f + (along - 0.5f) * w * 0.3f;
            float ty = h / 2f + ((r >>> 18 & 0xFF) / 255f - 0.5f) * h * 0.3f;
            int segments = 6;
            float stepX = (tx - x) * 0.55f / segments;
            float stepY = (ty - y) * 0.55f / segments;
            for (int s = 0; s < segments; s++) {
                int rr = hash(r + s * 31);
                float nx = x + stepX + ((rr & 0xFF) / 255f - 0.5f) * 14;
                float ny = y + stepY + ((rr >>> 8 & 0xFF) / 255f - 0.5f) * 14;
                b.line(x, y, nx, ny, 1.2f, color);
                x = nx;
                y = ny;
            }
        }
    }

    /** A heartbeat trace running across the screen: flat at first, then the restart spike. */
    private static void ecg(Draw.Batch b, int w, float mid, float t, float alpha) {
        float drawn = Math.min(1, t * 2.5f) * w;
        float prevX = 0;
        float prevY = mid;
        for (float x = 0; x <= drawn; x += 2) {
            float phase = (x / w * 3) % 1f;
            float v;
            if (x < w * 0.3f) {
                v = 0;
            } else if (phase < 0.06f) {
                v = -0.3f;
            } else if (phase < 0.1f) {
                v = 1f;
            } else if (phase < 0.14f) {
                v = -0.5f;
            } else {
                v = 0;
            }
            float y = mid - v * 22;
            b.line(prevX, prevY, x, y, 1.5f, Anim.fade(0xFF39FF88, alpha * (0.3f + 0.7f * x / Math.max(1, drawn))));
            prevX = x;
            prevY = y;
        }
    }

    private static int hash(int x) {
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    private ClientFx() {
    }
}
