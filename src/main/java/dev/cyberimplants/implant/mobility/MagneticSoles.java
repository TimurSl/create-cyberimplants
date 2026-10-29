package dev.cyberimplants.implant.mobility;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
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
 * Magnetic Soles: a toggle that lets the user walk up metal walls. The climbing itself runs on the client; it reports
 * attach / detach and refreshes the attachment every few ticks, and the server bills the upkeep while attached.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class MagneticSoles {
    private static final Map<UUID, Long> ATTACHED_UNTIL = new ConcurrentHashMap<>();
    /** The client re-reports at least this often while attached. */
    private static final long LEASE_TICKS = 12;

    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            boolean next = !Toggles.isOn(player, ImplantIds.MAGNETIC_SOLES);
            if (next && !Body.canAfford(player, 1.0)) {
                return UseResult.NO_CHARGE;
            }
            Toggles.set(player, ImplantIds.MAGNETIC_SOLES, next);
            ImplantFx.soles(player, next);
            if (!next) {
                detach(player);
            }
            return UseResult.OK;
        }

        @Override
        public boolean isActive(ServerPlayer player) {
            return Toggles.isOn(player, ImplantIds.MAGNETIC_SOLES);
        }

        @Override
        public boolean stop(ServerPlayer player) {
            if (!isActive(player)) {
                return false;
            }
            Toggles.set(player, ImplantIds.MAGNETIC_SOLES, false);
            detach(player);
            return true;
        }
    };

    public static boolean attached(Player player) {
        Long until = ATTACHED_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    /** Client report: the user is (still) attached to a magnetic surface. */
    public static void reportAttached(ServerPlayer player) {
        if (!Toggles.isOn(player, ImplantIds.MAGNETIC_SOLES)) {
            return;
        }
        boolean was = attached(player);
        ATTACHED_UNTIL.put(player.getUUID(), player.level().getGameTime() + LEASE_TICKS);
        if (!was) {
            ImplantFx.solesClamp(player);
            DeckEvents.fire(player, "attached", Map.of("surface", "metal"));
        }
    }

    public static void detach(ServerPlayer player) {
        if (ATTACHED_UNTIL.remove(player.getUUID()) != null) {
            DeckEvents.fire(player, "detached", Map.of());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Long until = ATTACHED_UNTIL.get(player.getUUID());
        if (until == null) {
            return;
        }
        if (player.level().getGameTime() >= until) {
            detach(player);
            return;
        }
        if (player.tickCount % 4 == 0) {
            ImplantFx.solesHold(player);
        }
        double perSecond = Stats.of(ImplantIds.MAGNETIC_SOLES, BodyState.get(player).tierOf(ImplantIds.MAGNETIC_SOLES))
                .value("upkeep_per_second", 0.5);
        if (!Body.spend(player, perSecond / 20.0)) {
            Toggles.set(player, ImplantIds.MAGNETIC_SOLES, false);
            detach(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ATTACHED_UNTIL.remove(event.getEntity().getUUID());
    }

    private MagneticSoles() {
    }
}
