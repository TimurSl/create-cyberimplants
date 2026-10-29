package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server -> client: what the deck screen shows. {@code sources} are the stored programs' texts (names parallel), the
 * hardware summary, the running program and its log, and the result of the last compile / upload.
 */
public record DeckStatusPayload(
        boolean booted,
        int isa,
        int ramCells,
        int storageBytes,
        int storageUsed,
        List<String> names,
        List<String> sources,
        String loaded,
        int footprint,
        List<String> log,
        String message) implements CustomPacketPayload {

    public static final Type<DeckStatusPayload> TYPE = new Type<>(CyberImplants.id("deck_status"));

    private static final StreamCodec<ByteBuf, List<String>> STRINGS =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeckStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.booted);
                buf.writeVarInt(p.isa);
                buf.writeVarInt(p.ramCells);
                buf.writeVarInt(p.storageBytes);
                buf.writeVarInt(p.storageUsed);
                STRINGS.encode(buf, p.names);
                STRINGS.encode(buf, p.sources);
                ByteBufCodecs.STRING_UTF8.encode(buf, p.loaded);
                buf.writeVarInt(p.footprint);
                STRINGS.encode(buf, p.log);
                ByteBufCodecs.STRING_UTF8.encode(buf, p.message);
            },
            buf -> new DeckStatusPayload(
                    buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    STRINGS.decode(buf), STRINGS.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                    buf.readVarInt(), STRINGS.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
