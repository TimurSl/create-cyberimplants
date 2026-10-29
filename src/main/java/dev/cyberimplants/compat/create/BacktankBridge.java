package dev.cyberimplants.compat.create;

import net.minecraft.world.entity.player.Player;

/** Pressurised air from a worn backtank, for pneumatic implants. */
public interface BacktankBridge {
    default int airAvailable(Player player) {
        return 0;
    }

    default boolean consumeAir(Player player, int amount) {
        return false;
    }
}
