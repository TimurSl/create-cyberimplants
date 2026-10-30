package dev.cyberimplants.deck;

import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.synapse.vm.Handle;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/** {@code deck.*}: tier, free RAM, last cycle count, dropped events and the Processor overclock. */
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
            case "threads" -> (double) hw.threads();
            case "latency" -> new dev.cyberimplants.synapse.compiler.Ticks(hw.latencyTicks());
            case "ram_free" -> {
                int used = 0;
                for (DeckRuntime other : DeckService.runtimes(player)) {
                    used += other.runner().program().footprintCells();
                }
                yield (double) Math.max(0, hw.ramCells() - used);
            }
            case "cycles_last_tick" -> r == null ? 0.0 : (double) r.lastCycles;
            case "dropped_events" -> r == null ? 0.0 : (double) r.runner().droppedEvents();
            case "overclocked" -> DeckService.overclocked(player);
            case "overclock_cooldown" -> new dev.cyberimplants.synapse.compiler.Ticks(DeckService.overclockCooldown(player));
            default -> null;
        };
    }
}
