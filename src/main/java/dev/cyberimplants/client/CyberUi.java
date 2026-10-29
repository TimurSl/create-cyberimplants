package dev.cyberimplants.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The mod's cyberpunk look, drawn with plain fills so it needs no GUI textures: near-black glass panels, neon edges with
 * clipped corners, scanlines and a faint grid. Screens, buttons and the HUD all share these colours.
 */
public final class CyberUi {
    public static final int BG = 0xF00A0E18;
    public static final int PANEL = 0xE0121828;
    public static final int PANEL_LIGHT = 0xE01C2640;
    public static final int CYAN = 0xFF19D9FF;
    public static final int MAGENTA = 0xFFFF2BD6;
    public static final int GREEN = 0xFF39FF88;
    public static final int AMBER = 0xFFFFB830;
    public static final int RED = 0xFFFF3A5C;
    public static final int TEXT = 0xFFE6F4FF;
    public static final int TEXT_DIM = 0xFF7C8CAA;

    /** The full-screen backdrop: a dark gradient, a grid and slow scanlines. {@code time} is in ticks. */
    public static void backdrop(GuiGraphics g, int width, int height, float time) {
        g.fillGradient(0, 0, width, height, 0xF0060912, 0xF0110A24);
        for (int x = 0; x < width; x += 16) {
            g.fill(x, 0, x + 1, height, 0x1019D9FF);
        }
        for (int y = 0; y < height; y += 16) {
            g.fill(0, y, width, y + 1, 0x1019D9FF);
        }
        int scan = (int) (time * 0.8f) % Math.max(1, height);
        g.fillGradient(0, scan, width, Math.min(height, scan + 24), 0x0019D9FF, 0x1819D9FF);
        for (int y = 0; y < height; y += 3) {
            g.fill(0, y, width, y + 1, 0x14000000);
        }
    }

    /** A glass panel with clipped top-left and bottom-right corners and a neon edge. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int accent) {
        panel(g, x, y, w, h, accent, PANEL);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h, int accent, int fill) {
        int c = Math.min(4, Math.min(w, h) / 3);
        g.fill(x + c, y, x + w, y + h, fill);
        g.fill(x, y + c, x + c, y + h, fill);
        // Edge: top and left run into the clipped corner, bottom and right likewise.
        g.fill(x + c, y, x + w, y + 1, accent);
        g.fill(x, y + c, x + 1, y + h, accent);
        g.fill(x, y + h - 1, x + w - c, y + h, dim(accent, 0.55f));
        g.fill(x + w - 1, y, x + w, y + h - c, dim(accent, 0.55f));
        for (int i = 0; i < c; i++) { // the diagonal cuts
            g.fill(x + c - i - 1, y + i, x + c - i, y + i + 1, accent);
            g.fill(x + w - c + i, y + h - i - 1, x + w - c + i + 1, y + h - i, dim(accent, 0.55f));
        }
    }

    /** A screen title: neon label on a bar with a short underline. */
    public static void title(GuiGraphics g, net.minecraft.client.gui.Font font, Component title, int x, int y, int accent) {
        g.drawString(font, title.copy().withStyle(net.minecraft.ChatFormatting.BOLD), x, y, accent, true);
        int w = font.width(title) + 2;
        g.fill(x, y + 10, x + w, y + 11, accent);
        g.fill(x + w, y + 10, x + w + 14, y + 11, dim(accent, 0.35f));
    }

    /** A horizontal meter: dark well, neon fill with a bright leading edge, segment ticks. */
    public static void meter(GuiGraphics g, int x, int y, int w, int h, double fraction, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF04060C);
        g.fill(x, y, x + w, y + h, 0xFF141B2E);
        int filled = (int) Math.round(w * Mth.clamp(fraction, 0.0, 1.0));
        if (filled > 0) {
            g.fillGradient(x, y, x + filled, y + h, color, dim(color, 0.6f));
            g.fill(Math.max(x, x + filled - 1), y, x + filled, y + h, 0xFFFFFFFF);
        }
        for (int tx = x + w / 4; tx < x + w; tx += w / 4) {
            g.fill(tx, y, tx + 1, y + h, 0x60000000);
        }
    }

    public static int dim(int argb, float factor) {
        int a = argb >>> 24;
        int r = (int) (((argb >> 16) & 0xFF) * factor);
        int gr = (int) (((argb >> 8) & 0xFF) * factor);
        int b = (int) ((argb & 0xFF) * factor);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    /** A button in the same style: dark glass, neon edge that lights up on hover, dimmed when inactive. */
    public static final class CyberButton extends Button {
        private final int accent;

        private CyberButton(int x, int y, int w, int h, Component label, OnPress onPress, int accent) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
            this.accent = accent;
        }

        public static CyberButton of(int x, int y, int w, int h, Component label, OnPress onPress) {
            return new CyberButton(x, y, w, h, label, onPress, CYAN);
        }

        public static CyberButton of(int x, int y, int w, int h, Component label, OnPress onPress, int accent) {
            return new CyberButton(x, y, w, h, label, onPress, accent);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            int edge = !active ? 0xFF2A3350 : isHoveredOrFocused() ? accent : dim(accent, 0.6f);
            int fill = !active ? 0xC00C101C : isHoveredOrFocused() ? withAlpha(dim(accent, 0.30f), 0xE0) : PANEL;
            panel(g, getX(), getY(), width, height, edge, fill);
            if (active && isHoveredOrFocused()) {
                g.fill(getX() + 2, getY() + height - 3, getX() + width - 2, getY() + height - 2, withAlpha(accent, 0x90));
            }
            int color = !active ? 0xFF505A74 : isHoveredOrFocused() ? 0xFFFFFFFF : TEXT;
            int maxWidth = width - 8;
            Component label = getMessage();
            String shown = font.plainSubstrByWidth(label.getString(), maxWidth);
            if (shown.length() < label.getString().length()) {
                g.drawCenteredString(font, shown, getX() + width / 2, getY() + (height - 8) / 2, color);
            } else {
                g.drawCenteredString(font, label, getX() + width / 2, getY() + (height - 8) / 2, color);
            }
        }
    }

    private CyberUi() {
    }
}
