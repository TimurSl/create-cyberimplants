package dev.cyberimplants.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Create 6 stock networks for Machine Interface Tier V (Layer 5): which network a linked block (Stock Link, Stock
 * Ticker, Redstone Requester ...) belongs to, stock reads and package requests.
 */
public interface LogisticsBridge {
    /** The network of the logistically linked block at {@code pos}, if the player may use it. */
    default Optional<UUID> network(ServerPlayer player, BlockPos pos) {
        return Optional.empty();
    }

    /** How many of {@code item} the network holds. */
    default OptionalInt stock(UUID network, ItemStack item) {
        return OptionalInt.empty();
    }

    /** Sends {@code count} of {@code item} from the network to a package address. */
    default boolean request(UUID network, ItemStack item, int count, String address) {
        return false;
    }
}
