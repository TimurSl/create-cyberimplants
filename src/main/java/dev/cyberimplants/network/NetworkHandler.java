package dev.cyberimplants.network;

import dev.cyberimplants.client.ClientBodyState;
import dev.cyberimplants.data.ImplantStatsRegistry;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {
    /** Bump when a payload's wire format changes incompatibly. */
    public static final String PROTOCOL_VERSION = "1";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(BodySnapshotPayload.TYPE, BodySnapshotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBodyState.set(payload.state())));

        registrar.playToClient(ImplantStatsSyncPayload.TYPE, ImplantStatsSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ImplantStatsRegistry.replace(payload.stats())));
    }

    private NetworkHandler() {
    }
}
