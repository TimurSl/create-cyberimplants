package dev.cyberimplants.client;

import dev.cyberimplants.body.BodyState;

/** The local player's body state as last received from the server. Read by HUD and screens. */
public final class ClientBodyState {
    private static volatile BodyState state = BodyState.EMPTY;

    public static BodyState get() {
        return state;
    }

    public static void set(BodyState next) {
        state = next;
    }

    private ClientBodyState() {
    }
}
