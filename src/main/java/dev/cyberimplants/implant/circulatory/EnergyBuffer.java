package dev.cyberimplants.implant.circulatory;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Energy Buffer: a reserve pool that only Emergency activations may draw on. It fills only when the battery is full.
 * Tier V Surge (Layer 5): once per minute an ordinary activation the battery cannot pay for may draw on it too.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class EnergyBuffer {
    private static final long SURGE_INTERVAL_TICKS = 1200;
    private static final Map<UUID, Long> LAST_SURGE = new ConcurrentHashMap<>();

    /**
     * Whether a Surge may cover {@code cost} now (battery and buffer together hold enough and the minute has passed). Uses
     * the Surge up when it says yes; the caller then drains battery and buffer.
     */
    public static boolean surge(ServerPlayer player, double cost) {
        BodyState s = BodyState.get(player);
        if (s.charge() + s.buffer() + 1e-9 < cost || !Stats.unlocked(player, ImplantIds.ENERGY_BUFFER, "surge")) {
            return false;
        }
        long now = player.level().getGameTime();
        Long last = LAST_SURGE.get(player.getUUID());
        if (last != null && now - last < SURGE_INTERVAL_TICKS) {
            return false;
        }
        LAST_SURGE.put(player.getUUID(), now);
        ImplantFx.bufferFull(player);
        return true;
    }

    public static double capacity(Player player) {
        return Body.bufferCapacity(BodyState.get(player));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        BodyState s = BodyState.get(player);
        int tier = s.tierOf(ImplantIds.ENERGY_BUFFER);
        if (tier == 0 || !Body.chargeEnabled()) {
            return;
        }
        double cap = Body.bufferCapacity(s);
        if (s.buffer() >= cap || s.charge() < Body.maxCharge(s) - 0.01) {
            return;
        }
        double add = Math.min(cap - s.buffer(), Stats.of(ImplantIds.ENERGY_BUFFER, tier).value("fill_rate", 1.0) * 0.5);
        Body.updateQuiet(player, st -> st.withCharge(st.charge(), st.buffer() + add));
        if (s.buffer() + add >= cap - 1.0E-6) {
            ImplantFx.bufferFull(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SURGE.remove(event.getEntity().getUUID());
    }

    private EnergyBuffer() {
    }
}
