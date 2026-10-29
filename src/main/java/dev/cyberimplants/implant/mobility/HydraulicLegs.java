package dev.cyberimplants.implant.mobility;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.Map;

/** Hydraulic Legs: server side of the charged leap. The client builds pressure and applies the launch velocity. */
public final class HydraulicLegs {
    /** Client-reported leap; {@code fraction} is how much of the full charge was built (0-1). */
    public static boolean leap(ServerPlayer player, float fraction) {
        int tier = BodyState.get(player).tierOf(ImplantIds.HYDRAULIC_LEGS);
        if (tier == 0 || !Body.ready(player, ImplantIds.HYDRAULIC_LEGS)) {
            return false;
        }
        TierStats stats = Stats.of(ImplantIds.HYDRAULIC_LEGS, tier);
        if (!Body.spend(player, stats.chargeCost())) {
            return false;
        }
        Body.startCooldown(player, ImplantIds.HYDRAULIC_LEGS, stats.cooldownTicks());
        float power = Mth.clamp(fraction, 0.0f, 1.0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.8f, 0.7f);
        DeckEvents.fire(player, "leap", Map.of("power", (double) power));
        return true;
    }

    /** The ERS escape leap: straight up at full power, paid for from the emergency reserves. */
    public static boolean escapeLeap(ServerPlayer player) {
        int tier = BodyState.get(player).tierOf(ImplantIds.HYDRAULIC_LEGS);
        if (tier == 0 || !Body.ready(player, ImplantIds.HYDRAULIC_LEGS)) {
            return false;
        }
        TierStats stats = Stats.of(ImplantIds.HYDRAULIC_LEGS, tier);
        if (!Body.spendEmergency(player, stats.chargeCost())) {
            return false;
        }
        Body.startCooldown(player, ImplantIds.HYDRAULIC_LEGS, stats.cooldownTicks());
        double height = stats.value("max_vertical", 4);
        player.setDeltaMovement(player.getDeltaMovement().x * 0.5, launchSpeedForHeight(height), player.getDeltaMovement().z * 0.5);
        player.hurtMarked = true;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.8f, 0.7f);
        DeckEvents.fire(player, "leap", Map.of("power", 1.0));
        return true;
    }

    /** Initial vertical speed (blocks/tick) that reaches roughly {@code height} blocks under vanilla gravity and drag. */
    public static double launchSpeedForHeight(double height) {
        return Math.sqrt(0.16 * height) * 1.07;
    }

    private HydraulicLegs() {
    }
}
