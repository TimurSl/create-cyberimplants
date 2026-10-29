package dev.cyberimplants.data;

import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.config.Layers;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Convenience lookups over the loaded implant stats. Works on both sides (clients get a synced copy). */
public final class Stats {
    /** The tier's numbers, or {@link TierStats#EMPTY} when the implant has no stats file. */
    public static TierStats of(ResourceLocation id, int tier) {
        return ImplantStatsRegistry.get(id)
                .map(s -> cap(id, s, Mth.clamp(tier, 1, 5)))
                .orElse(TierStats.EMPTY);
    }

    /**
     * Under {@code layers.maxEnabled = 1} tiers II-V repeat the Tier I numbers and keep only their own Load. The Core
     * already scaled per tier in Layer 1 and is exempt.
     */
    private static TierStats cap(ResourceLocation id, ImplantStats stats, int tier) {
        if (tier == 1 || Layers.atLeast(2) || id.equals(ImplantIds.CORE)) {
            return stats.tier(tier);
        }
        TierStats own = stats.tier(tier);
        TierStats first = stats.tier(1);
        return new TierStats(own.load(), first.chargeCost(), first.cooldownTicks(), first.values(), first.unlocks());
    }

    /** Stats of the player's installed implant of this type; {@link TierStats#EMPTY} if not installed. */
    public static TierStats of(Player player, ResourceLocation id) {
        int tier = BodyState.get(player).tierOf(id);
        return tier == 0 ? TierStats.EMPTY : of(id, tier);
    }

    /** A named number from the player's installed implant, or {@code fallback}. */
    public static double value(Player player, ResourceLocation id, String key, double fallback) {
        return of(player, id).value(key, fallback);
    }

    /** Whether the tier-gated feature is on for this implant tier. Features of a layer above {@code layers.maxEnabled} are off. */
    public static boolean unlocked(ResourceLocation id, int tier, String unlock) {
        return tier > 0 && ImplantStatsRegistry.get(id)
                .map(s -> s.activeUnlocks(Mth.clamp(tier, 1, 5), Layers.maxEnabled()).contains(unlock))
                .orElse(false);
    }

    /** Whether the player's installed implant of this type has the feature. */
    public static boolean unlocked(Player player, ResourceLocation id, String unlock) {
        return unlocked(id, BodyState.get(player).tierOf(id), unlock);
    }

    private Stats() {
    }
}
