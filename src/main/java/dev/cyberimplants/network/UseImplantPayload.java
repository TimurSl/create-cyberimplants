package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: fire or toggle an installed Active / Toggle implant (from the Implant Wheel). */
public record UseImplantPayload(ResourceLocation implant) implements CustomPacketPayload {
    public static final Type<UseImplantPayload> TYPE = new Type<>(CyberImplants.id("use_implant"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UseImplantPayload> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, UseImplantPayload::implant, UseImplantPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
