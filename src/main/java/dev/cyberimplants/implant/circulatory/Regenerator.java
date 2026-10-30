package dev.cyberimplants.implant.circulatory;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
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
 * Regenerator: out-of-combat healing paid for with Charge instead of food. Stops for a lockout after any damage.
 * Tier V adds Burst (Layer 5): 3 s at triple rate that ignores the lockout, used from the Implant Wheel or a deck.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Regenerator {
    private static final int BURST_TICKS = 60;
    private static final double BURST_RATE = 3.0;
    private static final Map<UUID, Long> LAST_HURT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_HEAL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BURST_UNTIL = new ConcurrentHashMap<>();

    /** Burst: the action ImplantActions only offers once the installed tier has the {@code burst} unlock. */
    public static final ImplantAction BURST = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            if (bursting(player)) {
                return UseResult.BUSY;
            }
            if (!Body.ready(player, ImplantIds.REGENERATOR)) {
                return UseResult.COOLDOWN;
            }
            long now = player.level().getGameTime();
            BURST_UNTIL.put(player.getUUID(), now + BURST_TICKS);
            Body.startCooldown(player, ImplantIds.REGENERATOR, Body.cooldownTicks(player, ImplantIds.REGENERATOR));
            ImplantFx.regenerate(player);
            return UseResult.OK;
        }

        @Override
        public boolean isActive(ServerPlayer player) {
            return bursting(player);
        }
    };

    public static boolean bursting(Player player) {
        Long until = BURST_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    public static boolean healing(Player player) {
        Long last = LAST_HEAL.get(player.getUUID());
        return last != null && player.level().getGameTime() - last <= 25;
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_HURT.put(player.getUUID(), player.level().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.REGENERATOR);
        if (tier == 0 || !player.isAlive() || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.REGENERATOR, tier);
        long now = player.level().getGameTime();
        boolean burst = bursting(player);
        if (!burst && now - LAST_HURT.getOrDefault(player.getUUID(), -1_000_000L) < stats.value("lockout_ticks", 160)) {
            return;
        }
        if (player.getFoodData().getFoodLevel() <= 0 && !Stats.unlocked(ImplantIds.REGENERATOR, tier, "works_when_starving")) {
            return;
        }
        float heal = (float) (stats.value("heal_per_second", 0.25) * (burst ? BURST_RATE : 1.0));
        if (Body.spend(player, heal * stats.value("charge_per_hp", 2.0))) {
            player.heal(heal);
            LAST_HEAL.put(player.getUUID(), now);
            ImplantFx.regenerate(player);
            cure(player, tier);
        }
    }

    /** Tier III cures Poison, Tier IV also Wither and Hunger. */
    private static void cure(ServerPlayer player, int tier) {
        if (Stats.unlocked(ImplantIds.REGENERATOR, tier, "cures_poison")) {
            player.removeEffect(MobEffects.POISON);
        }
        if (Stats.unlocked(ImplantIds.REGENERATOR, tier, "cures_wither_hunger")) {
            player.removeEffect(MobEffects.WITHER);
            player.removeEffect(MobEffects.HUNGER);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HURT.remove(event.getEntity().getUUID());
        LAST_HEAL.remove(event.getEntity().getUUID());
        BURST_UNTIL.remove(event.getEntity().getUUID());
    }

    private Regenerator() {
    }
}
