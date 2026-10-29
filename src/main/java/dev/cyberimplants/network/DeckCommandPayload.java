package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: a deck control command. */
/** {@code thread}: the Coprocessor thread to load into, or to unload (negative = all). */
public record DeckCommandPayload(Op op, String program, int thread) implements CustomPacketPayload {
    public enum Op {
        /** Send the current {@link DeckStatusPayload}. */
        STATUS,
        /** Load (boot) the named stored program. */
        LOAD,
        /** Unload the running program. */
        UNLOAD,
        /** Delete the named stored program. */
        DELETE
    }

    public static final Type<DeckCommandPayload> TYPE = new Type<>(CyberImplants.id("deck_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeckCommandPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Op.values()[Math.floorMod(i, Op.values().length)], Op::ordinal), DeckCommandPayload::op,
            ByteBufCodecs.stringUtf8(256), DeckCommandPayload::program,
            ByteBufCodecs.VAR_INT, DeckCommandPayload::thread,
            DeckCommandPayload::new);

    public DeckCommandPayload(Op op, String program) {
        this(op, program, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
