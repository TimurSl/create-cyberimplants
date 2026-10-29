package dev.cyberimplants.network;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.data.ImplantStats;
import dev.cyberimplants.data.ImplantStatsRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** All loaded implant stats, sent to clients on login and whenever data packs reload. */
public record ImplantStatsSyncPayload(Map<ResourceLocation, ImplantStats> stats) implements CustomPacketPayload {
    public static final Type<ImplantStatsSyncPayload> TYPE = new Type<>(CyberImplants.id("implant_stats_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ImplantStatsSyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(ImplantStatsRegistry.MAP_CODEC)
                    .map(ImplantStatsSyncPayload::new, ImplantStatsSyncPayload::stats);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
