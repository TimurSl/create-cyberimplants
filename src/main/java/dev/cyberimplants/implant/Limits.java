package dev.cyberimplants.implant;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.config.Layers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/**
 * Layer 4 limitations: the costs of an implant that the earlier layers left out (docs/design/implants, the
 * "Limits" lines). Each rule is off under {@code layers.maxEnabled < 4}.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Limits {
    /** Servo whine: sprinting footsteps carry this much further (here: louder). */
    public static final float SERVO_WHINE_VOLUME = 1.5f;
    /** Dense frame: extra gravity at Reinforced Skeleton Tier III and up, which also makes the user sink in water. */
    public static final double DENSE_FRAME_GRAVITY = 0.05;

    /** Micro-Actuators Tier I-III whine while sprinting, unless Silent Actuators Tier IV+ cancel it. */
    public static boolean servoWhine(Player player) {
        if (!Layers.atLeast(4) || !player.isSprinting()) {
            return false;
        }
        BodyState body = BodyState.get(player);
        int micro = body.tierOf(ImplantIds.MICRO_ACTUATORS);
        return micro >= 1 && micro <= 3 && body.tierOf(ImplantIds.SILENT_ACTUATORS) < 4;
    }

    /** Reinforced Skeleton Tier III+ is heavy. */
    public static boolean denseFrame(BodyState body) {
        return Layers.atLeast(4) && body.tierOf(ImplantIds.REINFORCED_SKELETON) >= 3;
    }

    @SubscribeEvent
    public static void onSound(PlayLevelSoundEvent.AtEntity event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && isStep(event) && servoWhine(player)) {
            event.setNewVolume(event.getNewVolume() * SERVO_WHINE_VOLUME);
        }
    }

    @SubscribeEvent
    public static void onPositionalSound(PlayLevelSoundEvent.AtPosition event) {
        if (!isStep(event)) {
            return;
        }
        Vec3 at = event.getPosition();
        for (Player player : event.getLevel().players()) {
            if (player.position().distanceToSqr(at) < 1.5 && servoWhine(player)) {
                event.setNewVolume(event.getNewVolume() * SERVO_WHINE_VOLUME);
                return;
            }
        }
    }

    private static boolean isStep(PlayLevelSoundEvent event) {
        return event.getSound().value().getLocation().getPath().endsWith(".step");
    }

    private Limits() {
    }
}
