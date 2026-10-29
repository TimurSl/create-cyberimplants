package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: an operation at the Implantation Rig. {@code install}: put the implant from inventory slot
 * {@code inventorySlot} into body slot {@code region}/{@code index}; otherwise remove the implant in that body slot.
 */
public record RigActionPayload(BlockPos rig, boolean install, int region, int index, int inventorySlot) implements CustomPacketPayload {
    public static final Type<RigActionPayload> TYPE = new Type<>(CyberImplants.id("rig_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RigActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RigActionPayload::rig,
            ByteBufCodecs.BOOL, RigActionPayload::install,
            ByteBufCodecs.VAR_INT, RigActionPayload::region,
            ByteBufCodecs.VAR_INT, RigActionPayload::index,
            ByteBufCodecs.VAR_INT, RigActionPayload::inventorySlot,
            RigActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
