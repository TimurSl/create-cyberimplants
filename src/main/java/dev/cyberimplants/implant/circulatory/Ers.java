package dev.cyberimplants.implant.circulatory;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantActions;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.mobility.HydraulicLegs;
import dev.cyberimplants.implant.subdermal.EmergencyShell;
import dev.cyberimplants.implant.subdermal.KineticShield;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;

/**
 * Emergency Response System. Layer 1: the only danger input is health, and it reacts on the next tick. When the
 * danger score crosses the threshold it fires its response implants in a fixed priority order.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Ers {
    public static final String DISABLED_KEY = "ers/disabled";
    public static final String THRESHOLD_KEY = "ers/threshold";
    public static final double DEFAULT_THRESHOLD = 0.6;

    /** Responses in priority order. */
    private static final List<ResourceLocation> RESPONSES = List.of(
            ImplantIds.KINETIC_SHIELD,
            ImplantIds.EMERGENCY_SHELL,
            ImplantIds.OPTICAL_CAMOUFLAGE,
            ImplantIds.HYDRAULIC_LEGS);

    public static boolean enabled(Player player) {
        return !BodyState.get(player).flag(DISABLED_KEY);
    }

    /** Danger score in [0, 1]; Layer 1 is {@code 1 - health fraction}. */
    public static double danger(Player player) {
        return Math.max(0.0, Math.min(1.0, 1.0 - player.getHealth() / player.getMaxHealth()));
    }

    public static double threshold(Player player) {
        return BodyState.get(player).number(THRESHOLD_KEY, DEFAULT_THRESHOLD);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        BodyState s = BodyState.get(player);
        int tier = s.tierOf(ImplantIds.ERS);
        if (tier == 0 || s.flag(DISABLED_KEY) || !Body.ready(player, ImplantIds.ERS)) {
            return;
        }
        if (danger(player) < threshold(player)) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.ERS, tier);
        int slots = (int) stats.value("response_slots", 1);
        int fired = 0;
        for (ResourceLocation response : RESPONSES) {
            if (fired >= slots) {
                break;
            }
            if (s.has(response) && fire(player, response)) {
                fired++;
            }
        }
        if (fired > 0) {
            Body.startCooldown(player, ImplantIds.ERS, stats.cooldownTicks());
            DeckEvents.fire(player, "ers_fired", Map.of());
        }
    }

    private static boolean fire(ServerPlayer player, ResourceLocation response) {
        if (response.equals(ImplantIds.KINETIC_SHIELD)) {
            return KineticShield.emergencyRecharge(player);
        }
        if (response.equals(ImplantIds.EMERGENCY_SHELL)) {
            return EmergencyShell.trigger(player);
        }
        if (response.equals(ImplantIds.HYDRAULIC_LEGS)) {
            return HydraulicLegs.escapeLeap(player);
        }
        return ImplantActions.use(player, response, UseContext.EMERGENCY).ok();
    }

    private Ers() {
    }
}
