package dev.cyberimplants.deck;

import dev.cyberimplants.config.ServerConfig;
import dev.cyberimplants.perf.DeckBudget;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entry point for implants to raise deck events (docs/design/04-synapse-language.md, section 6.2). At most
 * {@code maxEventsPerSecond} events per player reach the decks; the rest are dropped and counted.
 */
public final class DeckEvents {
    /** Per player: the second (game time / 20) being counted and how many events it has seen. */
    private static final Map<UUID, long[]> RATE = new ConcurrentHashMap<>();

    public static void fire(ServerPlayer player, String event, Map<String, Object> payload) {
        fire(player, event, null, payload);
    }

    /** {@code argument} selects handlers declared with that number ({@code ON health_below 30}). */
    public static void fire(ServerPlayer player, String event, Double argument, Map<String, Object> payload) {
        var runtimes = DeckService.runtimes(player);
        if (runtimes.isEmpty()) {
            return;
        }
        if (!withinRate(player)) {
            DeckBudget.eventDropped();
            runtimes.forEach(rt -> rt.runner().dropEvent());
            return;
        }
        for (DeckRuntime runtime : runtimes) {
            runtime.runner().fire(event, argument, payload);
        }
    }

    private static boolean withinRate(ServerPlayer player) {
        long second = player.level().getGameTime() / 20;
        long[] window = RATE.computeIfAbsent(player.getUUID(), id -> new long[]{second, 0});
        if (window[0] != second) {
            window[0] = second;
            window[1] = 0;
        }
        return ++window[1] <= ServerConfig.DECK_MAX_EVENTS_PER_SECOND.get();
    }

    private DeckEvents() {
    }
}
