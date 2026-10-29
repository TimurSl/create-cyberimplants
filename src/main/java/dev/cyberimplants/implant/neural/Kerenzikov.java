package dev.cyberimplants.implant.neural;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.Attrs;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kerenzikov: a manual dodge that opens a short reaction window in which attack cooldown is effectively reset.
 * The dash itself is applied by the client (movement is client-authoritative); the server validates, charges and
 * runs the window (docs/design/08-technical-architecture.md, section 8.6).
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Kerenzikov {
    private static final Map<UUID, Long> WINDOW_END = new ConcurrentHashMap<>();

    public static boolean windowActive(Player player) {
        Long end = WINDOW_END.get(player.getUUID());
        return end != null && player.level().getGameTime() < end;
    }

    /** Validates a client-reported dodge. Returns false (and the caller resyncs the client) if it is not allowed. */
    public static boolean dodge(ServerPlayer player, float dirX, float dirZ) {
        BodyState body = BodyState.get(player);
        int tier = body.tierOf(ImplantIds.KERENZIKOV);
        if (tier == 0 || player.isPassenger() || player.isFallFlying() || player.isSwimming() || player.isInWater()) {
            return false;
        }
        if (!Body.ready(player, ImplantIds.KERENZIKOV)) {
            return false;
        }
        TierStats stats = Stats.of(ImplantIds.KERENZIKOV, tier);
        if (!Body.spend(player, stats.chargeCost())) {
            return false;
        }
        Body.startCooldown(player, ImplantIds.KERENZIKOV, stats.cooldownTicks());
        double direction = Math.toDegrees(Math.atan2(dirZ, dirX));
        openWindow(player, tier, direction);
        DeckEvents.fire(player, "dodge", Map.of("direction", direction));
        return true;
    }

    /** Opens the reaction window without a dodge, its Charge or its cooldown (the AES synergy uses this). */
    public static void openWindow(ServerPlayer player, int tier, double direction) {
        long end = player.level().getGameTime() + (long) Stats.of(ImplantIds.KERENZIKOV, tier).value("window_ticks", 8);
        WINDOW_END.put(player.getUUID(), end);
        // Hold the attack-strength meter full for the window: a huge attack speed finishes the cooldown instantly.
        Attrs.added(player, Attributes.ATTACK_SPEED, "kerenzikov_window", 100.0, true);
        DeckEvents.fire(player, "window_opened", Map.of("direction", direction));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Long end = WINDOW_END.get(player.getUUID());
        if (end != null && player.level().getGameTime() >= end) {
            WINDOW_END.remove(player.getUUID());
            Attrs.added(player, Attributes.ATTACK_SPEED, "kerenzikov_window", 0, false);
        }
    }

    public static void onBodyChanged(ServerPlayer player) {
        if (!BodyState.get(player).has(ImplantIds.KERENZIKOV)) {
            WINDOW_END.remove(player.getUUID());
            Attrs.added(player, Attributes.ATTACK_SPEED, "kerenzikov_window", 0, false);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WINDOW_END.remove(event.getEntity().getUUID());
    }

    private Kerenzikov() {
    }
}
