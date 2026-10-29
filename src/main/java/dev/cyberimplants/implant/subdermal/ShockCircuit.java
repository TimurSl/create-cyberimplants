package dev.cyberimplants.implant.subdermal;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.deck.EntityHandle;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shock Circuit: melee attackers take an electric discharge. Projectiles don't trigger it. Each proc costs Charge,
 * and a Creeper that is shocked becomes charged, as if struck by lightning.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class ShockCircuit {
    private static final double CHAIN_RANGE = 4.0;
    private static final int SLOW_TICKS = 20;
    private static final Map<UUID, Long> LAST_PROC = new ConcurrentHashMap<>();

    private static void shock(ServerLevel level, ServerPlayer player, LivingEntity target, float damage, int tier) {
        target.hurt(level.damageSources().indirectMagic(player, player), damage);
        if (Stats.unlocked(ImplantIds.SHOCK_CIRCUIT, tier, "slows_attacker")) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 0));
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.SHOCK_CIRCUIT);
        if (tier == 0) {
            return;
        }
        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        // Melee only: the entity that caused the damage is the one standing next to the player.
        if (!(direct instanceof LivingEntity attacker) || direct != source.getEntity() || attacker == player) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.SHOCK_CIRCUIT, tier);
        long now = player.level().getGameTime();
        if (now - LAST_PROC.getOrDefault(player.getUUID(), -1_000_000L) < stats.value("internal_cooldown_ticks", 30)) {
            return;
        }
        if (!Body.spend(player, stats.chargeCost())) {
            return;
        }
        LAST_PROC.put(player.getUUID(), now);
        ServerLevel level = player.serverLevel();
        // Magic damage ignores armor; half the discharge is meant to pierce, so deal it as magic.
        float damage = (float) stats.value("damage", 2.0);
        shock(level, player, attacker, damage, tier);
        if (Stats.unlocked(ImplantIds.SHOCK_CIRCUIT, tier, "chain_one")) {
            // Tier III: the discharge arcs to one more hostile within 4 blocks.
            attacker.level().getEntitiesOfClass(LivingEntity.class, attacker.getBoundingBox().inflate(CHAIN_RANGE),
                            e -> e != player && e != attacker && e.isAlive() && e instanceof Enemy)
                    .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(attacker)))
                    .ifPresent(next -> shock(level, player, next, damage, tier));
        }
        if (attacker instanceof Creeper creeper && !creeper.isPowered()) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                creeper.thunderHit(level, bolt);
            }
        }
        DeckEvents.fire(player, "shock_proc", Map.of("attacker", EntityHandle.of(player, attacker)));
    }

    private ShockCircuit() {
    }
}
