package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the visible hardware of a player (network id), as {@link dev.cyberimplants.implant.Appearance} bits.
 * Sent to the player and everyone tracking them, so implants can be drawn on other players too.
 */
public record AppearancePayload(int entityId, int bits) implements CustomPacketPayload {
    public static final Type<AppearancePayload> TYPE = new Type<>(CyberImplants.id("appearance"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AppearancePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, AppearancePayload::entityId, ByteBufCodecs.VAR_INT, AppearancePayload::bits,
                    AppearancePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
