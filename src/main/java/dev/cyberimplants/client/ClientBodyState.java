package dev.cyberimplants.client;

import dev.cyberimplants.body.BodyState;

/** The local player's body state as last received from the server. Read by HUD, screens and client movement. */
public final class ClientBodyState {
    private static volatile BodyState state = BodyState.EMPTY;

    public static BodyState get() {
        return state;
    }

    public static void set(BodyState next) {
        state = next;
    }

    /** Advances the local copy of {@code onlineTicks} so cooldown readouts count down between syncs. */
    public static void tick() {
        BodyState s = state;
        if (!s.cooldownEnd().isEmpty()) {
            state = s.withOnlineTicks(s.onlineTicks() + 1);
        }
    }

    private ClientBodyState() {
    }
}
