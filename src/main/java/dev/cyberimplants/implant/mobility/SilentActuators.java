package dev.cyberimplants.implant.mobility;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;

/**
 * Silent Actuators: the user's footsteps stop producing vibrations (sculk sensors, wardens) and sounds. Layer 1
 * suppresses footsteps only; later tiers add more actions.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class SilentActuators {
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
