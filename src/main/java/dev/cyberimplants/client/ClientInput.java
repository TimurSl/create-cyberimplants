package dev.cyberimplants.client;

import dev.cyberimplants.client.screen.DeckScreen;
import dev.cyberimplants.client.screen.ImplantWheelScreen;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.implant.ActivationType;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantType;
import dev.cyberimplants.implant.Arms;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.network.DeckMacroKeyPayload;
import dev.cyberimplants.network.FireBallisticPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Reads this mod's key bindings once per client tick. */
final class ClientInput {
    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        while (ModKeys.WHEEL.consumeClick()) {
            if (mc.screen == null) {
                List<ResourceLocation> entries = wheelEntries();
                if (!entries.isEmpty()) {
                    mc.setScreen(new ImplantWheelScreen(entries));
                }
            }
        }
        while (ModKeys.DECK.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new DeckScreen());
            }
        }
        while (ModKeys.FIRE_BALLISTIC.consumeClick()) {
            if (mc.screen == null && Arms.freeHandWith(player, ImplantIds.BALLISTIC_MODULE) != null) {
                PacketDistributor.sendToServer(new FireBallisticPayload());
            }
        }
        for (int i = 0; i < ModKeys.MACROS.length; i++) {
            while (ModKeys.MACROS[i].consumeClick()) {
                if (mc.screen == null) {
                    PacketDistributor.sendToServer(new DeckMacroKeyPayload(i + 1));
                }
            }
        }
    }

    /** Installed Active and Toggle implants, in a stable order. */
    static List<ResourceLocation> wheelEntries() {
        BodyState body = ClientBodyState.get();
        List<ResourceLocation> out = new ArrayList<>();
        for (ImplantCatalog.Entry entry : ImplantCatalog.ALL) {
            ImplantType type = entry.type();
            if ((type.activation() == ActivationType.ACTIVE || type.activation() == ActivationType.TOGGLE)
                    && body.has(entry.id())) {
                out.add(entry.id());
            }
        }
        return out;
    }

    private ClientInput() {
    }
}
