package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the entity (network id) that Target Tracking is following, or -1 for none. */
public record TrackedTargetPayload(int entityId) implements CustomPacketPayload {
    public static final Type<TrackedTargetPayload> TYPE = new Type<>(CyberImplants.id("tracked_target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrackedTargetPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, TrackedTargetPayload::entityId, TrackedTargetPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
