package dev.cyberimplants.implant.neural;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Tags;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.synapse.compiler.Ticks;
import dev.cyberimplants.synapse.vm.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sandevistan (docs/design/implants/a-neural.md): a stationary temporal field. Entities inside run only a fraction
 * of their ticks while their hurt cooldown keeps counting at real speed, so the user gets a hit window.
 * Layer 1: fixed slow, cast on self, no unlocks.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Sandevistan {
    /** An active field. {@code usedFraction} feeds the cooldown refund when it is cut short. */
    private record Field(ResourceKey<Level> dimension, Vec3 center, double radius, long endTick, double scale,
                         int durationTicks, int maxDurationTicks) {
    }

    private static final Map<UUID, Field> FIELDS = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> ACCUMULATORS = new ConcurrentHashMap<>();

    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            if (FIELDS.containsKey(player.getUUID())) {
                return UseResult.BUSY;
            }
            if (!Body.ready(player, ImplantIds.SANDEVISTAN)) {
                return UseResult.COOLDOWN;
            }
            TierStats stats = Stats.of(player, ImplantIds.SANDEVISTAN);
            double radius = stats.value("radius", 3);
            int maxDuration = (int) stats.value("duration_ticks", 40);
            double scale = stats.value("time_scale", 0.25);

            int duration = maxDuration;
            if (ctx.args().get("duration") instanceof Ticks t) {
                duration = (int) Mth.clamp(t.ticks(), 1, maxDuration);
            }
            if (ctx.args().get("radius") instanceof Double r) {
                radius = Mth.clamp(r, 1, radius);
            }
            if (!(ctx.emergency() ? Body.spendEmergency(player, stats.chargeCost()) : Body.spend(player, stats.chargeCost()))) {
                return UseResult.NO_CHARGE;
            }

            Field field = new Field(player.level().dimension(), player.position(), radius,
                    player.level().getGameTime() + duration, scale, duration, maxDuration);
            FIELDS.put(player.getUUID(), field);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.6f);
            DeckEvents.fire(player, "chrono_field_started", Map.of("position", new Position(field.center.x, field.center.y, field.center.z)));
            return UseResult.OK;
        }

        @Override
        public boolean isActive(ServerPlayer player) {
            return FIELDS.containsKey(player.getUUID());
        }
    };

    public static boolean isActive(Player player) {
        return FIELDS.containsKey(player.getUUID());
    }

    public static double radiusMax(Player player) {
        return Stats.value(player, ImplantIds.SANDEVISTAN, "radius", 3);
    }

    public static double durationMax(Player player) {
        return Stats.value(player, ImplantIds.SANDEVISTAN, "duration_ticks", 40);
    }

    /** Ends the owner's field: Temporal Backlash, then the cooldown (cooldowns start when the effect ends). */
    private static void end(ServerPlayer owner) {
        Field field = FIELDS.remove(owner.getUUID());
        if (field == null) {
            return;
        }
        if (FIELDS.isEmpty()) {
            ACCUMULATORS.clear();
        }
        if (owner.isAlive()) {
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
        if (BodyState.get(owner).has(ImplantIds.SANDEVISTAN)) {
            double used = field.maxDurationTicks == 0 ? 1 : (double) field.durationTicks / field.maxDurationTicks;
            int cooldown = Body.cooldownTicks(owner, ImplantIds.SANDEVISTAN);
            Body.startCooldown(owner, ImplantIds.SANDEVISTAN, (int) Math.round(cooldown * (0.5 + 0.5 * used)));
        }
        DeckEvents.fire(owner, "chrono_field_ended", Map.of());
    }

    public static void onBodyChanged(ServerPlayer player) {
        if (!BodyState.get(player).has(ImplantIds.SANDEVISTAN)) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (FIELDS.isEmpty()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity.level().isClientSide || entity instanceof Player) {
            return;
        }
        for (Field field : FIELDS.values()) {
            if (!field.dimension.equals(entity.level().dimension())
                    || entity.position().distanceToSqr(field.center) > field.radius * field.radius) {
                continue;
            }
            double scale = entity.getType().is(Tags.AUGMENT_RESISTANT) ? Math.max(field.scale, 0.5) : field.scale;
            double accumulated = ACCUMULATORS.merge(entity.getUUID(), scale, Double::sum);
            if (accumulated >= 1.0) {
                ACCUMULATORS.put(entity.getUUID(), accumulated - 1.0);
                return; // this tick is allowed through
            }
            event.setCanceled(true);
            if (entity instanceof LivingEntity living) {
                // The hit window: hurt cooldowns keep counting at real speed.
                if (living.invulnerableTime > 0) {
                    living.invulnerableTime--;
                }
                if (living.hurtTime > 0) {
                    living.hurtTime--;
                }
            }
            return;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) {
            return;
        }
        Field field = FIELDS.get(owner.getUUID());
        if (field == null) {
            return;
        }
        ServerLevel level = owner.serverLevel();
        if (!owner.isAlive() || !level.dimension().equals(field.dimension) || level.getGameTime() >= field.endTick) {
            end(owner);
            return;
        }
        // Other players cannot have ticks skipped: slow them instead (docs/design/08-technical-architecture.md, 8.7).
        if (owner.tickCount % 10 == 0) {
            for (ServerPlayer other : level.players()) {
                if (other != owner && other.position().distanceToSqr(field.center) <= field.radius * field.radius) {
                    other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 2, false, true));
                }
            }
        }
        if (owner.tickCount % 3 == 0) {
            for (int i = 0; i < 12; i++) {
                Vec3 dir = new Vec3(level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian());
                if (dir.lengthSqr() < 1.0E-4) {
                    continue;
                }
                Vec3 p = field.center.add(dir.normalize().scale(field.radius));
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1.0, p.z, 1, 0, 0, 0, 0.0);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player);
        }
    }

    private Sandevistan() {
    }
}
