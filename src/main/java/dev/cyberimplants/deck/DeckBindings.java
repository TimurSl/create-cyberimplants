package dev.cyberimplants.deck;

import dev.cyberimplants.registry.ModAttachments;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Binding Neural Ports to a player's deck. The chassis has a built-in Tier I interface (two ports, 16 blocks); a Machine
 * Interface module raises both limits with its tier.
 */
public final class DeckBindings {
    /** The built-in interface's limits, used when there is no module. */
    public static final int MAX_PORTS = 2;
    public static final double DIRECT_RANGE = 16.0;

    private static int interfaceTier(ServerPlayer player) {
        return Math.max(1, DeckHardware.of(dev.cyberimplants.body.BodyState.get(player)).machineInterface());
    }

    public static int maxPorts(ServerPlayer player) {
        return (int) dev.cyberimplants.data.Stats.of(dev.cyberimplants.implant.ImplantIds.DECK_MACHINE_INTERFACE, interfaceTier(player))
                .value("bound_ports", MAX_PORTS);
    }

    public static double directRange(ServerPlayer player) {
        return dev.cyberimplants.data.Stats.of(dev.cyberimplants.implant.ImplantIds.DECK_MACHINE_INTERFACE, interfaceTier(player))
                .value("direct_range", DIRECT_RANGE);
    }

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
        if (data.ports().size() >= maxPorts(player)) {
            player.displayClientMessage(Component.translatable("cyberimplants.port.full", maxPorts(player)), true);
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
