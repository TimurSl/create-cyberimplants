package dev.cyberimplants.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Numbers for one tier of one implant.
 *
 * @param load          Neural Load
 * @param chargeCost    activation cost in C (0 when none)
 * @param cooldownTicks cooldown in ticks (0 when none)
 * @param values        implant-specific numbers keyed by name (radius, duration, damage ...)
 * @param unlocks       ids of tier-gated features this tier has; see {@link ImplantStats#unlockLayers()}
 */
public record TierStats(int load, double chargeCost, int cooldownTicks, Map<String, Double> values, List<String> unlocks) {
    public static final Codec<TierStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("load", 0).forGetter(TierStats::load),
            Codec.DOUBLE.optionalFieldOf("charge_cost", 0.0).forGetter(TierStats::chargeCost),
            Codec.INT.optionalFieldOf("cooldown_ticks", 0).forGetter(TierStats::cooldownTicks),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("values", Map.of()).forGetter(TierStats::values),
            Codec.STRING.listOf().optionalFieldOf("unlocks", List.of()).forGetter(TierStats::unlocks)
    ).apply(i, TierStats::new));

    /** Used when an implant has no stats file; every number reads as its fallback. */
    public static final TierStats EMPTY = new TierStats(0, 0, 0, Map.of(), List.of());

    public TierStats {
        values = Map.copyOf(values);
        unlocks = List.copyOf(unlocks);
    }

    public double value(String key, double fallback) {
        return values.getOrDefault(key, fallback);
    }
}
