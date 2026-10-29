package dev.cyberimplants.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;

/** Engineer's Goggles integration for the Machine Analyzer. */
public interface GoggleBridge {
    default List<Component> tooltip(Level level, BlockPos pos, Player viewer) {
        return List.of();
    }

    /** Makes Create treat players matching the predicate as wearing Engineer's Goggles (goggle overlays and tooltips). */
    default void registerWearingPredicate(Predicate<Player> predicate) {
    }
}
