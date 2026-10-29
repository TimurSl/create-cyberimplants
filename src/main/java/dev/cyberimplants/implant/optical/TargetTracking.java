package dev.cyberimplants.implant.optical;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.deck.EntityHandle;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import dev.cyberimplants.network.TrackedTargetPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Target Tracking: tags one entity (Layer 1) and keeps a marker on it through walls. Tags are lost when the target
 * dies, leaves the tag range or changes dimension.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class TargetTracking {
    private static final Map<UUID, UUID> TARGETS = new ConcurrentHashMap<>();

    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            if (ctx.target() instanceof EntityHandle handle) {
                Entity e = handle.resolve();
                return e == null ? UseResult.MISSING : tag(player, e, false);
            }
            TierStats stats = Stats.of(player, ImplantIds.TARGET_TRACKING);
            double range = stats.value("range", 24);
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getLookAngle().scale(range));
            AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0);
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                    e -> e instanceof LivingEntity && e != player && e.isAlive() && e.isPickable(), range * range);
            if (hit == null) {
                return UseResult.NO_LOS;
            }
            return tag(player, hit.getEntity(), true);
        }

        @Override
        public boolean isActive(ServerPlayer player) {
            return TARGETS.containsKey(player.getUUID());
        }

        @Override
        public boolean stop(ServerPlayer player) {
            return untag(player);
        }
    };

    /** The currently tagged entity, or null. */
    public static Entity target(ServerPlayer player) {
        UUID id = TARGETS.get(player.getUUID());
        return id == null ? null : player.serverLevel().getEntity(id);
    }

    public static UseResult tag(ServerPlayer player, Entity entity, boolean needLineOfSight) {
        BodyState body = BodyState.get(player);
        int tier = body.tierOf(ImplantIds.TARGET_TRACKING);
        if (tier == 0) {
            return UseResult.MISSING;
        }
        TierStats stats = Stats.of(ImplantIds.TARGET_TRACKING, tier);
        if (entity == player || entity.distanceTo(player) > stats.value("range", 24)) {
            return UseResult.RANGE;
        }
        if (needLineOfSight && !player.hasLineOfSight(entity)) {
            return UseResult.NO_LOS;
        }
        if (!Body.spend(player, stats.chargeCost())) {
            return UseResult.NO_CHARGE;
        }
        UUID previous = TARGETS.put(player.getUUID(), entity.getUUID());
        if (previous != null && !previous.equals(entity.getUUID())) {
            Entity old = player.serverLevel().getEntity(previous);
            if (old != null) {
                DeckEvents.fire(player, "target_lost", Map.of("entity", EntityHandle.of(player, old)));
            }
        }
        PacketDistributor.sendToPlayer(player, new TrackedTargetPayload(entity.getId()));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.9f);
        DeckEvents.fire(player, "target_tagged", Map.of("entity", EntityHandle.of(player, entity)));
        return UseResult.OK;
    }

    public static boolean untag(ServerPlayer player) {
        UUID id = TARGETS.remove(player.getUUID());
        if (id == null) {
            return false;
        }
        PacketDistributor.sendToPlayer(player, new TrackedTargetPayload(-1));
        Entity old = player.serverLevel().getEntity(id);
        if (old != null) {
            DeckEvents.fire(player, "target_lost", Map.of("entity", EntityHandle.of(player, old)));
        }
        return true;
    }

    public static void onBodyChanged(ServerPlayer player) {
        if (!BodyState.get(player).has(ImplantIds.TARGET_TRACKING)) {
            untag(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        UUID id = TARGETS.get(player.getUUID());
        if (id == null) {
            return;
        }
        Entity target = player.serverLevel().getEntity(id);
        double range = Stats.value(player, ImplantIds.TARGET_TRACKING, "range", 24) * 1.25;
        if (target == null || !target.isAlive() || target.distanceTo(player) > range) {
            untag(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TARGETS.remove(player.getUUID());
            PacketDistributor.sendToPlayer(player, new TrackedTargetPayload(-1));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TARGETS.remove(event.getEntity().getUUID());
    }

    private TargetTracking() {
    }
}
