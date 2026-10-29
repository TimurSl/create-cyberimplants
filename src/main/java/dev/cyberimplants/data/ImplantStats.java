package dev.cyberimplants.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.cyberimplants.implant.TechTier;

import java.util.List;
import java.util.Map;

/**
 * Per-implant stat file ({@code data/<ns>/cyberimplants/implant_stats/<type>.json}). Always exactly five tiers.
 * Features that belong to a later implementation layer are listed in {@code unlock_layers} so they stay switched
 * off when {@code layers.maxEnabled} is lower than the layer that introduces them.
 */
public record ImplantStats(List<TierStats> tiers, Map<String, Integer> unlockLayers) {
    public static final Codec<ImplantStats> CODEC = RecordCodecBuilder.<ImplantStats>create(i -> i.group(
            TierStats.CODEC.listOf().fieldOf("tiers").forGetter(ImplantStats::tiers),
            Codec.unboundedMap(Codec.STRING, Codec.intRange(1, 5))
                    .optionalFieldOf("unlock_layers", Map.of()).forGetter(ImplantStats::unlockLayers)
    ).apply(i, ImplantStats::new)).validate(ImplantStats::validate);

    public ImplantStats {
        tiers = List.copyOf(tiers);
        unlockLayers = Map.copyOf(unlockLayers);
    }

    private static DataResult<ImplantStats> validate(ImplantStats stats) {
        if (stats.tiers.size() != TechTier.MAX_LEVEL) {
            return DataResult.error(() -> "Expected " + TechTier.MAX_LEVEL + " tiers but found " + stats.tiers.size());
        }
        for (TierStats tier : stats.tiers) {
            for (String unlock : tier.unlocks()) {
                if (!stats.unlockLayers.containsKey(unlock)) {
                    return DataResult.error(() -> "Unlock '" + unlock + "' has no entry in unlock_layers");
                }
            }
        }
        return DataResult.success(stats);
    }

    /** Stats for a tier level 1-5. */
    public TierStats tier(int level) {
        return tiers.get(level - 1);
    }

    /** Unlocks of the given tier that are active under the given layer cap. */
    public List<String> activeUnlocks(int level, int maxLayer) {
        return tier(level).unlocks().stream()
                .filter(u -> unlockLayers.getOrDefault(u, 1) <= maxLayer)
                .toList();
    }
}
