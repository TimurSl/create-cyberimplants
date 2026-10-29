package dev.cyberimplants.client;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.config.ClientConfig;
import dev.cyberimplants.body.Strain;
import dev.cyberimplants.client.ui.Anim;
import dev.cyberimplants.compat.create.block.RigView;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ActivationType;
import dev.cyberimplants.implant.Appearance;
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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The implant HUD. A status panel (Charge, Energy Buffer, Kinetic Shield, Momentum, then what is running: blade, ammo,
 * Sandevistan, camouflage, toggles, then key hints) anchored to a configurable corner, cooldowns above the hotbar,
 * Biomonitor vitals, the scan readout and the Thermal Vision colour wash. Layout and contents follow the {@code hud}
 * section of the client config.
 */
final class Hud {
    private static final int CHARGE = CyberUi.CYAN;
    private static final int BUFFER = CyberUi.AMBER;
    private static final int SHIELD = CyberUi.GREEN;
    private static final int MOMENTUM = CyberUi.MAGENTA;
    private static final int TEXT = CyberUi.TEXT;
    private static final int BAR_W = 90;
    private static final int PAD = 4;

    // Displayed values glide towards the real ones, so bars do not jump between syncs.
    private static final Anim.Smooth CHARGE_SHOWN = new Anim.Smooth(0, 8);
    private static final Anim.Smooth BUFFER_SHOWN = new Anim.Smooth(0, 8);
    private static final Anim.Smooth SHIELD_SHOWN = new Anim.Smooth(0, 10);
    private static final Anim.Smooth MOMENTUM_SHOWN = new Anim.Smooth(0, 10);

    /** One line of the status panel: a label, a value on the right and optionally a bar under them. */
    private record Row(Component label, String value, int color, float bar, int barColor) {
        static Row text(Component label, String value, int color) {
            return new Row(label, value, color, -1, 0);
        }

        static Row bar(Component label, String value, int color, float fraction) {
            return new Row(label, value, TEXT, fraction, color);
        }

        int height() {
            return bar >= 0 ? 14 : 10;
        }
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null && mc.screen.isPauseScreen()) {
            return;
        }
        Font font = mc.font;
        int height = g.guiHeight();
        int width = g.guiWidth();
        rigOperation(g, font, width, height); // also during the very first operation, on an empty body
        BodyState body = ClientBodyState.get();
        if (body.installed().isEmpty()) {
            return;
        }

        thermalWash(g, player, width, height);
        if (!ClientConfig.get(ClientConfig.HUD_ENABLED)) {
            return;
        }

        float scale = ClientConfig.get(ClientConfig.HUD_SCALE).floatValue();
        int w = Math.round(width / scale);
        int h = Math.round(height / scale);
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1);
        statusPanel(g, font, player, body, w, h);
        if (ClientConfig.get(ClientConfig.HUD_COOLDOWNS)) {
            cooldownStrip(g, font, body, w, h);
        }
        if (ClientConfig.get(ClientConfig.HUD_VITALS)) {
            vitals(g, font, player, body, w);
        }
        if (ClientConfig.get(ClientConfig.HUD_SCAN)) {
            scanReadout(g, font, mc, player);
        }
        g.pose().popPose();
    }

    private static void statusPanel(GuiGraphics g, Font font, LocalPlayer player, BodyState body, int width, int height) {
        boolean labels = ClientConfig.get(ClientConfig.HUD_LABELS);
        List<Row> rows = new ArrayList<>();

        double max = Body.maxCharge(body);
        if (max > 0) {
            double fraction = body.charge() / max;
            // Below 15% the readout pulses red.
            int color = fraction < 0.15 ? Anim.mix(CyberUi.RED, CHARGE, Anim.pulse(0.9f)) : CHARGE;
            rows.add(Row.bar(Component.translatable("cyberimplants.hud.charge"), Math.round(body.charge()) + "/" + Math.round(max),
                    color, CHARGE_SHOWN.update((float) fraction)));
            double bufferCap = Body.bufferCapacity(body);
            if (bufferCap > 0) {
                rows.add(Row.bar(Component.translatable("cyberimplants.hud.buffer"), Math.round(body.buffer()) + "/" + Math.round(bufferCap),
                        BUFFER, BUFFER_SHOWN.update((float) (body.buffer() / bufferCap))));
            }
        }
        if (body.has(ImplantIds.KINETIC_SHIELD)) {
            double cap = KineticShield.max(player);
            double value = KineticShield.value(player);
            rows.add(Row.bar(Component.translatable("cyberimplants.hud.shield"), Math.round(value) + "/" + Math.round(cap), SHIELD,
                    SHIELD_SHOWN.update(cap <= 0 ? 0f : (float) (value / cap))));
        }
        if (body.has(ImplantIds.MOMENTUM_BOOSTER)) {
            double momentum = MomentumBooster.value(player);
            rows.add(Row.bar(Component.translatable("cyberimplants.hud.momentum"), Math.round(momentum) + "%", MOMENTUM,
                    MOMENTUM_SHOWN.update((float) (momentum / 100.0))));
        }
        if (ClientConfig.get(ClientConfig.HUD_STATUS)) {
            status(rows, player, body);
        }
        if (ClientConfig.get(ClientConfig.HUD_KEY_HINTS)) {
            hints(rows, body);
        }
        if (rows.isEmpty()) {
            return;
        }

        int panelW = BAR_W + PAD * 2;
        for (Row row : rows) {
            if (labels || row.bar < 0) {
                panelW = Math.max(panelW, font.width(row.label) + font.width(row.value) + 8 + PAD * 2);
            }
        }
        int panelH = PAD * 2 - 2;
        for (Row row : rows) {
            panelH += labels || row.bar < 0 ? row.height() : 6;
        }
        ClientConfig.Corner corner = ClientConfig.get(ClientConfig.HUD_CORNER);
        int ox = ClientConfig.get(ClientConfig.HUD_OFFSET_X);
        int oy = ClientConfig.get(ClientConfig.HUD_OFFSET_Y);
        int x = corner.right() ? width - ox - panelW : ox;
        int y = corner.bottom() ? height - oy - panelH : oy;
        x = Math.max(0, Math.min(x, width - panelW));
        y = Math.max(0, Math.min(y, height - panelH));
        CyberUi.panel(g, x, y, panelW, panelH, CyberUi.dim(CHARGE, 0.6f), 0x900A0E18);

        int cy = y + PAD;
        for (Row row : rows) {
            if (row.bar >= 0 && !labels) {
                bar(g, x + PAD, cy, panelW - PAD * 2, 3, row.bar, row.barColor);
                cy += 6;
                continue;
            }
            g.drawString(font, row.label, x + PAD, cy, row.bar >= 0 ? row.barColor : row.color, false);
            if (!row.value.isEmpty()) {
                g.drawString(font, row.value, x + panelW - PAD - font.width(row.value), cy, row.color, false);
            }
            if (row.bar >= 0) {
                bar(g, x + PAD, cy + 9, panelW - PAD * 2, 3, row.bar, row.barColor);
            }
            cy += row.height();
        }
    }

    /** What the player's hardware is doing right now, in words. */
    private static void status(List<Row> rows, LocalPlayer player, BodyState body) {
        int bladeTier = Arms.anyModuleTier(body, ImplantIds.RETRACTABLE_BLADE);
        if (bladeTier > 0) {
            if (!Toggles.isOn(player, ImplantIds.RETRACTABLE_BLADE)) {
                rows.add(Row.text(Component.translatable("cyberimplants.hud.blade"),
                        Component.translatable("cyberimplants.hud.blade.stowed").getString(), CyberUi.TEXT_DIM));
            } else if (Arms.bladeArmed(player)) {
                long damage = Math.round(Stats.of(ImplantIds.RETRACTABLE_BLADE, bladeTier).value("damage", 6.0));
                rows.add(Row.text(Component.translatable("cyberimplants.hud.blade"),
                        Component.translatable("cyberimplants.hud.blade.armed", damage).getString(), CyberUi.GREEN));
            } else {
                rows.add(Row.text(Component.translatable("cyberimplants.hud.blade"),
                        Component.translatable("cyberimplants.hud.blade.hand_full").getString(),
                        Anim.mix(CyberUi.AMBER, CyberUi.RED, Anim.pulse(1.2f))));
            }
        }
        int ballisticTier = Arms.anyModuleTier(body, ImplantIds.BALLISTIC_MODULE);
        if (ballisticTier > 0) {
            int capacity = (int) Stats.of(ImplantIds.BALLISTIC_MODULE, ballisticTier).value("magazine", 4);
            int loaded = BallisticModule.magazine(player);
            String value;
            int color;
            if (Arms.freeHandWith(player, ImplantIds.BALLISTIC_MODULE) == null) {
                value = Component.translatable("cyberimplants.hud.ammo.hand_full", loaded, capacity).getString();
                color = CyberUi.TEXT_DIM;
            } else if (loaded <= 0) {
                value = Component.translatable("cyberimplants.hud.ammo.empty").getString();
                color = CyberUi.AMBER;
            } else {
                value = loaded + "/" + capacity;
                color = TEXT;
            }
            rows.add(Row.text(Component.translatable("cyberimplants.hud.ammo"), value, color));
        }
        if (ClientAppearance.has(player, Appearance.SANDEVISTAN)) {
            rows.add(Row.text(Component.translatable("item.cyberimplants.sandevistan"),
                    Component.translatable("cyberimplants.hud.active").getString(), Anim.mix(CyberUi.GREEN, TEXT, Anim.pulse(0.6f))));
        }
        if (body.has(ImplantIds.OPTICAL_CAMOUFLAGE) && player.hasEffect(MobEffects.INVISIBILITY)) {
            rows.add(Row.text(Component.translatable("item.cyberimplants.optical_camouflage"),
                    Component.translatable("cyberimplants.hud.active").getString(), CyberUi.CYAN));
        }
        for (ImplantCatalog.Entry e : ImplantCatalog.ALL) {
            if (e.type().activation() == ActivationType.TOGGLE && !e.id().equals(ImplantIds.RETRACTABLE_BLADE)
                    && Toggles.isOn(player, e.id())) {
                rows.add(Row.text(Component.translatable("item." + e.id().getNamespace() + "." + e.id().getPath()),
                        Component.translatable("cyberimplants.hud.on").getString(), CyberUi.categoryColor(e.type().category())));
            }
        }
    }

    private static void hints(List<Row> rows, BodyState body) {
        if (ClientInput.wheelEntries().isEmpty()) {
            return;
        }
        rows.add(Row.text(Component.translatable("cyberimplants.hud.hint.wheel"), keyName(ModKeys.WHEEL), CyberUi.TEXT_DIM));
        if (Arms.anyModuleTier(body, ImplantIds.BALLISTIC_MODULE) > 0) {
            rows.add(Row.text(Component.translatable("cyberimplants.hud.hint.fire"), keyName(ModKeys.FIRE_BALLISTIC), CyberUi.TEXT_DIM));
        }
        if (body.has(ImplantIds.KERENZIKOV)) {
            rows.add(Row.text(Component.translatable("cyberimplants.hud.hint.dodge"), keyName(ModKeys.DODGE), CyberUi.TEXT_DIM));
        }
        rows.add(Row.text(Component.translatable("cyberimplants.hud.hint.manual"), keyName(ModKeys.MANUAL), CyberUi.TEXT_DIM));
    }

    private static String keyName(KeyMapping key) {
        return key.isUnbound() ? "-" : "[" + key.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT) + "]";
    }

    /** One entry per Active or Toggle implant that is cooling down (or knocked offline by Strain): name, bar, seconds. */
    private static void cooldownStrip(GuiGraphics g, Font font, BodyState body, int width, int height) {
        int entryW = 58;
        int left = width / 2 - 91;
        int x = left;
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
            String time = offline ? "!!" : (remaining + 19) / 20 + "s";
            String name = Component.translatable("item." + e.id().getNamespace() + "." + e.id().getPath()).getString();
            int room = entryW - font.width(" " + time) - 2;
            if (font.width(name) > room) {
                name = font.plainSubstrByWidth(name, room - font.width(".")) + ".";
            }
            g.drawString(font, name + " " + time, x, y - 9, offline ? CyberUi.RED : TEXT);
            long total = Math.max(1, Body.cooldownTicksFor(body, e.id()));
            bar(g, x, y, entryW - 4, 3, offline ? 1.0 : 1.0 - (double) remaining / total, offline ? CyberUi.RED : CyberUi.TEXT_DIM);
            x += entryW + 4;
            if (x + entryW > left + 182) {
                x = left;
                y -= 16;
            }
        }
    }

    /**
     * While strapped into the Rig with its screen closed: a progress plate at the top of the screen and a soft magenta
     * vignette that says the body is restrained.
     */
    private static void rigOperation(GuiGraphics g, Font font, int width, int height) {
        if (!ClientRig.restrained() || Minecraft.getInstance().screen != null) {
            return;
        }
        if (!ClientConfig.get(ClientConfig.REDUCE_SCREEN_EFFECTS)) {
            int edge = Anim.fade(CyberUi.MAGENTA, 0.18f + 0.12f * Anim.pulse(1.4f));
            g.fillGradient(0, 0, width, 24, edge, 0x00FF2BD6);
            g.fillGradient(0, height - 24, width, height, 0x00FF2BD6, edge);
        }
        RigView.Operation op = ClientRig.view(ClientRig.strappedInto()).operation();
        if (op == null) {
            return;
        }
        float p = op.progress(0);
        int w = 160;
        int x = (width - w) / 2;
        int y = 6;
        CyberUi.panel(g, x, y, w, 22, CyberUi.MAGENTA, 0xD00A0E18);
        Component label = Component.translatable("cyberimplants.rig.progress", Math.round(p * 100));
        g.drawString(font, label, x + 6, y + 4, TEXT, false);
        String left = Component.translatable("cyberimplants.rig.op.left", op.secondsLeft()).getString();
        g.drawString(font, left, x + w - 6 - font.width(left), y + 4, CyberUi.TEXT_DIM, false);
        CyberUi.meter(g, x + 4, y + 16, w - 8, 2, p, CyberUi.MAGENTA);
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
            g.drawString(font, line, width - font.width(line) - 6, y, CyberUi.RED);
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
                        e.distanceTo(player)), 6, y + 4, CyberUi.AMBER);
            }
        }
    }

    private static int kindColor(String kind) {
        return switch (kind) {
            case "hostile" -> CyberUi.RED;
            case "boss" -> 0xFFFF40FF;
            case "neutral" -> CyberUi.AMBER;
            case "player" -> 0xFF60C0FF;
            default -> 0xFF80FF80;
        };
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int color) {
        CyberUi.meter(g, x, y, w, h, fraction, color);
    }

    private Hud() {
    }
}
