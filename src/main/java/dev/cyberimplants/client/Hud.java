package dev.cyberimplants.client;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.config.ClientConfig;
import dev.cyberimplants.body.Strain;
import dev.cyberimplants.implant.Arms;
import dev.cyberimplants.implant.ImplantActions;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.implant.limbs.BallisticModule;
import dev.cyberimplants.implant.mobility.MomentumBooster;
import dev.cyberimplants.implant.subdermal.KineticShield;
import dev.cyberimplants.network.ScanResultPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.List;

/** The Charge bar, status bars, Biomonitor vitals, scan readout and the Thermal Vision colour wash. */
final class Hud {
    private static final int FRAME = 0xFF101418;
    private static final int CHARGE = 0xFF35D0FF;
    private static final int BUFFER = 0xFFFFB020;
    private static final int SHIELD = 0xFF7AF0A0;
    private static final int MOMENTUM = 0xFFE070FF;
    private static final int TEXT = 0xFFE8F4FF;

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null && mc.screen.isPauseScreen()) {
            return;
        }
        BodyState body = ClientBodyState.get();
        if (body.installed().isEmpty()) {
            return;
        }
        Font font = mc.font;
        int height = g.guiHeight();
        int width = g.guiWidth();

        thermalWash(g, player, width, height);

        // Charge bar (bottom left), with the Energy Buffer as a separate amber segment.
        double max = Body.maxCharge(body);
        int y = height - 14;
        if (max > 0) {
            bar(g, 6, y, 90, 6, body.charge() / max, CHARGE);
            double bufferCap = Body.bufferCapacity(body);
            if (bufferCap > 0) {
                bar(g, 98, y, 30, 6, body.buffer() / bufferCap, BUFFER);
            }
            g.drawString(font, "C " + Math.round(body.charge()) + "/" + Math.round(max), 6, y - 10, TEXT);
            y -= 18;
        }
        if (body.has(ImplantIds.KINETIC_SHIELD)) {
            double cap = KineticShield.max(player);
            bar(g, 6, y, 90, 4, cap <= 0 ? 0 : KineticShield.value(player) / cap, SHIELD);
            y -= 8;
        }
        if (body.has(ImplantIds.MOMENTUM_BOOSTER)) {
            bar(g, 6, y, 90, 4, MomentumBooster.value(player) / 100.0, MOMENTUM);
            y -= 8;
        }
        if (Arms.freeHandWith(player, ImplantIds.BALLISTIC_MODULE) != null
                || body.installed().values().stream().anyMatch(i -> i.type().equals(ImplantIds.BALLISTIC_MODULE))) {
            g.drawString(font, Component.translatable("cyberimplants.hud.magazine", BallisticModule.magazine(player)), 6, y - 6, TEXT);
        }

        cooldownStrip(g, font, body, width, height);
        vitals(g, font, player, body, width);
        scanReadout(g, font, mc, player);
    }

    /** One entry per Active or Toggle implant that is cooling down (or knocked offline by Strain): name, bar, seconds. */
    private static void cooldownStrip(GuiGraphics g, Font font, BodyState body, int width, int height) {
        int x = width / 2 - 91;
        int y = height - 52;
        for (ImplantCatalog.Entry e : ImplantCatalog.ALL) {
            if (!body.has(e.id()) || !ImplantActions.wheelEligible(e.id())) {
                continue;
            }
            boolean offline = Strain.offline(Minecraft.getInstance().player, e.id());
            long remaining = body.cooldownRemaining(e.id());
            if (remaining <= 0 && !offline) {
                continue;
            }
            Component name = Component.translatable("item." + e.id().getNamespace() + "." + e.id().getPath());
            String label = name.getString();
            label = (label.length() > 4 ? label.substring(0, 4) : label) + " " + (offline ? "!!" : (remaining + 19) / 20 + "s");
            g.drawString(font, label, x, y - 9, offline ? 0xFFFF6060 : TEXT);
            long total = Math.max(1, Body.cooldownTicksFor(body, e.id()));
            bar(g, x, y, 34, 3, offline ? 1.0 : 1.0 - (double) remaining / total, offline ? 0xFFFF6060 : 0xFF8090A0);
            x += 40;
            if (x > width / 2 + 91) {
                x = width / 2 - 91;
                y -= 16;
            }
        }
    }

    private static void thermalWash(GuiGraphics g, LocalPlayer player, int width, int height) {
        if (Toggles.isOn(player, ImplantIds.THERMAL_VISION) && !ClientConfig.REDUCE_SCREEN_EFFECTS.get()) {
            int alpha = (int) (ClientConfig.OVERLAY_OPACITY.get() * 0x38);
            g.fill(0, 0, width, height, (alpha << 24) | 0xFF6A00);
        }
    }

    private static void vitals(GuiGraphics g, Font font, LocalPlayer player, BodyState body, int width) {
        if (!body.has(ImplantIds.BIOMONITOR)) {
            return;
        }
        String[] lines = {
                String.format("HP  %.1f / %.1f", player.getHealth(), player.getMaxHealth()),
                String.format("FOOD %d  SAT %.1f", player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel())};
        int y = 6;
        for (String line : lines) {
            g.drawString(font, line, width - font.width(line) - 6, y, 0xFFFF8A8A);
            y += 10;
        }
    }

    private static void scanReadout(GuiGraphics g, Font font, Minecraft mc, LocalPlayer player) {
        List<ScanResultPayload.Marker> markers = ClientOptics.scanMarkers();
        int y = 6;
        if (!markers.isEmpty()) {
            g.drawString(font, Component.translatable("cyberimplants.hud.scan"), 6, y, TEXT);
            y += 10;
            for (ScanResultPayload.Marker m : markers) {
                String name = m.type().contains(":") ? m.type().substring(m.type().indexOf(':') + 1) : m.type();
                g.drawString(font, String.format("%s  [%s]  %.0fm", name, m.kind(), m.distance()), 6, y, kindColor(m.kind()));
                y += 9;
            }
        }
        int tracked = ClientOptics.trackedId();
        if (tracked >= 0 && ClientBodyState.get().has(ImplantIds.TARGET_TRACKING) && mc.level != null) {
            Entity e = mc.level.getEntity(tracked);
            if (e != null) {
                g.drawString(font, String.format("TRACKING  %s  %.0fm", e.getType().getDescription().getString(),
                        e.distanceTo(player)), 6, y + 4, 0xFFFFD060);
            }
        }
    }

    private static int kindColor(String kind) {
        return switch (kind) {
            case "hostile" -> 0xFFFF6060;
            case "boss" -> 0xFFFF40FF;
            case "neutral" -> 0xFFFFD060;
            case "player" -> 0xFF60C0FF;
            default -> 0xFF80FF80;
        };
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, FRAME);
        int filled = (int) Math.round(w * Math.max(0.0, Math.min(1.0, fraction)));
        g.fill(x, y, x + filled, y + h, color);
    }

    private Hud() {
    }
}
