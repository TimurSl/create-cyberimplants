package dev.cyberimplants.data;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/** Holds the currently loaded stats: filled by the reload listener on the server, by a sync payload on clients. */
public final class ImplantStatsRegistry {
    public static final Codec<Map<ResourceLocation, ImplantStats>> MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, ImplantStats.CODEC);

    private static volatile Map<ResourceLocation, ImplantStats> stats = Map.of();

    public static void replace(Map<ResourceLocation, ImplantStats> next) {
        stats = Map.copyOf(next);
    }

    public static Map<ResourceLocation, ImplantStats> all() {
        return stats;
    }

    public static Optional<ImplantStats> get(ResourceLocation implantId) {
        return Optional.ofNullable(stats.get(implantId));
    }

    private ImplantStatsRegistry() {
    }
}
