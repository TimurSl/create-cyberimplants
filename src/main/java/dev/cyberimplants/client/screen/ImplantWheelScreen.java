package dev.cyberimplants.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.client.ClientBodyState;
import dev.cyberimplants.client.ModKeys;
import dev.cyberimplants.implant.ActivationType;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.network.UseImplantPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The Implant Wheel: hold the key, point at an entry and release to fire or toggle it
 * (docs/design/01-core-systems.md, section 1.6).
 */
public class ImplantWheelScreen extends Screen {
    private static final int RADIUS = 78;
    private static final int ENTRY_W = 108;
    private static final int ENTRY_H = 22;

    private final List<ResourceLocation> entries;

    public ImplantWheelScreen(List<ResourceLocation> entries) {
        super(Component.translatable("cyberimplants.wheel.title"));
        this.entries = entries;
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

    private int entryX(int i) {
        double angle = -Math.PI / 2 + 2 * Math.PI * i / entries.size();
        return (int) (centerX() + Math.cos(angle) * RADIUS * 1.35);
    }

    private int entryY(int i) {
        double angle = -Math.PI / 2 + 2 * Math.PI * i / entries.size();
        return (int) (centerY() + Math.sin(angle) * RADIUS);
    }

    /** The entry whose direction is closest to the mouse, or -1 when the mouse is near the centre. */
    private int hovered(int mouseX, int mouseY) {
        double dx = mouseX - centerX();
        double dy = mouseY - centerY();
        if (dx * dx + dy * dy < 14 * 14) {
            return -1;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2;
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        double segment = 2 * Math.PI / entries.size();
        return (int) Math.floor((angle + segment / 2) / segment) % entries.size();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // A light dim only: no blur, the world stays readable behind the wheel.
        g.fill(0, 0, width, height, 0x40000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        BodyState body = ClientBodyState.get();
        int hover = hovered(mouseX, mouseY);
        g.drawCenteredString(font, title, centerX(), centerY() - 4, 0xFF9FD8FF);
        for (int i = 0; i < entries.size(); i++) {
            ResourceLocation id = entries.get(i);
            int x = entryX(i) - ENTRY_W / 2;
            int y = entryY(i) - ENTRY_H / 2;
            long cooldown = body.cooldownRemaining(id);
            boolean on = Toggles.isOn(minecraft.player, id);
            int background = i == hover ? 0xE0305870 : 0xC0182028;
            g.fill(x, y, x + ENTRY_W, y + ENTRY_H, background);
            g.renderOutline(x, y, ENTRY_W, ENTRY_H, on ? 0xFF60FF90 : 0xFF406080);
            g.drawCenteredString(font, Component.translatable("item." + id.getNamespace() + "." + id.getPath()),
                    x + ENTRY_W / 2, y + 3, 0xFFFFFFFF);
            String status;
            if (on) {
                status = Component.translatable("cyberimplants.wheel.on").getString();
            } else if (cooldown > 0) {
                status = String.format("%.0fs", cooldown / 20.0);
            } else {
                status = Component.translatable("cyberimplants.wheel.ready").getString();
            }
            g.drawCenteredString(font, status, x + ENTRY_W / 2, y + 12, cooldown > 0 ? 0xFFFF9090 : 0xFF90FFB0);
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
                }
            }
            onClose();
        }
    }
}
