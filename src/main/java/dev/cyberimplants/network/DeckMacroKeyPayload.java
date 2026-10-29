package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: deck macro key {@code n} (1-4) was pressed; becomes a {@code key n} event. */
public record DeckMacroKeyPayload(int key) implements CustomPacketPayload {
    public static final Type<DeckMacroKeyPayload> TYPE = new Type<>(CyberImplants.id("deck_macro_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeckMacroKeyPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, DeckMacroKeyPayload::key, DeckMacroKeyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
