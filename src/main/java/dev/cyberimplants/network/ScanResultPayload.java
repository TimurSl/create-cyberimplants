package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server -> client: the markers of an Entity Scanner pulse. */
public record ScanResultPayload(List<Marker> markers, int revealTicks) implements CustomPacketPayload {
    public static final Type<ScanResultPayload> TYPE = new Type<>(CyberImplants.id("scan_result"));

    /** One revealed entity: network id, threat class, entity type id and distance at pulse time. */
    public record Marker(int entityId, String kind, String type, float distance) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Marker> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Marker::entityId,
                ByteBufCodecs.STRING_UTF8, Marker::kind,
                ByteBufCodecs.STRING_UTF8, Marker::type,
                ByteBufCodecs.FLOAT, Marker::distance,
                Marker::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ScanResultPayload> STREAM_CODEC = StreamCodec.composite(
            Marker.STREAM_CODEC.apply(ByteBufCodecs.list(64)), ScanResultPayload::markers,
            ByteBufCodecs.VAR_INT, ScanResultPayload::revealTicks,
            ScanResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
