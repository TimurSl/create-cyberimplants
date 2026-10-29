package dev.cyberimplants.data;

import dev.cyberimplants.body.BodyState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Convenience lookups over the loaded implant stats. Works on both sides (clients get a synced copy). */
public final class Stats {
    /** The tier's numbers, or {@link TierStats#EMPTY} when the implant has no stats file. */
    public static TierStats of(ResourceLocation id, int tier) {
        return ImplantStatsRegistry.get(id)
                .map(s -> s.tier(Mth.clamp(tier, 1, 5)))
                .orElse(TierStats.EMPTY);
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

    private Stats() {
    }
}
