package dev.cyberimplants.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/** Reads whatever a Display Link could read from the block at {@code pos}. */
public interface DisplaySourceBridge {
    default List<Component> read(Level level, BlockPos pos) {
        return List.of();
    }
}
