package dev.cyberimplants.client;

import dev.cyberimplants.client.screen.RigScreen;
import dev.cyberimplants.network.DeckStatusPayload;
import dev.cyberimplants.network.ScanResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Entry points for server -> client payloads. Only ever called on the physical client. */
public final class ClientPayloads {
    public static void openRig(BlockPos pos) {
        Minecraft.getInstance().setScreen(new RigScreen(pos));
    }

    public static void openManual() {
        Minecraft.getInstance().setScreen(new dev.cyberimplants.client.screen.ManualScreen());
    }

    public static void scanResult(ScanResultPayload payload) {
        ClientOptics.setScan(payload);
    }

    public static void trackedTarget(int entityId) {
        ClientOptics.setTracked(entityId);
    }

    public static void deckStatus(DeckStatusPayload payload) {
        ClientDeckState.set(payload);
    }

    private ClientPayloads() {
    }
}
