package dev.cyberimplants.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.Strain;
import dev.cyberimplants.client.ClientBodyState;
import dev.cyberimplants.client.CyberUi;
import dev.cyberimplants.client.ModKeys;
import dev.cyberimplants.client.ui.Anim;
import dev.cyberimplants.client.ui.Draw;
import dev.cyberimplants.client.ui.StatFormat;
import dev.cyberimplants.client.ui.UiSounds;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.implant.ActivationType;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantItem;
import dev.cyberimplants.implant.ImplantType;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.network.UseImplantPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The Implant Wheel: hold the key, point at a sector and release to fire or toggle it
 * (docs/design/01-core-systems.md, section 1.6). Each sector shows the implant's icon in its category colour; a dark
 * sweep covers the part of the cooldown still to run, toggles that are on get a green rim, and the hub in the middle
 * names the implant under the pointer with its cost and the battery.
 */
public class ImplantWheelScreen extends Screen {
    private static final float OPEN_SECONDS = 0.22f;

    private final List<ResourceLocation> entries;
    private final float openedAt = Anim.now();
    private final Anim.Smooth[] lift;
    private int lastHover = -1;

    public ImplantWheelScreen(List<ResourceLocation> entries) {
        super(Component.translatable("cyberimplants.wheel.title"));
        this.entries = entries;
        this.lift = new Anim.Smooth[entries.size()];
        for (int i = 0; i < lift.length; i++) {
            lift[i] = new Anim.Smooth(0, 16);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int centerX() {
        return width / 2;
    }

    private int centerY() {
        return height / 2;
    }

    private float outer() {
        return Mth.clamp(Math.min(width, height) * 0.36f, 60, 110);
    }

    private float inner() {
        return outer() * 0.5f;
    }

    private float segment() {
        return Mth.PI * 2 / entries.size();
    }

    /** Screen angle of the middle of sector {@code i}: the first one points straight up. */
    private float angleOf(int i) {
        return -Mth.PI / 2 + segment() * i;
    }

    /** The sector in the mouse's direction, or -1 when the mouse rests on the hub. */
    private int hovered(int mouseX, int mouseY) {
        double dx = mouseX - centerX();
        double dy = mouseY - centerY();
        if (dx * dx + dy * dy < inner() * inner() * 0.36) {
            return -1;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2;
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        return (int) Math.floor((angle + segment() / 2) / segment()) % entries.size();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // A light dim only, so the world stays readable behind the wheel.
        float open = Anim.easeOutCubic(Anim.progress(openedAt, OPEN_SECONDS));
        g.fill(0, 0, width, height, Anim.fade(0x70040810, open));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        BodyState body = ClientBodyState.get();
        float open = Anim.progress(openedAt, OPEN_SECONDS);
        float grow = Anim.easeOutBack(open);
        int cx = centerX();
        int cy = centerY();
        float rOut = outer() * (0.6f + 0.4f * grow);
        float rIn = inner() * (0.6f + 0.4f * grow);
        int hover = hovered(mouseX, mouseY);
        if (hover != lastHover && hover >= 0) {
            UiSounds.hover();
        }
        lastHover = hover;

        float gap = Math.min(0.06f, segment() * 0.08f);
        Draw.Batch b = Draw.batch(g);
        b.disc(cx, cy, rOut + 14, Anim.fade(0xC0060912, open), 0x00060912);
        for (int i = 0; i < entries.size(); i++) {
            ResourceLocation id = entries.get(i);
            ImplantType type = ImplantCatalog.get(id);
            int color = type == null ? CyberUi.CYAN : CyberUi.categoryColor(type.category());
            float up = lift[i].update(i == hover ? 1f : 0f);
            float push = up * 5;
            float a = angleOf(i);
            float ox = Mth.cos(a) * push;
            float oy = Mth.sin(a) * push;
            float mid = (rIn + rOut) / 2;
            float thick = rOut - rIn;
            float start = a - segment() / 2 + gap / 2;
            float sweep = segment() - gap;
            boolean offline = minecraft.player != null && Strain.offline(minecraft.player, id);
            int base = Anim.mix(CyberUi.withAlpha(CyberUi.dim(color, 0.22f), 0xD8), CyberUi.withAlpha(CyberUi.dim(color, 0.45f), 0xF0), up);
            b.arc(cx + ox, cy + oy, mid, thick, start, sweep, Anim.fade(base, open), Anim.fade(CyberUi.dim(base, 0.7f), open));
            // Cooldown: a dark sweep over the part still to run.
            long remaining = body.cooldownRemaining(id);
            long total = Math.max(1, Body.cooldownTicksFor(body, id));
            if (remaining > 0) {
                float frac = Math.min(1f, (float) remaining / total);
                b.arc(cx + ox, cy + oy, mid, thick, start, sweep * frac, Anim.fade(0xB0000000, open));
            }
            boolean on = minecraft.player != null && Toggles.isOn(minecraft.player, id);
            int rim = offline ? CyberUi.RED : on ? CyberUi.GREEN : i == hover ? color : CyberUi.dim(color, 0.6f);
            b.arc(cx + ox, cy + oy, rOut - 1, i == hover || on ? 2.5f : 1.2f, start, sweep, Anim.fade(rim, open));
            b.arc(cx + ox, cy + oy, rIn + 0.5f, 1f, start, sweep, Anim.fade(CyberUi.dim(color, 0.5f), open));
        }
        // The pointer: a line from the hub towards the mouse.
        if (hover >= 0) {
            float a = (float) Math.atan2(mouseY - cy, mouseX - cx);
            b.line(cx + Mth.cos(a) * rIn * 0.62f, cy + Mth.sin(a) * rIn * 0.62f, cx + Mth.cos(a) * (rIn - 2), cy + Mth.sin(a) * (rIn - 2),
                    2f, CyberUi.MAGENTA);
        }
        b.disc(cx, cy, rIn * 0.95f, Anim.fade(0xF00A0E18, open), Anim.fade(0xE0121828, open));
        b.ring(cx, cy, rIn * 0.95f, 1.2f, Anim.fade(CyberUi.CYAN, open));
        b.arc(cx, cy, rIn * 0.8f, 1f, Anim.now() * 0.8f, Mth.PI * 0.6f, Anim.fade(CyberUi.MAGENTA, 0.7f * open));
        b.end();

        // Icons, cooldown seconds and short labels.
        for (int i = 0; i < entries.size(); i++) {
            ResourceLocation id = entries.get(i);
            float a = angleOf(i);
            float push = lift[i].get() * 5;
            float mid = (rIn + rOut) / 2 + push;
            int ix = (int) (cx + Mth.cos(a) * mid);
            int iy = (int) (cy + Mth.sin(a) * mid);
            g.renderItem(ImplantItem.stackOf(id, Math.max(1, body.tierOf(id))), ix - 8, iy - 8);
            long remaining = body.cooldownRemaining(id);
            if (remaining > 0) {
                String s = (remaining + 19) / 20 + "s";
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.drawString(font, s, ix - font.width(s) / 2, iy + 5, CyberUi.RED, true);
                g.pose().popPose();
            }
        }
        hub(g, body, hover, cx, cy, rIn, open);
    }

    /** The centre: the hovered implant's name and state, its cost, and the battery. */
    private void hub(GuiGraphics g, BodyState body, int hover, int cx, int cy, float rIn, float open) {
        int maxW = (int) (rIn * 1.7f);
        if (hover < 0) {
            g.drawCenteredString(font, title, cx, cy - 10, Anim.fade(CyberUi.CYAN, open));
        } else {
            ResourceLocation id = entries.get(hover);
            String name = font.plainSubstrByWidth(Component.translatable("item." + id.getNamespace() + "." + id.getPath()).getString(), maxW);
            g.drawCenteredString(font, name, cx, cy - 14, CyberUi.TEXT);
            long cooldown = body.cooldownRemaining(id);
            boolean on = Toggles.isOn(minecraft.player, id);
            boolean offline = Strain.offline(minecraft.player, id);
            String status;
            int color;
            if (offline) {
                status = Component.translatable("cyberimplants.use.offline").getString();
                color = CyberUi.RED;
            } else if (on) {
                status = Component.translatable("cyberimplants.wheel.on").getString();
                color = CyberUi.GREEN;
            } else if (cooldown > 0) {
                status = String.format("%.0fs", cooldown / 20.0);
                color = CyberUi.RED;
            } else {
                status = Component.translatable("cyberimplants.wheel.ready").getString();
                color = CyberUi.GREEN;
            }
            g.drawCenteredString(font, font.plainSubstrByWidth(status, maxW), cx, cy - 4, color);
            TierStats stats = Stats.of(id, Math.max(1, body.tierOf(id)));
            if (stats.chargeCost() > 0) {
                String cost = "-" + StatFormat.number(stats.chargeCost() * Body.chargeMultiplier(body)) + " C";
                boolean afford = !Body.chargeEnabled() || body.charge() + 1e-9 >= stats.chargeCost() * Body.chargeMultiplier(body);
                g.drawCenteredString(font, cost, cx, cy + 6, afford ? CyberUi.CYAN : CyberUi.RED);
            }
        }
        double max = Body.maxCharge(body);
        if (max > 0) {
            int w = (int) (rIn * 1.1f);
            CyberUi.meter(g, cx - w / 2, cy + 17, w, 2, body.charge() / max, CyberUi.CYAN);
        }
    }

    @Override
    public void tick() {
        // Releasing the wheel key selects the hovered entry. Polling the key avoids relying on key-up events
        // reaching the screen.
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (!InputConstants.isKeyDown(window, ModKeys.WHEEL.getKey().getValue())) {
            Minecraft mc = Minecraft.getInstance();
            int hover = hovered((int) (mc.mouseHandler.xpos() * width / mc.getWindow().getScreenWidth()),
                    (int) (mc.mouseHandler.ypos() * height / mc.getWindow().getScreenHeight()));
            if (hover >= 0 && hover < entries.size()) {
                ResourceLocation id = entries.get(hover);
                ActivationType type = ImplantCatalog.get(id).activation();
                if (type == ActivationType.ACTIVE || type == ActivationType.TOGGLE) {
                    PacketDistributor.sendToServer(new UseImplantPayload(id));
                    UiSounds.select();
                }
            }
            onClose();
        }
    }
}
