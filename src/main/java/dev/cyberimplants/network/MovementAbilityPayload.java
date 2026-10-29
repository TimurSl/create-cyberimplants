package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: a movement ability the client has already applied. The server validates it and bills Charge
 * (docs/design/08-technical-architecture.md, section 8.6). {@code a}, {@code b} carry kind-specific parameters.
 */
public record MovementAbilityPayload(Kind kind, float a, float b) implements CustomPacketPayload {
    public enum Kind {
        /** Kerenzikov dodge; a, b = direction x, z. */
        DODGE,
        /** Enhanced Tendons air jump. */
        AIR_JUMP,
        /** Hydraulic Legs leap; a = charge fraction. */
        LEAP,
        /** Magnetic Soles: still attached (refresh). */
        SOLES_ATTACHED,
        /** Magnetic Soles: detached. */
        SOLES_DETACHED
    }

    public static final Type<MovementAbilityPayload> TYPE = new Type<>(CyberImplants.id("movement_ability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MovementAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Kind.values()[Math.floorMod(i, Kind.values().length)], Kind::ordinal), MovementAbilityPayload::kind,
            ByteBufCodecs.FLOAT, MovementAbilityPayload::a,
            ByteBufCodecs.FLOAT, MovementAbilityPayload::b,
            MovementAbilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
