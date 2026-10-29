package dev.cyberimplants.implant.subdermal;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.implant.Attrs;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emergency Shell: at critical health the subdermal layer locks rigid for a few seconds. Triggers after a hit that
 * leaves the player alive at or below the threshold; a hit that kills from above the threshold never triggers it.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class EmergencyShell {
    private static final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();

    public static boolean active(Player player) {
        Long until = ACTIVE_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    /** Applies the active Shell's damage reduction. */
    public static float reduce(ServerPlayer player, float amount) {
        if (!active(player)) {
            return amount;
        }
        double reduction = Stats.value(player, ImplantIds.EMERGENCY_SHELL, "reduction", 0.4);
        return (float) (amount * (1.0 - reduction));
    }

    /** Hardens the shell now (ERS or auto-trigger). Returns false if it is missing, cooling down or unaffordable. */
    public static boolean trigger(ServerPlayer player) {
        int tier = BodyState.get(player).tierOf(ImplantIds.EMERGENCY_SHELL);
        if (tier == 0 || active(player) || !Body.ready(player, ImplantIds.EMERGENCY_SHELL)) {
            return false;
        }
        TierStats stats = Stats.of(ImplantIds.EMERGENCY_SHELL, tier);
        if (!Body.spendEmergency(player, stats.chargeCost())) {
            return false;
        }
        ACTIVE_UNTIL.put(player.getUUID(), player.level().getGameTime() + (long) stats.value("duration_ticks", 40));
        Attrs.multiplied(player, Attributes.MOVEMENT_SPEED, "shell_slow", -stats.value("movement_penalty", 0.4), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.8f, 1.5f);
        player.serverLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
        return true;
    }

    private static void end(ServerPlayer player) {
        if (ACTIVE_UNTIL.remove(player.getUUID()) != null) {
            Attrs.multiplied(player, Attributes.MOVEMENT_SPEED, "shell_slow", 0, false);
            if (BodyState.get(player).has(ImplantIds.EMERGENCY_SHELL)) {
                Body.startCooldown(player, ImplantIds.EMERGENCY_SHELL, Body.cooldownTicks(player, ImplantIds.EMERGENCY_SHELL));
            }
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.EMERGENCY_SHELL);
        if (tier == 0) {
            return;
        }
        double threshold = Stats.of(ImplantIds.EMERGENCY_SHELL, tier).value("trigger_health", 0.2);
        if (player.getHealth() <= player.getMaxHealth() * threshold) {
            trigger(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Long until = ACTIVE_UNTIL.get(player.getUUID());
        if (until != null && (!player.isAlive() || player.level().getGameTime() >= until)) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }

    private EmergencyShell() {
    }
}
