package dev.cyberimplants.implant.mobility;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inertial Stabilizer: air control is applied by the client (ClientMovement). Tier V Redirect (Layer 5) turns the
 * velocity toward the look direction once per airtime; the client applies it, the server counts it and bills Charge.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class InertialStabilizer {
    private static final Set<UUID> REDIRECTED = ConcurrentHashMap.newKeySet();

    public static boolean redirect(ServerPlayer player) {
        int tier = BodyState.get(player).tierOf(ImplantIds.INERTIAL_STABILIZER);
        if (!Stats.unlocked(ImplantIds.INERTIAL_STABILIZER, tier, "redirect") || player.onGround() || player.isPassenger()
                || REDIRECTED.contains(player.getUUID())) {
            return false;
        }
        if (!Body.spend(player, Stats.of(ImplantIds.INERTIAL_STABILIZER, tier).value("redirect_charge", 10.0))) {
            return false;
        }
        REDIRECTED.add(player.getUUID());
        player.fallDistance = 0;
        ImplantFx.sound(player, SoundEvents.PHANTOM_FLAP, 0.7f, 1.6f);
        ImplantFx.burst(player.serverLevel(), ParticleTypes.CLOUD, player.position().add(0, 0.5, 0), 8, 0.3, 0.05);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (player.onGround() || player.onClimbable() || player.isInWater())) {
            REDIRECTED.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        REDIRECTED.remove(event.getEntity().getUUID());
    }

    private InertialStabilizer() {
    }
}
