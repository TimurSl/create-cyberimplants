package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: fire (or reload) the Ballistic Module. Carries no data; the server decides everything. */
public record FireBallisticPayload() implements CustomPacketPayload {
    public static final Type<FireBallisticPayload> TYPE = new Type<>(CyberImplants.id("fire_ballistic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireBallisticPayload> STREAM_CODEC =
            StreamCodec.unit(new FireBallisticPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
