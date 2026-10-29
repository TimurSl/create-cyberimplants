package dev.cyberimplants.body;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.data.ImplantStatsLoader;
import dev.cyberimplants.data.ImplantStatsRegistry;
import dev.cyberimplants.deck.DeckService;
import dev.cyberimplants.implant.ImplantRuntime;
import dev.cyberimplants.implant.PassiveModifiers;
import dev.cyberimplants.network.BodySnapshotPayload;
import dev.cyberimplants.network.ImplantStatsSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Game-bus hooks: data reload, per-tick body upkeep, and syncing state to clients. */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class BodyEvents {
    /** The respawn battery level (docs/design/01-core-systems.md, section 1.8). */
    private static final double RESPAWN_CHARGE_FRACTION = 0.25;

    public static void syncBody(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new BodySnapshotPayload(BodyState.get(player)));
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ImplantStatsLoader());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ImplantStatsSyncPayload payload = new ImplantStatsSyncPayload(ImplantStatsRegistry.all());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    /** Runs once per server player tick: time, metabolic recharge, attribute upkeep, the deck, then the client sync. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BodyState state = BodyState.get(player);
        if (state.installed().isEmpty() && state.data().isEmpty()) {
            return; // an unmodified body costs nothing
        }
        Body.updateQuiet(player, s -> s.withOnlineTicks(s.onlineTicks() + 1));
        metabolicRecharge(player);
        if (player.tickCount % 5 == 0) {
            PassiveModifiers.apply(player);
        }
        DeckService.tick(player);
        BodySync.flush(player);
    }

    /** The Core converts food into Charge: 1 C costs 0.1 exhaustion, and it stops when hunger is at 6 or below. */
    private static void metabolicRecharge(ServerPlayer player) {
        BodyState s = BodyState.get(player);
        double max = Body.maxCharge(s);
        if (max <= 0 || s.charge() >= max || player.getFoodData().getFoodLevel() <= 6 || !player.isAlive()) {
            return;
        }
        double add = Math.min(Body.metabolicRate(s) / 20.0, max - s.charge());
        Body.updateQuiet(player, st -> st.withCharge(st.charge() + add, st.buffer()));
        player.getFoodData().addExhaustion((float) (add * 0.1));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImplantRuntime.bodyChanged(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeckService.shutdown(player);
            BodySync.forget(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // The new player entity keeps the body (copy on death) but starts with a quarter battery.
            DeckService.shutdown(player);
            Body.update(player, s -> s.withCharge(Body.maxCharge(s) * RESPAWN_CHARGE_FRACTION, 0));
            ImplantRuntime.bodyChanged(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncBody(player);
        }
    }

    private BodyEvents() {
    }
}
