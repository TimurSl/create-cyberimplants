package dev.cyberimplants.network;

import dev.cyberimplants.client.ClientBodyState;
import dev.cyberimplants.client.ClientPayloads;
import dev.cyberimplants.data.ImplantStatsRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.BiConsumer;

public final class NetworkHandler {
    /** Bump when a payload's wire format changes incompatibly. */
    public static final String PROTOCOL_VERSION = "3";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Server -> client
        registrar.playToClient(BodySnapshotPayload.TYPE, BodySnapshotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientBodyState.set(payload.state())));
        registrar.playToClient(ImplantStatsSyncPayload.TYPE, ImplantStatsSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ImplantStatsRegistry.replace(payload.stats())));
        registrar.playToClient(OpenRigPayload.TYPE, OpenRigPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.openRig(payload.pos())));
        registrar.playToClient(ScanResultPayload.TYPE, ScanResultPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.scanResult(payload)));
        registrar.playToClient(TrackedTargetPayload.TYPE, TrackedTargetPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.trackedTarget(payload.entityId())));
        registrar.playToClient(DeckStatusPayload.TYPE, DeckStatusPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.deckStatus(payload)));
        registrar.playToClient(ScreenFxPayload.TYPE, ScreenFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.screenFx(payload)));

        // Client -> server. Every handler runs on the server thread and is rate limited.
        registrar.playToServer(UseImplantPayload.TYPE, UseImplantPayload.STREAM_CODEC,
                server((p, payload) -> ServerHandlers.useImplant(p, payload)));
        registrar.playToServer(MovementAbilityPayload.TYPE, MovementAbilityPayload.STREAM_CODEC,
                server((p, payload) -> ServerHandlers.movement(p, payload)));
        registrar.playToServer(RigActionPayload.TYPE, RigActionPayload.STREAM_CODEC,
                server((p, payload) -> ServerHandlers.rigAction(p, payload)));
        registrar.playToServer(UploadProgramPayload.TYPE, UploadProgramPayload.STREAM_CODEC,
                server((p, payload) -> ServerHandlers.uploadProgram(p, payload)));
        registrar.playToServer(DeckCommandPayload.TYPE, DeckCommandPayload.STREAM_CODEC,
                server((p, payload) -> ServerHandlers.deckCommand(p, payload)));
        registrar.playToServer(DeckMacroKeyPayload.TYPE, DeckMacroKeyPayload.STREAM_CODEC,
                server((p, payload) -> ServerHandlers.macroKey(p, payload)));
        registrar.playToServer(FireBallisticPayload.TYPE, FireBallisticPayload.STREAM_CODEC,
                server((p, payload) -> ServerHandlers.fireBallistic(p)));
    }

    private static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> net.neoforged.neoforge.network.handling.IPayloadHandler<T> server(BiConsumer<ServerPlayer, T> handler) {
        return (T payload, IPayloadContext context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && ServerHandlers.allow(player)) {
                handler.accept(player, payload);
            }
        });
    }

    private NetworkHandler() {
    }
}
