package dev.cyberimplants.implant.mobility;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Enhanced Tendons: mid-air jumps. The client applies the velocity; the server counts jumps per airtime and bills Charge. */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class EnhancedTendons {
    private static final Map<UUID, Integer> USED = new ConcurrentHashMap<>();

    public static boolean airJump(ServerPlayer player) {
        int tier = BodyState.get(player).tierOf(ImplantIds.ENHANCED_TENDONS);
        if (tier == 0 || player.onGround() || player.isPassenger() || player.isFallFlying()) {
            return false;
        }
        TierStats stats = Stats.of(ImplantIds.ENHANCED_TENDONS, tier);
        int used = USED.getOrDefault(player.getUUID(), 0);
        if (used >= (int) stats.value("air_jumps", 1) || !Body.spend(player, stats.chargeCost())) {
            return false;
        }
        USED.put(player.getUUID(), used + 1);
        ImplantFx.airJump(player);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Air jumps reset on landing, climbing or entering water.
        if (event.getEntity() instanceof ServerPlayer player
                && (player.onGround() || player.onClimbable() || player.isInWater())) {
            USED.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        USED.remove(event.getEntity().getUUID());
    }

    private EnhancedTendons() {
    }
}
