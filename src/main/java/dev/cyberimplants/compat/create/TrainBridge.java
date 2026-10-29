package dev.cyberimplants.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Train station state (Machine Interface tier 4). */
public interface TrainBridge {
    record StationInfo(String stationName, boolean trainPresent, String trainName) {
    }

    default Optional<StationInfo> station(Level level, BlockPos pos) {
        return Optional.empty();
    }
}
