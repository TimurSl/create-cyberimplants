package dev.cyberimplants.implant;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.config.Layers;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.mobility.MomentumBooster;
import dev.cyberimplants.implant.neural.Kerenzikov;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Layer 4 cross-implant synergies (docs/design/09-roadmap.md). Each rule is off under {@code layers.maxEnabled < 4}
 * and lives here so the implants themselves stay free of knowledge about their partners.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Synergies {
    /** Momentum per block of fall beyond the unhurt 3 blocks, from Momentum Booster Tier II ("+ falling"). */
    public static final double FALL_MOMENTUM_PER_BLOCK = 5.0;
    /** Impact Dampeners turn the landing into stored motion instead of lost motion. */
    public static final double DAMPENER_MOMENTUM_FACTOR = 1.5;
    /** Impact Dampeners Tier V Recoil Storage (Layer 5). */
    public static final double RECOIL_STORAGE_FACTOR = 2.0;
    private static final double VANILLA_SAFE_FALL = 3.0;

    /** AES + Kerenzikov: every evasion opens a Kerenzikov reaction window at no Charge and no cooldown. */
    public static void aesEvaded(ServerPlayer player) {
        int tier = BodyState.get(player).tierOf(ImplantIds.KERENZIKOV);
        if (tier > 0 && Layers.atLeast(4)) {
            Kerenzikov.openWindow(player, tier, 0.0);
        }
    }

    /** AES + Ballistic Visor: the visor reads the arrow sooner, so the detection window is one tick longer. */
    public static int visorDetectionBonus(BodyState body) {
        return Layers.atLeast(4) && body.has(ImplantIds.BALLISTIC_VISOR) ? 1 : 0;
    }

    /** Momentum gained from a fall of this distance for the player's installed implants; 0 when there is no source. */
    public static double fallMomentum(ServerPlayer player, double distance) {
        BodyState body = BodyState.get(player);
        if (!Layers.atLeast(4) || body.tierOf(ImplantIds.MOMENTUM_BOOSTER) < 2) {
            return 0.0;
        }
        double gain = Math.max(0.0, distance - VANILLA_SAFE_FALL) * FALL_MOMENTUM_PER_BLOCK;
        if (!body.has(ImplantIds.IMPACT_DAMPENERS)) {
            return gain;
        }
        return gain * (Stats.unlocked(player, ImplantIds.IMPACT_DAMPENERS, "recoil_storage") ? RECOIL_STORAGE_FACTOR : DAMPENER_MOMENTUM_FACTOR);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        double gain = fallMomentum(player, event.getDistance());
        if (gain > 0) {
            double next = Math.min(100.0, MomentumBooster.value(player) + gain);
            Body.updateQuiet(player, s -> s.withData(MomentumBooster.KEY, next));
        }
    }

    private Synergies() {
    }
}
