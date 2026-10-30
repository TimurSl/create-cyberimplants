package dev.cyberimplants.implant.subdermal;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Tags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kinetic Shield: a regenerating absorb pool that takes a share of each hit. The pool lives in the synced body data
 * (key {@code shield}; -1 means "full") so the HUD can show it. Recharging costs Charge. Tier V Overcharge (Layer 5):
 * once the pool is full, an installed Energy Buffer keeps feeding it up to 150%.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class KineticShield {
    public static final String KEY = "shield";
    private static final Map<UUID, Long> LAST_HIT = new ConcurrentHashMap<>();

    private static final double OVERCHARGE = 1.5;

    public static double max(Player player) {
        return Stats.value(player, ImplantIds.KINETIC_SHIELD, "capacity", 4);
    }

    /** The highest the pool can go: {@link #max} or, with Overcharge and an Energy Buffer installed, 150% of it. */
    public static double cap(Player player) {
        boolean overcharge = BodyState.get(player).has(ImplantIds.ENERGY_BUFFER)
                && Stats.unlocked(player, ImplantIds.KINETIC_SHIELD, "overcharge");
        return max(player) * (overcharge ? OVERCHARGE : 1.0);
    }

    public static double value(Player player) {
        BodyState s = BodyState.get(player);
        if (!s.has(ImplantIds.KINETIC_SHIELD)) {
            return 0;
        }
        double v = s.number(KEY, -1);
        return v < 0 ? max(player) : Math.min(v, cap(player));
    }

    /** Takes the shield's share of {@code amount}; returns what is left to hit the player. */
    public static float absorb(ServerPlayer player, DamageSource source, float amount) {
        BodyState s = BodyState.get(player);
        int tier = s.tierOf(ImplantIds.KINETIC_SHIELD);
        if (tier == 0 || amount <= 0 || source.is(Tags.SHIELD_BYPASS)) {
            return amount;
        }
        TierStats stats = Stats.of(ImplantIds.KINETIC_SHIELD, tier);
        double pool = value(player);
        LAST_HIT.put(player.getUUID(), player.level().getGameTime());
        if (pool <= 0) {
            return amount;
        }
        // Tier III: projectiles are absorbed in full (while the pool lasts), everything else by the usual share.
        double share = source.is(DamageTypeTags.IS_PROJECTILE) && Stats.unlocked(ImplantIds.KINETIC_SHIELD, tier, "absorbs_projectiles")
                ? 1.0 : stats.value("absorb_share", 0.5);
        double absorbed = Math.min(pool, amount * share);
        double left = pool - absorbed;
        Body.updateQuiet(player, st -> st.withData(KEY, Math.max(0.0, left)));
        if (absorbed > 0) {
            ImplantFx.shieldAbsorb(player, absorbed, left <= 1.0E-6);
        }
        if (left <= 1.0E-6 && absorbed > 0) {
            DeckEvents.fire(player, "shield_broken", Map.of());
        }
        return (float) (amount - absorbed);
    }

    /** Emergency recharge (ERS or deck): refills half the pool at once. 45 s cooldown, Energy Buffer allowed. */
    public static boolean emergencyRecharge(ServerPlayer player) {
        BodyState s = BodyState.get(player);
        int tier = s.tierOf(ImplantIds.KINETIC_SHIELD);
        if (tier == 0 || !Body.ready(player, ImplantIds.KINETIC_SHIELD)) {
            return false;
        }
        TierStats stats = Stats.of(ImplantIds.KINETIC_SHIELD, tier);
        double target = Math.max(value(player), max(player) * 0.5);
        double added = target - value(player);
        if (added <= 0 || !Body.spendEmergency(player, added * stats.value("charge_per_hp", 3.0))) {
            return false;
        }
        Body.updateQuiet(player, st -> st.withData(KEY, target));
        ImplantFx.shieldEmergency(player);
        Body.startCooldown(player, ImplantIds.KINETIC_SHIELD, (int) stats.value("emergency_cooldown_ticks", 900));
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.KINETIC_SHIELD);
        if (tier == 0) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.KINETIC_SHIELD, tier);
        double pool = value(player);
        double full = max(player);
        double cap = cap(player);
        if (pool >= cap) {
            return;
        }
        long sinceHit = player.level().getGameTime() - LAST_HIT.getOrDefault(player.getUUID(), -1_000_000L);
        if (sinceHit < stats.value("recharge_delay_ticks", 160)) {
            return;
        }
        boolean overcharging = pool >= full;
        double add = Math.min((overcharging ? cap : full) - pool, stats.value("recharge_rate", 0.5) * 0.5); // per 10 ticks
        double cost = add * stats.value("charge_per_hp", 3.0);
        if (overcharging ? Body.spendBuffer(player, cost) : Body.spend(player, cost)) {
            double next = pool + add;
            Body.updateQuiet(player, st -> st.withData(KEY, next));
            if (!overcharging && next >= full - 1.0E-6) {
                ImplantFx.shieldFull(player);
                DeckEvents.fire(player, "shield_full", Map.of());
            }
        }
    }

    public static void onBodyChanged(ServerPlayer player) {
        BodyState s = BodyState.get(player);
        if (!s.has(ImplantIds.KINETIC_SHIELD) && s.number(KEY, -1) != -1) {
            Body.update(player, st -> st.withData(KEY, -1));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HIT.remove(event.getEntity().getUUID());
    }

    private KineticShield() {
    }
}
