package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: open the Implantation Rig screen for the Rig at {@code pos}. */
public record OpenRigPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenRigPayload> TYPE = new Type<>(CyberImplants.id("open_rig"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRigPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, OpenRigPayload::pos, OpenRigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
