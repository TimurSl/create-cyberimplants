package dev.cyberimplants.implant.circulatory;

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
        BOOST_END.put(player.getUUID(), player.level().getGameTime() + (long) stats.value("boost_ticks", 60));
        Attrs.multiplied(player, Attributes.MOVEMENT_SPEED, "adrenaline_boost", stats.value("boost", 0.15), true);
        Body.startCooldown(player, ImplantIds.ADRENALINE_PUMP, stats.cooldownTicks());
        DeckEvents.fire(player, "adrenaline_triggered", Map.of());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Long end = BOOST_END.get(player.getUUID());
        if (end != null && (!player.isAlive() || player.level().getGameTime() >= end)) {
            finish(player, player.isAlive());
        }
    }

    private static void finish(ServerPlayer player, boolean crash) {
        BOOST_END.remove(player.getUUID());
        Attrs.multiplied(player, Attributes.MOVEMENT_SPEED, "adrenaline_boost", 0, false);
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
