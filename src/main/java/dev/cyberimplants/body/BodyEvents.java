package dev.cyberimplants.body;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.data.ImplantStatsLoader;
import dev.cyberimplants.data.ImplantStatsRegistry;
import dev.cyberimplants.network.BodySnapshotPayload;
import dev.cyberimplants.network.ImplantStatsSyncPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Game-bus hooks: data reload, and syncing state to clients. */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class BodyEvents {
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

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncBody(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncBody(player);
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
