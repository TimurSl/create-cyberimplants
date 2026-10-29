package dev.cyberimplants.deck;

import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.synapse.vm.Handle;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** {@code deck.*}: tier, free RAM, last cycle count and dropped events. */
final class DeckStateHandle implements Handle {
    private final ServerPlayer player;
    private final Supplier<DeckRuntime> runtime;

    DeckStateHandle(ServerPlayer player, Supplier<DeckRuntime> runtime) {
        this.player = player;
        this.runtime = runtime;
    }

    @Override
    public Object get(String member) {
        DeckRuntime r = runtime.get();
        DeckHardware hw = DeckHardware.of(BodyState.get(player));
        return switch (member) {
            case "tier" -> (double) hw.chassisTier();
            case "ram_free" -> r == null ? (double) hw.ramCells()
                    : (double) Math.max(0, hw.ramCells() - r.runner().program().footprintCells());
            case "cycles_last_tick" -> r == null ? 0.0 : (double) r.lastCycles;
            case "dropped_events" -> r == null ? 0.0 : (double) r.runner().droppedEvents();
            default -> null;
        };
    }
}
