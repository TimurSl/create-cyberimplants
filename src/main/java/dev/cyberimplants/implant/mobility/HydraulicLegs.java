package dev.cyberimplants.implant.mobility;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hydraulic Legs: server side of the charged leap. The client builds pressure and applies the launch velocity.
 * Tier III Ground slam (Layer 3): a dive that hits everything around the landing spot; Tier IV scales it with the
 * fall and launches the victims. Tier V Rebound (Layer 5): a jump right after the slam is a free full-charge leap.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class HydraulicLegs {
    public static final double SLAM_RADIUS = 3.0;
    public static final float SLAM_DAMAGE = 3.0f;
    /** Tier IV: +1 damage per this many blocks fallen, at most {@link #SLAM_MAX_BONUS}. */
    public static final double SLAM_BLOCKS_PER_BONUS = 4.0;
    public static final float SLAM_MAX_BONUS = 6.0f;
    /** A slam that has not landed after this long is forgotten. */
    private static final int SLAM_TIMEOUT_TICKS = 200;
    /** The 0.5 s Rebound window, plus slack for the round trip to the client. */
    private static final int REBOUND_WINDOW_TICKS = 10 + 10;

    /** Running dives: the game time and height they started at. */
    private record Slam(long start, double startY) {
    }

    private static final Map<UUID, Slam> SLAMS = new ConcurrentHashMap<>();
    /** Game time until which a Rebound leap is allowed. */
    private static final Map<UUID, Long> REBOUND = new ConcurrentHashMap<>();

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
        ImplantFx.leap(player, power);
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
        ImplantFx.leap(player, 1.0f);
        DeckEvents.fire(player, "leap", Map.of("power", 1.0));
        return true;
    }

    public static boolean slamStart(ServerPlayer player) {
        int tier = BodyState.get(player).tierOf(ImplantIds.HYDRAULIC_LEGS);
        if (!Stats.unlocked(ImplantIds.HYDRAULIC_LEGS, tier, "ground_slam") || player.onGround() || player.isPassenger()) {
            return false;
        }
        SLAMS.put(player.getUUID(), new Slam(player.level().getGameTime(), player.getY()));
        ImplantFx.sound(player, SoundEvents.PISTON_CONTRACT, 0.8f, 0.6f);
        return true;
    }

    /** The client reports the landing; the fall height is the smaller of its measure and the server's. */
    public static boolean slamLand(ServerPlayer player, float reportedFall) {
        Slam slam = SLAMS.remove(player.getUUID());
        long now = player.level().getGameTime();
        int tier = BodyState.get(player).tierOf(ImplantIds.HYDRAULIC_LEGS);
        if (slam == null || now - slam.start() > SLAM_TIMEOUT_TICKS || !Stats.unlocked(ImplantIds.HYDRAULIC_LEGS, tier, "ground_slam")) {
            return false;
        }
        double fall = Mth.clamp(Math.min(reportedFall, slam.startY() - player.getY() + 1.0), 0.0, 64.0);
        boolean scaling = Stats.unlocked(ImplantIds.HYDRAULIC_LEGS, tier, "slam_scaling");
        float damage = slamDamage(fall, scaling);
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SLAM_RADIUS, 1.0, SLAM_RADIUS),
                e -> e != player && e.isAlive() && !e.isAlliedTo(player) && e.distanceToSqr(player) <= SLAM_RADIUS * SLAM_RADIUS)) {
            e.hurt(player.damageSources().playerAttack(player), damage);
            if (scaling) {
                e.push(0, 0.6, 0);
                e.hurtMarked = true;
            }
        }
        if (Stats.unlocked(ImplantIds.HYDRAULIC_LEGS, tier, "rebound")) {
            REBOUND.put(player.getUUID(), now + REBOUND_WINDOW_TICKS);
        }
        ImplantFx.shockwave(player.serverLevel(), ParticleTypes.EXPLOSION, player.position().add(0, 0.1, 0), 8, 0.4);
        ImplantFx.sound(player, SoundEvents.ANVIL_LAND, 0.8f, 0.6f);
        DeckEvents.fire(player, "slam", Map.of("damage", (double) damage, "fall", fall));
        return true;
    }

    /** Slam damage: 3, plus at Tier IV one per four blocks fallen (at most +6). */
    public static float slamDamage(double fall, boolean scaling) {
        return SLAM_DAMAGE + (scaling ? Math.min(SLAM_MAX_BONUS, (float) Math.floor(fall / SLAM_BLOCKS_PER_BONUS)) : 0f);
    }

    /** A Rebound leap: full charge, no Charge and no cooldown, once per slam. Otherwise it is an ordinary leap. */
    public static boolean rebound(ServerPlayer player) {
        Long until = REBOUND.remove(player.getUUID());
        if (until == null || player.level().getGameTime() > until
                || !Stats.unlocked(player, ImplantIds.HYDRAULIC_LEGS, "rebound")) {
            return leap(player, 1.0f);
        }
        ImplantFx.leap(player, 1.0f);
        DeckEvents.fire(player, "leap", Map.of("power", 1.0));
        return true;
    }

    /** The slam's own landing does not hurt the user. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SLAMS.containsKey(player.getUUID())) {
            event.setDamageMultiplier(0.0f);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SLAMS.remove(event.getEntity().getUUID());
        REBOUND.remove(event.getEntity().getUUID());
    }

    /** Initial vertical speed (blocks/tick) that reaches roughly {@code height} blocks under vanilla gravity and drag. */
    public static double launchSpeedForHeight(double height) {
        return Math.sqrt(0.16 * height) * 1.07;
    }

    private HydraulicLegs() {
    }
}
