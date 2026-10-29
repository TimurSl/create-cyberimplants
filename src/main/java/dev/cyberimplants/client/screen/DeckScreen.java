package dev.cyberimplants.client.screen;

import dev.cyberimplants.client.ClientDeckState;
import dev.cyberimplants.client.CyberUi;
import dev.cyberimplants.client.CyberUi.CyberButton;
import dev.cyberimplants.network.DeckCommandPayload;
import dev.cyberimplants.network.DeckStatusPayload;
import dev.cyberimplants.network.UploadProgramPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The basic deck text editor: a program list, an editor, and Save / Load / Unload / Delete. The server compiles on
 * save and reports errors with line numbers (docs/design/03-cyberdeck.md, section 3.6; Layer 1 has the Programs tab).
 */
public class DeckScreen extends Screen {
    private static final String TEMPLATE = """
            PROGRAM "New Program"

            ON boot:
                LOG "online"
            """;
    private static final int LIST_W = 130;

    private static final int TAB_PROGRAMS = 0;
    private static final int TAB_PROCESSES = 1;
    private static final int TAB_LOG = 2;
    private static final String[] TAB_KEYS = {"programs", "processes", "log"};

    private MultiLineEditBox editor;
    private int seenVersion = -1;
    private String pendingText = TEMPLATE;
    private int tab = TAB_PROGRAMS;
    /** The thread Load targets (0 = the Processor's, 1.. = Coprocessor threads). */
    private int thread;
    private boolean requested;

    public DeckScreen() {
        super(Component.translatable("cyberimplants.deck.title"));
    }

    @Override
    protected void init() {
        if (!requested) {
            requested = true;
            PacketDistributor.sendToServer(new DeckCommandPayload(DeckCommandPayload.Op.STATUS, ""));
        }
        for (int t = 0; t < TAB_KEYS.length; t++) {
            int target = t;
            CyberButton button = CyberButton.of(10 + t * 82, 6, 80, 16, Component.translatable("cyberimplants.deck.tab." + TAB_KEYS[t]), b -> {
                if (editor != null) {
                    pendingText = editor.getValue();
                }
                tab = target;
                rebuildWidgets();
            }, CyberUi.GREEN);
            button.active = tab != t;
            addRenderableWidget(button);
        }
        addRenderableWidget(CyberButton.of(width - 70, height - 24, 60, 18, Component.translatable("gui.done"), b -> onClose(), CyberUi.MAGENTA));
        if (tab != TAB_PROGRAMS) {
            editor = null;
            return;
        }
        int editorX = 10 + LIST_W + 8;
        int editorW = width - editorX - 10;
        int editorH = Math.max(60, height - 104);

        editor = new MultiLineEditBox(font, editorX, 38, editorW, editorH,
                Component.translatable("cyberimplants.deck.placeholder"), Component.empty());
        editor.setValue(pendingText);
        addRenderableWidget(editor);
        setInitialFocus(editor);

        int by = 38 + editorH + 4;
        int bw = Math.min(90, (editorW - 12) / 4);
        addRenderableWidget(CyberButton.of(editorX, by, bw, 18, Component.translatable("cyberimplants.deck.save"),
                b -> PacketDistributor.sendToServer(new UploadProgramPayload(editor.getValue())), CyberUi.CYAN));
        addRenderableWidget(CyberButton.of(editorX + (bw + 4), by, bw, 18, Component.translatable("cyberimplants.deck.load"),
                b -> PacketDistributor.sendToServer(new DeckCommandPayload(DeckCommandPayload.Op.LOAD, editedProgramName(), thread)), CyberUi.GREEN));
        addRenderableWidget(CyberButton.of(editorX + 2 * (bw + 4), by, bw, 18, Component.translatable("cyberimplants.deck.unload"),
                b -> PacketDistributor.sendToServer(new DeckCommandPayload(DeckCommandPayload.Op.UNLOAD, "", thread)), CyberUi.AMBER));
        DeckStatusPayload status = ClientDeckState.get();
        if (status != null && status.threads() > 1) {
            thread = Math.min(thread, status.threads() - 1);
            addRenderableWidget(CyberButton.of(editorX + 4 * (bw + 4), by, bw, 18, Component.translatable("cyberimplants.deck.thread", thread),
                    b -> {
                        thread = (thread + 1) % status.threads();
                        rebuildWidgets();
                    }, CyberUi.MAGENTA));
        }
        addRenderableWidget(CyberButton.of(editorX + 3 * (bw + 4), by, bw, 18, Component.translatable("cyberimplants.deck.delete"),
                b -> command(DeckCommandPayload.Op.DELETE), CyberUi.RED));

        addProgramButtons();
        addRenderableWidget(CyberButton.of(10, 38, LIST_W, 16, Component.translatable("cyberimplants.deck.new"), b -> {
            pendingText = TEMPLATE;
            editor.setValue(TEMPLATE);
        }, CyberUi.GREEN));
    }

    private void addProgramButtons() {
        DeckStatusPayload status = ClientDeckState.get();
        if (status == null) {
            return;
        }
        int y = 58;
        for (int i = 0; i < status.names().size() && i < 14; i++) {
            String name = status.names().get(i);
            String source = status.sources().get(i);
            boolean running = name.equals(status.loaded());
            addRenderableWidget(CyberButton.of(10, y, LIST_W, 16, Component.literal((running ? "> " : "") + name), b -> {
                pendingText = source;
                editor.setValue(source);
            }, running ? CyberUi.GREEN : CyberUi.CYAN));
            y += 17;
        }
    }

    /** The program named by the first PROGRAM line of the editor, which is what the server stores it under. */
    private String editedProgramName() {
        for (String line : editor.getValue().split("\n")) {
            String trimmed = line.trim();
            if (trimmed.regionMatches(true, 0, "PROGRAM", 0, 7)) {
                int open = trimmed.indexOf('"');
                int close = trimmed.lastIndexOf('"');
                if (open >= 0 && close > open) {
                    return trimmed.substring(open + 1, close);
                }
            }
        }
        return "";
    }

    private void command(DeckCommandPayload.Op op) {
        PacketDistributor.sendToServer(new DeckCommandPayload(op, editedProgramName()));
    }

    @Override
    public void tick() {
        if (ClientDeckState.version() != seenVersion) {
            seenVersion = ClientDeckState.version();
            if (editor != null) {
                pendingText = editor.getValue();
            }
            rebuildWidgets();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        CyberUi.backdrop(g, width, height, minecraft.player == null ? 0 : minecraft.player.tickCount + partialTick);
        if (tab == TAB_PROGRAMS) {
            int editorX = 10 + LIST_W + 8;
            CyberUi.panel(g, 6, 35, LIST_W + 8, Math.max(60, height - 66), CyberUi.GREEN);
            CyberUi.panel(g, editorX - 3, 35, width - editorX - 4, Math.max(60, height - 104) + 6, CyberUi.CYAN);
        } else {
            CyberUi.panel(g, 6, 35, width - 12, Math.max(60, height - 66), CyberUi.CYAN);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        CyberUi.title(g, font, title, width - font.width(title) - 14, 8, CyberUi.CYAN);
        DeckStatusPayload status = ClientDeckState.get();
        int editorX = tab == TAB_PROGRAMS ? 10 + LIST_W + 8 : 10;
        if (status == null) {
            g.drawString(font, Component.translatable("cyberimplants.deck.waiting"), editorX, 26, CyberUi.TEXT_DIM);
            return;
        }
        String hardware = status.booted()
                ? Component.translatable("cyberimplants.deck.hardware", status.isa(), status.ramCells(),
                status.storageUsed(), status.storageBytes()).getString()
                : Component.translatable("cyberimplants.deck.not_booted").getString();
        g.drawString(font, hardware, editorX, 26, status.booted() ? CyberUi.GREEN : CyberUi.RED);
        if (tab == TAB_PROCESSES) {
            int row = 42;
            g.drawString(font, Component.translatable("cyberimplants.deck.threads", status.threads(), status.latency()), 10, row, CyberUi.CYAN);
            row += 14;
            if (status.processes().isEmpty()) {
                g.drawString(font, Component.translatable("cyberimplants.deck.idle"), 10, row, CyberUi.TEXT_DIM);
            }
            for (String line : status.processes()) {
                g.drawString(font, line, 10, row, CyberUi.TEXT);
                row += 11;
            }
            return;
        }
        if (tab == TAB_LOG) {
            List<String> lines = status.log();
            int rows = Math.max(1, (height - 60) / 10);
            int row = 42;
            for (int i = Math.max(0, lines.size() - rows); i < lines.size(); i++) {
                g.drawString(font, lines.get(i), 10, row, CyberUi.CYAN);
                row += 10;
            }
            return;
        }

        int y = Math.max(60, height - 104) + 38 + 24;
        if (!status.message().isEmpty()) {
            g.drawString(font, status.message(), editorX, y, CyberUi.AMBER);
            y += 11;
        }
        List<String> log = status.log();
        for (int i = Math.max(0, log.size() - 3); i < log.size(); i++) {
            g.drawString(font, "> " + log.get(i), editorX, y, CyberUi.CYAN);
            y += 10;
        }
        if (!status.loaded().isEmpty()) {
            g.drawString(font, Component.translatable("cyberimplants.deck.running", status.loaded(), status.footprint()),
                    10, height - 20, CyberUi.GREEN);
        }
    }
}
