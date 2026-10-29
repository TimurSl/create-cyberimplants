package dev.cyberimplants.client;

import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.config.Layers;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.network.ScanResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Optical implant visuals. Markers are the vanilla glow outline (which draws through walls), applied to the client's
 * copy of an entity; lead indicators and projectile arcs are particles. Everything here is presentation only.
 */
public final class ClientOptics {
    private static List<ScanResultPayload.Marker> scanMarkers = List.of();
    private static long scanExpires;
    private static int trackedId = -1;

    /** Entities we switched to glowing, with whether they were already glowing (so we can restore). */
    private static final Map<Integer, Boolean> GLOWING = new HashMap<>();

    public static void setScan(ScanResultPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        scanMarkers = payload.markers();
        scanExpires = (mc.level == null ? 0 : mc.level.getGameTime()) + payload.revealTicks();
    }

    public static void setTracked(int entityId) {
        trackedId = entityId;
    }

    public static List<ScanResultPayload.Marker> scanMarkers() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getGameTime() < scanExpires ? scanMarkers : List.of();
    }

    public static int trackedId() {
        return trackedId;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            GLOWING.clear();
            return;
        }
        BodyState body = ClientBodyState.get();

        Set<Integer> wanted = new HashSet<>();
        if (!body.installed().isEmpty()) {
            for (ScanResultPayload.Marker m : scanMarkers()) {
                wanted.add(m.entityId());
            }
            if (trackedId >= 0 && body.has(ImplantIds.TARGET_TRACKING)) {
                wanted.add(trackedId);
            }
            if (Toggles.isOn(player, ImplantIds.THERMAL_VISION)) {
                addThermal(mc, player, body, wanted);
            }
        }
        applyGlow(mc, wanted);

        if (!body.installed().isEmpty() && player.tickCount % 2 == 0) {
            leadIndicator(mc, player, body);
            if (player.tickCount % 3 == 0) {
                projectileArcs(mc, player, body);
            }
        }
    }

    // ---------------------------------------------------------------- glow markers

    private static void addThermal(Minecraft mc, LocalPlayer player, BodyState body, Set<Integer> wanted) {
        double range = Stats.of(ImplantIds.THERMAL_VISION, Math.max(1, body.tierOf(ImplantIds.THERMAL_VISION))).value("range", 16);
        AABB box = player.getBoundingBox().inflate(range);
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, box,
                x -> x != player && x.isAlive() && x.distanceToSqr(player) <= range * range)) {
            // Undead are cold and invisible to thermal sensors.
            if (!e.getType().is(EntityTypeTags.UNDEAD)) {
                wanted.add(e.getId());
            }
        }
    }

    private static void applyGlow(Minecraft mc, Set<Integer> wanted) {
        for (Integer id : wanted) {
            Entity e = mc.level.getEntity(id);
            if (e == null) {
                continue;
            }
            if (!GLOWING.containsKey(id)) {
                GLOWING.put(id, e.isCurrentlyGlowing());
            }
            e.setGlowingTag(true);
        }
        GLOWING.entrySet().removeIf(entry -> {
            if (wanted.contains(entry.getKey())) {
                return false;
            }
            Entity e = mc.level.getEntity(entry.getKey());
            if (e != null) {
                e.setGlowingTag(entry.getValue());
            }
            return true;
        });
    }

    // ---------------------------------------------------------------- Combat Predictor

    private static void leadIndicator(Minecraft mc, LocalPlayer player, BodyState body) {
        int tier = body.tierOf(ImplantIds.COMBAT_PREDICTOR);
        if (tier == 0) {
            return;
        }
        double speed = projectileSpeed(player.getMainHandItem());
        if (speed <= 0) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        Entity target = null;
        // Layer 4 synergy: with Target Tracking installed the predictor leads the tagged target, not the crosshair's.
        if (Layers.atLeast(4) && trackedId >= 0 && body.has(ImplantIds.TARGET_TRACKING)) {
            Entity tagged = mc.level.getEntity(trackedId);
            if (tagged instanceof LivingEntity && tagged.isAlive() && tagged.distanceToSqr(eye) <= 64.0 * 64.0) {
                target = tagged;
            }
        }
        if (target == null) {
            Vec3 look = player.getLookAngle();
            double range = 64.0;
            AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(look.scale(range)), box,
                    e -> e instanceof LivingEntity && e != player && e.isAlive(), range * range);
            if (hit == null) {
                return;
            }
            target = hit.getEntity();
        }
        Vec3 velocity = new Vec3(target.getX() - target.xo, target.getY() - target.yo, target.getZ() - target.zo);
        double horizon = Stats.of(ImplantIds.COMBAT_PREDICTOR, tier).value("horizon_ticks", 10);
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);

        double t = center.distanceTo(eye) / speed;
        Vec3 aim = center;
        for (int i = 0; i < 4; i++) {
            double lead = Math.min(t, horizon);
            aim = center.add(velocity.scale(lead));
            t = aim.distanceTo(eye) / speed;
        }
        // Compensate for the arrow's drop over the flight time (0.05 b/t^2).
        aim = aim.add(0, 0.5 * 0.05 * t * t, 0);
        mc.level.addParticle(ParticleTypes.END_ROD, aim.x, aim.y, aim.z, 0, 0, 0);
    }

    /** Muzzle speed in blocks per tick of whatever ranged weapon is held, or 0 if none. */
    private static double projectileSpeed(ItemStack stack) {
        if (stack.getItem() instanceof BowItem) {
            return 3.0;
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return 3.15;
        }
        if (stack.getItem() instanceof TridentItem) {
            return 2.5;
        }
        return 0;
    }

    // ---------------------------------------------------------------- Ballistic Visor

    private static void projectileArcs(Minecraft mc, LocalPlayer player, BodyState body) {
        int tier = body.tierOf(ImplantIds.BALLISTIC_VISOR);
        if (tier == 0) {
            return;
        }
        var stats = Stats.of(ImplantIds.BALLISTIC_VISOR, tier);
        double radius = stats.value("tracking_radius", 8);
        int max = (int) stats.value("max_projectiles", 3);
        int shown = 0;
        for (AbstractArrow arrow : mc.level.getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(radius),
                a -> a.getDeltaMovement().lengthSqr() > 0.05 && a.getOwner() != player)) {
            if (shown++ >= max) {
                break;
            }
            Vec3 pos = arrow.position();
            Vec3 vel = arrow.getDeltaMovement();
            for (int step = 0; step < 36; step++) {
                pos = pos.add(vel);
                vel = vel.scale(0.99).add(0, -0.05, 0);
                if (step % 3 == 0) {
                    mc.level.addParticle(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 0, 0, 0);
                }
                if (!mc.level.getBlockState(net.minecraft.core.BlockPos.containing(pos)).isAir()) {
                    break;
                }
            }
        }
    }

    private ClientOptics() {
    }
}
