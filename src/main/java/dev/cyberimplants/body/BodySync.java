package dev.cyberimplants.body;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Batches body-state syncs. Discrete changes mark the player dirty and are flushed at the end of the tick; Charge
 * and other continuous values are sent at most every {@link #INTERVAL} ticks, and only when they changed.
 */
public final class BodySync {
    public static final int INTERVAL = 10;

    private static final Map<UUID, BodyState> LAST_SENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> DIRTY = new ConcurrentHashMap<>();

    public static void markDirty(ServerPlayer player) {
        DIRTY.put(player.getUUID(), Boolean.TRUE);
    }

    /** Called once per player tick after all implants have run. */
    public static void flush(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean dirty = DIRTY.remove(id) != null;
        boolean periodic = player.tickCount % INTERVAL == 0;
        if (!dirty && !periodic) {
            return;
        }
        BodyState now = BodyState.get(player);
        BodyState last = LAST_SENT.get(id);
        if (!dirty && last != null && sameForClient(last, now)) {
            return;
        }
        LAST_SENT.put(id, now);
        BodyEvents.syncBody(player);
    }

    /** Forget a player (logout) so a rejoin always sends a full snapshot. */
    public static void forget(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
        DIRTY.remove(player.getUUID());
    }

    /** Charge is compared at 0.5 C resolution so slow metabolic recharge doesn't resend every interval. */
    private static boolean sameForClient(BodyState a, BodyState b) {
        return a.installed().equals(b.installed())
                && a.cooldownEnd().equals(b.cooldownEnd())
                && a.data().equals(b.data())
                && Math.abs(a.charge() - b.charge()) < 0.5
                && Math.abs(a.buffer() - b.buffer()) < 0.5;
    }

    private BodySync() {
    }
}
