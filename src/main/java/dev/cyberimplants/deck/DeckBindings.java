package dev.cyberimplants.deck;

import dev.cyberimplants.registry.ModAttachments;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/** Binding Neural Ports to a player's deck. Layer 1 has the chassis' built-in interface: two ports, 16 blocks. */
public final class DeckBindings {
    public static final int MAX_PORTS = 2;
    public static final double DIRECT_RANGE = 16.0;

    /** Binds the port to the player's deck, or unbinds it if it is already bound. Reports the result in the action bar. */
    public static void togglePort(ServerPlayer player, GlobalPos pos, String name) {
        DeckData data = player.getData(ModAttachments.DECK_DATA);
        for (Map.Entry<String, GlobalPos> e : data.ports().entrySet()) {
            if (e.getValue().equals(pos)) {
                player.setData(ModAttachments.DECK_DATA, data.withoutPort(e.getKey()));
                player.displayClientMessage(Component.translatable("cyberimplants.port.unbound", e.getKey()), true);
                return;
            }
        }
        if (data.ports().size() >= MAX_PORTS) {
            player.displayClientMessage(Component.translatable("cyberimplants.port.full", MAX_PORTS), true);
            return;
        }
        if (data.ports().containsKey(name)) {
            player.displayClientMessage(Component.translatable("cyberimplants.port.name_taken", name), true);
            return;
        }
        player.setData(ModAttachments.DECK_DATA, data.withPort(name, pos));
        player.displayClientMessage(Component.translatable("cyberimplants.port.bound", name), true);
    }

    private DeckBindings() {
    }
}
