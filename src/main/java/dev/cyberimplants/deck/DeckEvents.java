package dev.cyberimplants.deck;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/** Entry point for implants to raise deck events (docs/design/04-synapse-language.md, section 6.2). */
public final class DeckEvents {
    public static void fire(ServerPlayer player, String event, Map<String, Object> payload) {
        fire(player, event, null, payload);
    }

    /** {@code argument} selects handlers declared with that number ({@code ON health_below 30}). */
    public static void fire(ServerPlayer player, String event, Double argument, Map<String, Object> payload) {
        DeckRuntime runtime = DeckService.runtime(player);
        if (runtime != null) {
            runtime.runner().fire(event, argument, payload);
        }
    }

    private DeckEvents() {
    }
}
