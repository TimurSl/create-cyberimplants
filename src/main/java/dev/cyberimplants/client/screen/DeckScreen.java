package dev.cyberimplants.client.screen;

import dev.cyberimplants.client.ClientDeckState;
import dev.cyberimplants.network.DeckCommandPayload;
import dev.cyberimplants.network.DeckStatusPayload;
import dev.cyberimplants.network.UploadProgramPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    private MultiLineEditBox editor;
    private int seenVersion = -1;
    private String pendingText = TEMPLATE;

    public DeckScreen() {
        super(Component.translatable("cyberimplants.deck.title"));
    }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(new DeckCommandPayload(DeckCommandPayload.Op.STATUS, ""));
        int editorX = 10 + LIST_W + 8;
        int editorW = width - editorX - 10;
        int editorH = Math.max(60, height - 96);

        editor = new MultiLineEditBox(font, editorX, 30, editorW, editorH,
                Component.translatable("cyberimplants.deck.placeholder"), Component.empty());
        editor.setValue(pendingText);
        addRenderableWidget(editor);
        setInitialFocus(editor);

        int by = 30 + editorH + 4;
        int bw = Math.min(90, (editorW - 12) / 4);
        addRenderableWidget(Button.builder(Component.translatable("cyberimplants.deck.save"),
                b -> PacketDistributor.sendToServer(new UploadProgramPayload(editor.getValue())))
                .bounds(editorX, by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("cyberimplants.deck.load"),
                b -> command(DeckCommandPayload.Op.LOAD)).bounds(editorX + (bw + 4), by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("cyberimplants.deck.unload"),
                b -> PacketDistributor.sendToServer(new DeckCommandPayload(DeckCommandPayload.Op.UNLOAD, "")))
                .bounds(editorX + 2 * (bw + 4), by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("cyberimplants.deck.delete"),
                b -> command(DeckCommandPayload.Op.DELETE)).bounds(editorX + 3 * (bw + 4), by, bw, 18).build());

        addProgramButtons();
        addRenderableWidget(Button.builder(Component.translatable("cyberimplants.deck.new"), b -> {
            pendingText = TEMPLATE;
            editor.setValue(TEMPLATE);
        }).bounds(10, 30, LIST_W, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width - 70, height - 24, 60, 18).build());
    }

    private void addProgramButtons() {
        DeckStatusPayload status = ClientDeckState.get();
        if (status == null) {
            return;
        }
        int y = 50;
        for (int i = 0; i < status.names().size() && i < 14; i++) {
            String name = status.names().get(i);
            String source = status.sources().get(i);
            boolean running = name.equals(status.loaded());
            addRenderableWidget(Button.builder(Component.literal((running ? "> " : "") + name), b -> {
                pendingText = source;
                editor.setValue(source);
            }).bounds(10, y, LIST_W, 16).build());
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
            pendingText = editor.getValue();
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFFFF);
        DeckStatusPayload status = ClientDeckState.get();
        int editorX = 10 + LIST_W + 8;
        if (status == null) {
            g.drawString(font, Component.translatable("cyberimplants.deck.waiting"), editorX, 18, 0xFFAAAAAA);
            return;
        }
        String hardware = status.booted()
                ? Component.translatable("cyberimplants.deck.hardware", status.isa(), status.ramCells(),
                status.storageUsed(), status.storageBytes()).getString()
                : Component.translatable("cyberimplants.deck.not_booted").getString();
        g.drawString(font, hardware, editorX, 18, status.booted() ? 0xFF90FFB0 : 0xFFFF9090);

        int y = Math.max(60, height - 96) + 30 + 24;
        if (!status.message().isEmpty()) {
            g.drawString(font, status.message(), editorX, y, 0xFFFFE080);
            y += 11;
        }
        List<String> log = status.log();
        for (int i = Math.max(0, log.size() - 3); i < log.size(); i++) {
            g.drawString(font, "> " + log.get(i), editorX, y, 0xFFB0C8FF);
            y += 10;
        }
        if (!status.loaded().isEmpty()) {
            g.drawString(font, Component.translatable("cyberimplants.deck.running", status.loaded(), status.footprint()),
                    10, height - 20, 0xFF90FFB0);
        }
    }
}
