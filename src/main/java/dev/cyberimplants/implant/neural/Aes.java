package dev.cyberimplants.implant.neural;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.deck.EntityHandle;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Synergies;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automatic Evasion System: sidesteps incoming arrows. Layer 1 uses the fixed rule (always on, arrows only, no
 * condition slots). Needs free space on a side to dodge; a dodge into a wall is wasted but still costs a charge.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Aes {
    public static final String DISABLED_KEY = "aes/disabled";

    private static final class State {
        int charges = -1; // -1 = not initialised
        int rechargeTimer;
        final Set<UUID> handled = new HashSet<>();
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    public static int charges(Player player) {
        State s = STATES.get(player.getUUID());
        return s == null || s.charges < 0 ? (int) Stats.value(player, ImplantIds.AES, "charges", 1) : s.charges;
    }

    public static boolean enabled(Player player) {
        return !BodyState.get(player).flag(DISABLED_KEY);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BodyState body = BodyState.get(player);
        int tier = body.tierOf(ImplantIds.AES);
        if (tier == 0) {
            STATES.remove(player.getUUID());
            return;
        }
        TierStats stats = Stats.of(ImplantIds.AES, tier);
        int maxCharges = (int) stats.value("charges", 1);
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        if (state.charges < 0) {
            state.charges = maxCharges;
        }
        if (state.charges < maxCharges && ++state.rechargeTimer >= stats.value("recharge_ticks", 240)) {
            state.charges++;
            state.rechargeTimer = 0;
        }
        if (state.charges <= 0 || player.tickCount % 2 != 0 || !enabled(player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        int window = (int) stats.value("detection_ticks", 4);
        AABB search = player.getBoundingBox().inflate(16.0);
        List<AbstractArrow> arrows = level.getEntitiesOfClass(AbstractArrow.class, search,
                a -> (a instanceof Arrow || a instanceof SpectralArrow) && a.getOwner() != player);
        if (state.handled.size() > 64) {
            state.handled.clear();
        }
        AABB hitBox = player.getBoundingBox().inflate(0.35);
        for (AbstractArrow arrow : arrows) {
            Vec3 velocity = arrow.getDeltaMovement();
            if (velocity.lengthSqr() < 0.05 || state.handled.contains(arrow.getUUID())) {
                continue;
            }
            Vec3 start = arrow.position();
            Vec3 end = start.add(velocity.scale(window));
            if (!hitBox.contains(start) && hitBox.clip(start, end).isEmpty()) {
                continue;
            }
            state.handled.add(arrow.getUUID());
            double eta = start.distanceTo(player.position()) / Math.max(velocity.length(), 1.0E-3);
            DeckEvents.fire(player, "projectile_incoming", Map.of(
                    "projectile", EntityHandle.of(player, arrow),
                    "eta", eta,
                    "damage", arrow.getBaseDamage() * velocity.length()));
            if (!Body.spend(player, stats.chargeCost())) {
                return;
            }
            state.charges--;
            sidestep(player, level, velocity, stats.value("distance", 1.5));
            DeckEvents.fire(player, "evaded", Map.of("projectile", EntityHandle.of(player, arrow)));
            Synergies.aesEvaded(player);
            return; // one evasion per check
        }
    }

    private static void sidestep(ServerPlayer player, ServerLevel level, Vec3 projectileVelocity, double distance) {
        Vec3 flat = new Vec3(projectileVelocity.x, 0, projectileVelocity.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = player.getLookAngle().multiply(1, 0, 1);
        }
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        Vec3 chosen = null;
        for (double sign : new double[]{1.0, -1.0}) {
            Vec3 candidate = side.scale(sign * distance);
            if (level.noCollision(player, player.getBoundingBox().move(candidate))) {
                chosen = candidate.normalize();
                break;
            }
        }
        // A dodge into a wall is wasted: the charge is spent and the flicker still shows.
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
        if (chosen != null) {
            double impulse = player.onGround() ? distance * 0.45 : distance * 0.12;
            player.setDeltaMovement(player.getDeltaMovement().add(chosen.scale(impulse)));
            player.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    private Aes() {
    }
}
