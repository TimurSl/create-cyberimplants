package dev.cyberimplants.implant.mobility;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Tendons: mid-air jumps. The client applies the velocity; the server counts jumps per airtime and bills Charge.
 * Tier V Gust (Layer 5): every air jump pushes nearby creatures away, like a small wind charge.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class EnhancedTendons {
    private static final double GUST_RADIUS = 3.0;
    private static final double GUST_STRENGTH = 0.9;

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
        if (Stats.unlocked(ImplantIds.ENHANCED_TENDONS, tier, "gust")) {
            gust(player);
        }
        return true;
    }

    private static void gust(ServerPlayer player) {
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(GUST_RADIUS),
                e -> e != player && e.isAlive() && !e.isAlliedTo(player))) {
            Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0E-4) {
                continue;
            }
            double falloff = 1.0 - Math.min(1.0, away.length() / (GUST_RADIUS + 1.0));
            Vec3 push = away.normalize().scale(GUST_STRENGTH * (0.4 + 0.6 * falloff));
            e.push(push.x, 0.25, push.z);
            e.hurtMarked = true;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6f, 1.3f);
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
