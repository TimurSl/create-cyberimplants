package dev.cyberimplants.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/** Engineer's Goggles tooltip lines for the Machine Analyzer. */
public interface GoggleBridge {
    default List<Component> tooltip(Level level, BlockPos pos, Player viewer) {
        return List.of();
    }
}
