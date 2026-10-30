package dev.cyberimplants.implant.optical;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optical Camouflage: temporary concealment. Detection ranges collapse (visibility modifier) and mobs that already
 * target the user forget after the aggro-drop delay. Attacking or taking damage breaks it. The cooldown starts when
 * the effect ends. Tier V Re-cloak (Layer 5): the first attack only drops the cloak for a second.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class OpticalCamouflage {
    /**
     * A running camouflage. {@code suspendedUntil} is the Tier V Re-cloak: after the one attack it survives, the cloak
     * drops for a second and comes back; {@code recloakUsed} allows that once per activation.
     */
    private record Camo(long start, long end, boolean recloakUsed, long suspendedUntil) {
        boolean suspended(long now) {
            return now < suspendedUntil;
        }
    }

    private static final int RECLOAK_DELAY_TICKS = 20;

    private static final Map<UUID, Camo> ACTIVE = new ConcurrentHashMap<>();

    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            if (ACTIVE.containsKey(player.getUUID())) {
                return UseResult.BUSY;
            }
            if (!Body.ready(player, ImplantIds.OPTICAL_CAMOUFLAGE)) {
                return UseResult.COOLDOWN;
            }
            TierStats stats = Stats.of(player, ImplantIds.OPTICAL_CAMOUFLAGE);
            if (!(ctx.emergency() ? Body.spendEmergency(player, stats.chargeCost()) : Body.spend(player, stats.chargeCost()))) {
                return UseResult.NO_CHARGE;
            }
            long now = player.level().getGameTime();
            int duration = (int) stats.value("duration_ticks", 80);
            ACTIVE.put(player.getUUID(), new Camo(now, now + duration, false, 0));
            // A faint shimmer: invisibility without particles.
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, false));
            ImplantFx.camoOn(player, duration);
            return UseResult.OK;
        }

        @Override
        public boolean isActive(ServerPlayer player) {
            return ACTIVE.containsKey(player.getUUID());
        }

        @Override
        public boolean stop(ServerPlayer player) {
            return end(player, "manual");
        }
    };

    public static boolean active(Player player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    private static boolean end(ServerPlayer player, String reason) {
        Camo camo = ACTIVE.remove(player.getUUID());
        if (camo == null) {
            return false;
        }
        player.removeEffect(MobEffects.INVISIBILITY);
        if (!player.hasDisconnected()) {
            ImplantFx.camoOff(player, !reason.equals("manual") && !reason.equals("expired"));
        }
        if (BodyState.get(player).has(ImplantIds.OPTICAL_CAMOUFLAGE)) {
            Body.startCooldown(player, ImplantIds.OPTICAL_CAMOUFLAGE, Body.cooldownTicks(player, ImplantIds.OPTICAL_CAMOUFLAGE));
        }
        if (!reason.equals("manual") && !reason.equals("expired")) {
            DeckEvents.fire(player, "camo_broken", Map.of("reason", reason));
        }
        return true;
    }

    public static void onBodyChanged(ServerPlayer player) {
        if (!BodyState.get(player).has(ImplantIds.OPTICAL_CAMOUFLAGE)) {
            end(player, "removed");
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Camo camo = ACTIVE.get(player.getUUID());
        if (camo == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (!player.isAlive() || now >= camo.end) {
            end(player, "expired");
            return;
        }
        if (camo.suspended(now)) {
            return;
        }
        if (camo.suspendedUntil() > 0 && !player.hasEffect(MobEffects.INVISIBILITY)) {
            // Re-cloak: the second is over and the cloak re-engages for the rest of its time.
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) (camo.end() - now), 0, false, false, false));
            ImplantFx.camoOn(player, (int) (camo.end() - now));
        }
        if (player.tickCount % 3 == 0) {
            ImplantFx.camoShimmer(player);
        }
        TierStats stats = Stats.of(ImplantIds.OPTICAL_CAMOUFLAGE, BodyState.get(player).tierOf(ImplantIds.OPTICAL_CAMOUFLAGE));
        // Upkeep: 2 C per second on top of the activation cost.
        if (player.tickCount % 20 == 0 && !Body.spend(player, stats.value("upkeep_per_second", 2.0))) {
            end(player, "no_charge");
            return;
        }
        // Existing targeters forget the user after the aggro-drop delay.
        if (player.tickCount % 10 == 0 && now - camo.start >= stats.value("aggro_drop_ticks", 60)) {
            for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0),
                    m -> m.getTarget() == player)) {
                mob.setTarget(null);
            }
        }
    }

    /** Detection ranges collapse while camouflaged: smaller multiplier when still, larger when moving. */
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        Camo camo = event.getEntity() instanceof Player p ? ACTIVE.get(p.getUUID()) : null;
        if (!(event.getEntity() instanceof Player player) || camo == null || camo.suspended(player.level().getGameTime())) {
            return;
        }
        TierStats stats = Stats.of(player, ImplantIds.OPTICAL_CAMOUFLAGE);
        boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 || player.isSprinting();
        event.modifyVisibility(moving ? stats.value("detect_moving", 0.8) : stats.value("detect_still", 0.5));
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !recloak(player)) {
            end(player, "attack");
        }
    }

    /** Tier V: the first attack only drops the cloak for a second. Returns false when the camo must end instead. */
    private static boolean recloak(ServerPlayer player) {
        Camo camo = ACTIVE.get(player.getUUID());
        if (camo == null || camo.recloakUsed() || !Stats.unlocked(player, ImplantIds.OPTICAL_CAMOUFLAGE, "recloak")) {
            return false;
        }
        long now = player.level().getGameTime();
        if (now + RECLOAK_DELAY_TICKS >= camo.end()) {
            return false;
        }
        ACTIVE.put(player.getUUID(), new Camo(camo.start(), camo.end(), true, now + RECLOAK_DELAY_TICKS));
        player.removeEffect(MobEffects.INVISIBILITY);
        ImplantFx.camoOff(player, true);
        return true;
    }

    /**
     * An Entity Scanner Tier V pulse (implant reading) breaks a cloak of Tier IV or lower. A Tier V cloak withstands it.
     * Returns whether the cloak was broken.
     */
    public static boolean revealByScan(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID()) && BodyState.get(player).tierOf(ImplantIds.OPTICAL_CAMOUFLAGE) <= 4
                && end(player, "scanned");
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player, "damage");
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player, "logout");
        }
    }

    private OpticalCamouflage() {
    }
}
