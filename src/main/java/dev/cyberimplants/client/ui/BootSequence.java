package dev.cyberimplants.client.ui;

import dev.cyberimplants.client.CyberUi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * A short loading screen: log lines type themselves out over a progress bar while a screen "connects". Purely cosmetic,
 * a fraction of a second long, skippable by clicking, and skipped entirely when animations are off.
 */
public final class BootSequence {
    private final List<String> lines;
    private final float duration;
    private float start;
    private int announced;
    private boolean skipped;

    public BootSequence(List<String> lines, float duration) {
        this.lines = lines;
        this.duration = duration;
        this.start = Anim.now();
    }

    public float progress() {
        return skipped ? 1f : Anim.progress(start, duration);
    }

    public boolean done() {
        return progress() >= 1f;
    }

    public void skip() {
        skipped = true;
    }

    public void restart() {
        start = Anim.now();
        skipped = false;
        announced = 0;
    }

    /** Draws the boot log centred in the box; the box is darkened behind it. */
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int accent) {
        float p = progress();
        float fadeOut = p > 0.85f ? 1 - (p - 0.85f) / 0.15f : 1f;
        g.fill(x, y, x + w, y + h, Anim.fade(0xF0060912, fadeOut));
        int boxW = Math.min(w - 20, 220);
        int lineCount = lines.size();
        int boxH = lineCount * 10 + 26;
        int bx = x + (w - boxW) / 2;
        int by = y + (h - boxH) / 2;
        CyberUi.panel(g, bx, by, boxW, boxH, Anim.fade(accent, fadeOut), Anim.fade(CyberUi.PANEL, fadeOut));
        int shown = Math.min(lineCount, (int) (p * 0.85f * (lineCount + 1)));
        if (shown > announced) {
            announced = shown;
            UiSounds.tick();
        }
        for (int i = 0; i < shown; i++) {
            String line = lines.get(i);
            boolean last = i == shown - 1 && shown < lineCount;
            String text = last ? line.substring(0, Math.min(line.length(), (int) (line.length() * frac(p * 0.85f * (lineCount + 1))) + 1)) : line;
            int color = i == lineCount - 1 ? CyberUi.GREEN : CyberUi.TEXT_DIM;
            g.drawString(font, text + (last && (int) (Anim.now() * 4) % 2 == 0 ? "_" : ""), bx + 8, by + 8 + i * 10,
                    Anim.fade(color, fadeOut), false);
        }
        CyberUi.meter(g, bx + 8, by + boxH - 12, boxW - 16, 3, Mth.clamp(p / 0.85f, 0, 1), Anim.fade(accent, fadeOut));
    }

    private static float frac(float v) {
        return v - Mth.floor(v);
    }
}
