package dev.cyberimplants.client.ui;

import dev.cyberimplants.config.ClientConfig;
import net.minecraft.Util;
import net.minecraft.util.Mth;

/**
 * Time and easing for interface animation. Everything runs on wall-clock milliseconds so screens stay smooth while the
 * game is paused or lagging. With {@code ui.animations = false} (or {@code reduceScreenEffects}) every animation jumps to
 * its end state.
 */
public final class Anim {
    /** Seconds since an arbitrary epoch. */
    public static float now() {
        return (Util.getMillis() % 10_000_000L) / 1000f;
    }

    public static boolean enabled() {
        try {
            return ClientConfig.UI_ANIMATIONS.get() && !ClientConfig.REDUCE_SCREEN_EFFECTS.get();
        } catch (IllegalStateException e) {
            return true; // config not loaded yet
        }
    }

    /** 0..1 progress of an animation that started at {@code start} (from {@link #now()}) and lasts {@code duration} s. */
    public static float progress(float start, float duration) {
        if (!enabled() || duration <= 0) {
            return 1f;
        }
        return Mth.clamp((now() - start) / duration, 0f, 1f);
    }

    public static float easeOutCubic(float t) {
        float u = 1 - t;
        return 1 - u * u * u;
    }

    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
    }

    /** Overshoots a little before settling: for things that pop in. */
    public static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    /** A 0..1 wave with the given period in seconds. Constant 0.5 when animations are off. */
    public static float pulse(float period) {
        return enabled() ? 0.5f + 0.5f * Mth.sin(now() * (float) (Math.PI * 2) / period) : 0.5f;
    }

    /** The first characters of {@code text} as if typed at {@code cps} characters per second since {@code start}. */
    public static String typed(String text, float start, float cps) {
        if (!enabled()) {
            return text;
        }
        int n = (int) Math.max(0, (now() - start) * cps);
        return n >= text.length() ? text : text.substring(0, n);
    }

    /** Scales the alpha byte of an ARGB colour. */
    public static int fade(int argb, float alpha) {
        int a = (int) ((argb >>> 24) * Mth.clamp(alpha, 0f, 1f));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** Linear blend of two ARGB colours. */
    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int aa = (int) Mth.lerp(t, a >>> 24, b >>> 24);
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (aa << 24) | (r << 16) | (g << 8) | bl;
    }

    /**
     * A value that glides towards its target at a frame-rate independent speed. Used for hover glows, meters and
     * panel slides.
     */
    public static final class Smooth {
        private float value;
        private long last = -1;
        private final float rate;

        /** @param rate how fast it closes the gap: about 1 - e^(-rate * seconds) of it per frame */
        public Smooth(float initial, float rate) {
            this.value = initial;
            this.rate = rate;
        }

        public float update(float target) {
            long t = Util.getMillis();
            if (last < 0 || !enabled()) {
                value = target;
            } else {
                float dt = Math.min(0.25f, (t - last) / 1000f);
                value += (target - value) * (1 - (float) Math.exp(-rate * dt));
                if (Math.abs(target - value) < 1e-4f) {
                    value = target;
                }
            }
            last = t;
            return value;
        }

        public float get() {
            return value;
        }

        public void snap(float target) {
            value = target;
            last = Util.getMillis();
        }
    }

    private Anim() {
    }
}
