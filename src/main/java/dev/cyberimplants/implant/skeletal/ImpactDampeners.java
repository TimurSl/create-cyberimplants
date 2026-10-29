package dev.cyberimplants.implant.skeletal;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import java.util.Map;

/**
 * Impact Dampeners recover energy from landings: from Tier II, every block of fall beyond the unhurt 3 blocks gives
 * 1-4 C, capped per landing (docs/design/implants/c-skeletal-muscular.md). The fall damage cut itself is an attribute.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class ImpactDampeners {
    private static final double VANILLA_SAFE_FALL = 3.0;

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.IMPACT_DAMPENERS);
        if (tier == 0) {
            return;
        }
        var stats = Stats.of(ImplantIds.IMPACT_DAMPENERS, tier);
        double absorbed = Math.max(0.0, event.getDistance() - VANILLA_SAFE_FALL);
        double recovered = Math.min(stats.value("recovery_cap", 50), absorbed * stats.value("energy_per_block", 0));
        if (recovered > 0) {
            Body.addCharge(player, recovered);
        }
        if (absorbed > 0) {
            ImplantFx.landing(player, event.getDistance(), recovered);
        }
        DeckEvents.fire(player, "landed", Map.of("fall_distance", (double) event.getDistance(), "recovered", recovered));
    }

    private ImpactDampeners() {
    }
}
