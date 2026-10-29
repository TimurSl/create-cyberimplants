package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: compile and store a Synapse program in deck Storage. The server enforces the size cap. */
public record UploadProgramPayload(String source) implements CustomPacketPayload {
    public static final Type<UploadProgramPayload> TYPE = new Type<>(CyberImplants.id("upload_program"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UploadProgramPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(65_536), UploadProgramPayload::source, UploadProgramPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
