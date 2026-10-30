package dev.cyberimplants.implant.circulatory;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.Attrs;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Adrenaline Pump: a sizeable hit floods the system with a speed surge, then a short crash. Costs one hunger point. */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class AdrenalinePump {
    private static final Map<UUID, Long> BOOST_END = new ConcurrentHashMap<>();
    /** One hunger point is 4 exhaustion. */
    private static final float HUNGER_POINT = 4.0f;
    /** "Jump +0.5": half a block of extra jump height is roughly +0.06 jump strength. */
    private static final double JUMP_BONUS = 0.06;
    private static final double ATTACK_SPEED_BONUS = 0.15;
    /** Tier V Last Stand (Layer 5): the boost doubles when the hit leaves the user below this share of health. */
    private static final double LAST_STAND_HEALTH = 0.3;

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.ADRENALINE_PUMP);
        if (tier == 0 || BOOST_END.containsKey(player.getUUID()) || !Body.ready(player, ImplantIds.ADRENALINE_PUMP)) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.ADRENALINE_PUMP, tier);
        if (event.getNewDamage() < stats.value("trigger_damage", 3.0)) {
            return;
        }
        player.getFoodData().addExhaustion(HUNGER_POINT);
        int boostTicks = (int) stats.value("boost_ticks", 60);
        BOOST_END.put(player.getUUID(), player.level().getGameTime() + boostTicks);
        ImplantFx.adrenaline(player, boostTicks);
        boolean lastStand = player.getHealth() < player.getMaxHealth() * LAST_STAND_HEALTH
                && Stats.unlocked(ImplantIds.ADRENALINE_PUMP, tier, "last_stand");
        double boost = stats.value("boost", 0.15) * (lastStand ? 2.0 : 1.0);
        Attrs.multiplied(player, Attributes.MOVEMENT_SPEED, "adrenaline_boost", boost, true);
        applyUnlocks(player, tier, true);
        Body.startCooldown(player, ImplantIds.ADRENALINE_PUMP, stats.cooldownTicks());
        DeckEvents.fire(player, "adrenaline_triggered", Map.of());
    }

    /** Tier III adds jump height, Tier IV attack speed; both last as long as the boost. */
    private static void applyUnlocks(ServerPlayer player, int tier, boolean present) {
        Attrs.added(player, Attributes.JUMP_STRENGTH, "adrenaline_jump", JUMP_BONUS,
                present && Stats.unlocked(ImplantIds.ADRENALINE_PUMP, tier, "boost_jump"));
        Attrs.multiplied(player, Attributes.ATTACK_SPEED, "adrenaline_attack_speed", ATTACK_SPEED_BONUS,
                present && Stats.unlocked(ImplantIds.ADRENALINE_PUMP, tier, "boost_attack_speed"));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Long end = BOOST_END.get(player.getUUID());
        // Tier II ignores Slowness during the boost (the crash comes afterwards).
        if (end != null && Stats.unlocked(player, ImplantIds.ADRENALINE_PUMP, "ignores_slowness")) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (end != null && (!player.isAlive() || player.level().getGameTime() >= end)) {
            finish(player, player.isAlive());
        } else if (end != null) {
            ImplantFx.adrenalineTick(player, end - player.level().getGameTime());
        }
    }

    private static void finish(ServerPlayer player, boolean crash) {
        BOOST_END.remove(player.getUUID());
        Attrs.multiplied(player, Attributes.MOVEMENT_SPEED, "adrenaline_boost", 0, false);
        applyUnlocks(player, 0, false);
        if (crash) {
            int tier = BodyState.get(player).tierOf(ImplantIds.ADRENALINE_PUMP);
            int ticks = (int) Stats.of(ImplantIds.ADRENALINE_PUMP, Math.max(tier, 1)).value("crash_ticks", 80);
            if (tier > 0 && ticks > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && BOOST_END.containsKey(player.getUUID())) {
            finish(player, false);
        }
    }

    private AdrenalinePump() {
    }
}
