package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Full body state, sent to the owning client on login, respawn and dimension change. */
public record BodySnapshotPayload(BodyState state) implements CustomPacketPayload {
    public static final Type<BodySnapshotPayload> TYPE = new Type<>(CyberImplants.id("body_snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BodySnapshotPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(BodyState.CODEC)
                    .map(BodySnapshotPayload::new, BodySnapshotPayload::state);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
