package dev.cyberimplants.client;

import dev.cyberimplants.client.ui.Anim;
import dev.cyberimplants.client.ui.UiSounds;
import dev.cyberimplants.implant.ImplantCategory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

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

    /** Neon colour of an implant category, the same as its item icons. */
    public static int categoryColor(ImplantCategory category) {
        return switch (category) {
            case CORE -> 0xFFE6F4FF;
            case NEURAL -> 0xFFB45CFF;
            case CYBERDECK -> 0xFF39FF88;
            case SKELETAL -> 0xFFFFB830;
            case LIMBS -> 0xFFFF5A2A;
            case OPTICAL -> 0xFF19D9FF;
            case CIRCULATORY -> 0xFFFF2A6D;
            case SUBDERMAL -> 0xFF8FB4E0;
            case MOBILITY -> 0xFFF5F54A;
        };
    }

    /** The full-screen backdrop: a dark gradient, a grid and slow scanlines. {@code time} is in ticks. */
    public static void backdrop(GuiGraphics g, int width, int height, float time) {
        g.fillGradient(0, 0, width, height, 0xF0060912, 0xF0110A24);
        // Vignette: darker towards the edges so the panels pop.
        g.fillGradient(0, 0, width, height / 4, 0x60000000, 0x00000000);
        g.fillGradient(0, height - height / 4, width, height, 0x00000000, 0x70000000);
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
    public static void title(GuiGraphics g, Font font, Component title, int x, int y, int accent) {
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

    /** Five pips for Tiers I-V; the first {@code tier} are lit. Returns the width drawn. */
    public static int tierPips(GuiGraphics g, int x, int y, int tier, int color) {
        for (int i = 0; i < 5; i++) {
            int px = x + i * 4;
            g.fill(px, y, px + 3, y + 3, i < tier ? color : 0xFF243048);
            if (i < tier) {
                g.fill(px, y, px + 3, y + 1, 0x60FFFFFF);
            }
        }
        return 19;
    }

    /** A small tag: text on a tinted plate with a coloured left edge. Returns its width. */
    public static int chip(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        int w = font.width(text) + 8;
        g.fill(x, y, x + w, y + 11, withAlpha(dim(color, 0.25f), 0xD0));
        g.fill(x, y, x + 1, y + 11, color);
        g.drawString(font, text, x + 5, y + 2, color, false);
        return w;
    }

    /** Clipped bracket corners around a box, for focus and selection. */
    public static void corners(GuiGraphics g, int x, int y, int w, int h, int len, int color) {
        g.fill(x, y, x + len, y + 1, color);
        g.fill(x, y, x + 1, y + len, color);
        g.fill(x + w - len, y, x + w, y + 1, color);
        g.fill(x + w - 1, y, x + w, y + len, color);
        g.fill(x, y + h - 1, x + len, y + h, color);
        g.fill(x, y + h - len, x + 1, y + h, color);
        g.fill(x + w - len, y + h - 1, x + w, y + h, color);
        g.fill(x + w - 1, y + h - len, x + w, y + h, color);
    }

    /**
     * A tooltip in the mod's style, kept on screen. The first line is the heading (drawn in {@code accent}); long lines
     * wrap at {@code maxWidth}.
     */
    public static void tooltip(GuiGraphics g, Font font, List<Component> lines, int mouseX, int mouseY, int accent, int maxWidth) {
        tooltip(g, font, lines, mouseX, mouseY, accent, maxWidth, false);
    }

    /** As above; {@code preferLeft} opens it to the left of the point when there is room. */
    public static void tooltip(GuiGraphics g, Font font, List<Component> lines, int mouseX, int mouseY, int accent, int maxWidth,
                               boolean preferLeft) {
        if (lines.isEmpty()) {
            return;
        }
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        List<Boolean> heading = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            for (FormattedCharSequence part : font.split(lines.get(i), maxWidth)) {
                wrapped.add(part);
                heading.add(i == 0);
            }
        }
        int w = 0;
        for (FormattedCharSequence part : wrapped) {
            w = Math.max(w, font.width(part));
        }
        w += 10;
        int h = wrapped.size() * 10 + 6;
        int x = preferLeft ? mouseX - w - 8 : mouseX + 12;
        int y = mouseY - 4;
        if (preferLeft && x < 2) {
            x = mouseX + 12;
        } else if (!preferLeft && x + w > g.guiWidth() - 2) {
            x = mouseX - w - 8;
        }
        y = Mth.clamp(y, 2, Math.max(2, g.guiHeight() - h - 2));
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        panel(g, x, y, w, h, accent, 0xF00A0E18);
        for (int i = 0; i < wrapped.size(); i++) {
            g.drawString(font, wrapped.get(i), x + 5, y + 4 + i * 10, heading.get(i) ? accent : TEXT, heading.get(i));
        }
        g.pose().popPose();
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
        private int accent;
        private final Anim.Smooth glow = new Anim.Smooth(0, 14);
        private boolean wasHovered;

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

        public void setAccent(int accent) {
            this.accent = accent;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            boolean hot = active && isHoveredOrFocused();
            if (hot && isHovered() && !wasHovered) {
                UiSounds.hover();
            }
            wasHovered = isHovered();
            float k = glow.update(hot ? 1f : 0f);
            int edge = !active ? 0xFF2A3350 : Anim.mix(dim(accent, 0.6f), accent, k);
            int fill = !active ? 0xC00C101C : Anim.mix(PANEL, withAlpha(dim(accent, 0.30f), 0xE0), k);
            panel(g, getX(), getY(), width, height, edge, fill);
            if (active && k > 0.01f) {
                // An underline that grows from the centre, and a sheen that runs across once on hover.
                int half = (int) ((width - 4) / 2f * k);
                int mid = getX() + width / 2;
                g.fill(mid - half, getY() + height - 3, mid + half, getY() + height - 2, withAlpha(accent, (int) (0x90 * k)));
                int sheen = getX() + (int) ((width + 20) * k) - 10;
                if (k < 0.98f) {
                    g.fill(Math.max(getX() + 1, sheen - 4), getY() + 1, Math.min(getX() + width - 1, sheen), getY() + height - 1, 0x30FFFFFF);
                }
            }
            int color = !active ? 0xFF505A74 : hot ? 0xFFFFFFFF : TEXT;
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
