package dev.cyberimplants.client.screen;

import dev.cyberimplants.client.CyberUi;
import dev.cyberimplants.client.CyberUi.CyberButton;
import dev.cyberimplants.client.ModKeys;
import dev.cyberimplants.client.ui.Anim;
import dev.cyberimplants.client.ui.UiSounds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Field Manual: a chapter list and a scrolling text page. All text lives in the language files
 * ({@code cyberimplants.manual.<chapter>.title} and {@code .body}), so translating the mod translates the guide.
 *
 * <p>A body is a string of paragraphs separated by {@code \n}. A paragraph starting with {@code "# "} is a heading,
 * {@code "- "} a bullet, {@code "> "} a tip, {@code "! "} a warning and {@code "|"} a line of Synapse code (not
 * wrapped). {@code {key:wheel}} is replaced by the player's current binding of that key.
 */
public class ManualScreen extends Screen {
    /** Chapter ids in reading order; each needs a title and a body key. */
    public static final List<String> CHAPTERS = List.of(
            "start", "body", "charge", "rig", "controls", "tiers", "catalog",
            "deck_build", "synapse", "synapse_ref", "machines", "tips");

    private static final Pattern KEY_TOKEN = Pattern.compile("\\{key:([a-z_0-9]+)}");
    private static final int LIST_W = 128;
    private static final int LINE_H = 10;

    private enum Kind { TEXT, HEADING, BULLET, TIP, WARN, CODE, GAP }

    private record Line(Kind kind, FormattedCharSequence text, String raw) {
    }

    /** Remembered between openings, so the manual reopens where the player stopped. */
    private static int lastChapter;
    private static int lastScroll;

    private int chapter;
    private int scroll;
    /** When the current chapter was opened, for the page-turn slide. */
    private float chapterAt = Anim.now();
    private final List<Line> lines = new ArrayList<>();

    public ManualScreen() {
        super(Component.translatable("cyberimplants.manual.title"));
        chapter = Mth.clamp(lastChapter, 0, CHAPTERS.size() - 1);
        scroll = lastScroll;
    }

    @Override
    protected void init() {
        int y = 30;
        for (int i = 0; i < CHAPTERS.size(); i++) {
            int target = i;
            Component label = Component.literal((i + 1) + ". ").append(title(i));
            addRenderableWidget(CyberButton.of(10, y, LIST_W, 14, label, b -> {
                openChapter(target);
            }, i == chapter ? CyberUi.MAGENTA : CyberUi.CYAN));
            y += 15;
        }
        int bottom = height - 26;
        addRenderableWidget(CyberButton.of(10, bottom, 62, 18, Component.translatable("cyberimplants.manual.prev"),
                b -> openChapter(chapter - 1)));
        addRenderableWidget(CyberButton.of(10 + 66, bottom, 62, 18, Component.translatable("cyberimplants.manual.next"),
                b -> openChapter(chapter + 1)));
        addRenderableWidget(CyberButton.of(width - 90, bottom, 80, 18, Component.translatable("gui.done"),
                b -> onClose(), CyberUi.MAGENTA));
        layoutText();
    }

    private static Component title(int chapter) {
        return Component.translatable("cyberimplants.manual." + CHAPTERS.get(chapter) + ".title");
    }

    private void openChapter(int target) {
        int next = Mth.clamp(target, 0, CHAPTERS.size() - 1);
        if (next != chapter) {
            chapter = next;
            scroll = 0;
            chapterAt = Anim.now();
            UiSounds.select();
            rebuildWidgets();
        }
    }

    private int pageLeft() {
        return LIST_W + 22;
    }

    private int pageWidth() {
        return width - pageLeft() - 12;
    }

    private int pageTop() {
        return 34;
    }

    private int pageBottom() {
        return height - 32;
    }

    private int visibleLines() {
        return Math.max(1, (pageBottom() - pageTop() - 8) / LINE_H);
    }

    private void layoutText() {
        lines.clear();
        Font font = minecraft.font;
        int wrap = pageWidth() - 22;
        String body = replaceKeys(Component.translatable("cyberimplants.manual." + CHAPTERS.get(chapter) + ".body").getString());
        for (String paragraph : body.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add(new Line(Kind.GAP, FormattedCharSequence.EMPTY, ""));
            } else if (paragraph.startsWith("|")) {
                lines.add(new Line(Kind.CODE, null, paragraph.substring(1)));
            } else {
                Kind kind = Kind.TEXT;
                String text = paragraph;
                if (paragraph.startsWith("# ")) {
                    kind = Kind.HEADING;
                    text = paragraph.substring(2);
                } else if (paragraph.startsWith("- ")) {
                    kind = Kind.BULLET;
                    text = "• " + paragraph.substring(2);
                } else if (paragraph.startsWith("> ")) {
                    kind = Kind.TIP;
                    text = paragraph.substring(2);
                } else if (paragraph.startsWith("! ")) {
                    kind = Kind.WARN;
                    text = paragraph.substring(2);
                }
                for (FormattedCharSequence part : font.split(Component.literal(text), wrap)) {
                    lines.add(new Line(kind, part, text));
                }
            }
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private static String replaceKeys(String text) {
        Matcher m = KEY_TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(keyName(m.group(1))));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String keyName(String id) {
        Map<String, KeyMapping> keys = Map.of("wheel", ModKeys.WHEEL, "dodge", ModKeys.DODGE,
                "fire_ballistic", ModKeys.FIRE_BALLISTIC, "deck", ModKeys.DECK, "manual", ModKeys.MANUAL);
        KeyMapping key = keys.get(id);
        return key == null ? id : "[" + key.getTranslatedKeyMessage().getString() + "]";
    }

    private int maxScroll() {
        return Math.max(0, lines.size() - visibleLines());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * 3, 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 264 -> scroll = Math.min(maxScroll(), scroll + 1);          // down
            case 265 -> scroll = Math.max(0, scroll - 1);                    // up
            case 267 -> scroll = Math.min(maxScroll(), scroll + visibleLines() - 1); // page down
            case 266 -> scroll = Math.max(0, scroll - visibleLines() + 1);   // page up
            case 263 -> openChapter(chapter - 1);                            // left
            case 262 -> openChapter(chapter + 1);                            // right
            default -> {
                if (ModKeys.MANUAL.matches(keyCode, scanCode)) {
                    onClose();
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public void onClose() {
        lastChapter = chapter;
        lastScroll = scroll;
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Replaces the vanilla blur, which {@link Screen#render} would otherwise draw over the page. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        CyberUi.backdrop(g, width, height, (minecraft.level == null ? 0 : minecraft.level.getGameTime()) + partialTick);
        CyberUi.panel(g, pageLeft() - 6, pageTop() - 4, pageWidth() + 12, pageBottom() - pageTop() + 8, CyberUi.CYAN);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        CyberUi.title(g, font, title, 10, 10, CyberUi.CYAN);
        int left = pageLeft();
        int top = pageTop();
        g.drawString(font, Component.literal((chapter + 1) + "/" + CHAPTERS.size() + "  ").append(title(chapter)),
                left, top - 20, CyberUi.MAGENTA, true);

        g.enableScissor(left - 4, top, left + pageWidth() + 4, pageBottom() - 2);
        // Page turn: the new chapter slides in from the right and fades up.
        float turn = Anim.easeOutCubic(Anim.progress(chapterAt, 0.28f));
        g.pose().pushPose();
        g.pose().translate((1 - turn) * 14, 0, 0);
        g.setColor(1f, 1f, 1f, Math.max(0.05f, turn));
        int y = top + 2;
        int end = Math.min(lines.size(), scroll + visibleLines());
        for (int i = scroll; i < end; i++) {
            Line line = lines.get(i);
            switch (line.kind()) {
                case GAP -> {
                    // blank line
                }
                case HEADING -> {
                    g.drawString(font, line.text(), left, y, CyberUi.CYAN, true);
                }
                case BULLET -> g.drawString(font, line.text(), left + 4, y, CyberUi.TEXT, false);
                case TIP -> {
                    g.fill(left - 2, y - 1, left - 1, y + 9, CyberUi.AMBER);
                    g.drawString(font, line.text(), left + 4, y, CyberUi.AMBER, false);
                }
                case WARN -> {
                    g.fill(left - 2, y - 1, left - 1, y + 9, CyberUi.RED);
                    g.drawString(font, line.text(), left + 4, y, CyberUi.RED, false);
                }
                case CODE -> {
                    g.fill(left - 1, y - 1, left + pageWidth() - 18, y + 9, 0xC0060912);
                    g.drawString(font, font.plainSubstrByWidth(line.raw(), pageWidth() - 28), left + 3, y, CyberUi.GREEN, false);
                }
                default -> g.drawString(font, line.text(), left, y, CyberUi.TEXT, false);
            }
            y += line.kind() == Kind.GAP ? LINE_H / 2 + 1 : LINE_H;
        }
        g.setColor(1f, 1f, 1f, 1f);
        g.pose().popPose();
        g.disableScissor();

        // The scroll bar: a thin meter on the right of the page.
        if (maxScroll() > 0) {
            int barX = left + pageWidth() - 4;
            int barH = pageBottom() - top - 8;
            g.fill(barX, top + 2, barX + 2, top + 2 + barH, 0xFF141B2E);
            int thumb = Math.max(12, barH * visibleLines() / lines.size());
            int thumbY = top + 2 + (barH - thumb) * scroll / maxScroll();
            g.fill(barX, thumbY, barX + 2, thumbY + thumb, CyberUi.CYAN);
        }
    }
}
