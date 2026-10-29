package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: start a screen effect ({@link dev.cyberimplants.fx.ScreenFx} by name) for {@code ticks} at
 * {@code strength} (0-1). {@code ticks <= 0} ends that effect early.
 */
public record ScreenFxPayload(String effect, int ticks, float strength) implements CustomPacketPayload {
    public static final Type<ScreenFxPayload> TYPE = new Type<>(CyberImplants.id("screen_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ScreenFxPayload::effect,
            ByteBufCodecs.VAR_INT, ScreenFxPayload::ticks,
            ByteBufCodecs.FLOAT, ScreenFxPayload::strength,
            ScreenFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
