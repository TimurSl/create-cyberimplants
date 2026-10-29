package dev.cyberimplants.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/** Writes text lines to display targets (Display Boards, Nixie Tubes, signs). */
public interface DisplayTargetBridge {
    default boolean write(Level level, BlockPos pos, List<Component> lines) {
        return false;
    }
}
