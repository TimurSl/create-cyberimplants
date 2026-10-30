package dev.cyberimplants.implant.mobility;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Silent Actuators: the user's footsteps stop producing vibrations (sculk sensors, wardens) and sounds. Layer 1
 * suppresses footsteps only; later tiers add more actions. Tier V Ghost step (Layer 5): no Thermal heat trails (the
 * {@code GHOST_STEP} appearance bit tells clients) and the Entity Scanner's auto-scan skips the user while still.
 * Servo whine is already cancelled from Tier IV ({@link dev.cyberimplants.implant.Limits}).
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class SilentActuators {
    /** Standing still means not having moved more than this (squared, blocks) ... */
    private static final double STILL_EPSILON_SQR = 0.01;
    /** ... for at least this many ticks. */
    private static final int STILL_TICKS = 10;

    /** Last position per player and the game time they last moved. */
    private record Motion(Vec3 at, long movedAt) {
    }

    private static final Map<UUID, Motion> MOTION = new ConcurrentHashMap<>();

    public static boolean ghostStep(Player player) {
        return Stats.unlocked(player, ImplantIds.SILENT_ACTUATORS, "ghost_step");
    }

    /** Ghost step and standing still: invisible to auto-scans. */
    public static boolean ghostStill(ServerPlayer player) {
        if (!ghostStep(player)) {
            return false;
        }
        Motion m = MOTION.get(player.getUUID());
        return m != null && player.level().getGameTime() - m.movedAt() >= STILL_TICKS;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !BodyState.get(player).has(ImplantIds.SILENT_ACTUATORS)) {
            return;
        }
        Vec3 at = player.position();
        Motion m = MOTION.get(player.getUUID());
        if (m == null || m.at().distanceToSqr(at) > STILL_EPSILON_SQR) {
            MOTION.put(player.getUUID(), new Motion(at, player.level().getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MOTION.remove(event.getEntity().getUUID());
    }
    private static boolean silent(Entity entity) {
        return entity instanceof Player player && BodyState.get(player).has(ImplantIds.SILENT_ACTUATORS);
    }

    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (event.getVanillaEvent().value() == GameEvent.STEP.value() && silent(event.getCause())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSound(PlayLevelSoundEvent.AtEntity event) {
        if (silent(event.getEntity()) && isStep(event)) {
            event.setNewVolume(0.0f);
        }
    }

    @SubscribeEvent
    public static void onPositionalSound(PlayLevelSoundEvent.AtPosition event) {
        if (!isStep(event)) {
            return;
        }
        Vec3 at = event.getPosition();
        for (Player player : event.getLevel().players()) {
            if (player.position().distanceToSqr(at) < 1.5 && silent(player)) {
                event.setNewVolume(0.0f);
                return;
            }
        }
    }

    private static boolean isStep(PlayLevelSoundEvent event) {
        return event.getSound().value().getLocation().getPath().endsWith(".step");
    }

    private SilentActuators() {
    }
}
