package dev.cyberimplants.client;

import dev.cyberimplants.network.DeckStatusPayload;

/** The last deck status the server sent, shown by the deck screen. */
public final class ClientDeckState {
    private static volatile DeckStatusPayload status;
    private static volatile int version;

    public static DeckStatusPayload get() {
        return status;
    }

    public static int version() {
        return version;
    }

    public static void set(DeckStatusPayload next) {
        status = next;
        version++;
    }

    private ClientDeckState() {
    }
}
