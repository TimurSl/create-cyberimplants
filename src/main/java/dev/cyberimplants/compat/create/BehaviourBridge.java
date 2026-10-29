package dev.cyberimplants.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.OptionalInt;

/** Value boxes (scroll value behaviour) and filters (filtering behaviour) on Create block entities. */
public interface BehaviourBridge {
    default OptionalInt readValue(Level level, BlockPos pos) {
        return OptionalInt.empty();
    }

    default boolean writeValue(Level level, BlockPos pos, int value) {
        return false;
    }

    default Optional<ItemStack> readFilter(Level level, BlockPos pos) {
        return Optional.empty();
    }

    default boolean writeFilter(Level level, BlockPos pos, ItemStack filter) {
        return false;
    }
}
