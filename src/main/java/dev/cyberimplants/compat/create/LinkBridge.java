package dev.cyberimplants.compat.create;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Virtual Redstone Link nodes for decks and Biomonitor broadcast channels. */
public interface LinkBridge {
    /** A Redstone Link frequency: two filter items. */
    record Frequency(ItemStack first, ItemStack second) {
    }

    /** Strongest signal (0-15) currently transmitted on the frequency. */
    default int receive(Level level, Frequency frequency) {
        return 0;
    }

    /** Transmit {@code strength} on behalf of {@code owner}; 0 stops transmitting. */
    default void transmit(ServerPlayer owner, Frequency frequency, int strength) {
    }
}
