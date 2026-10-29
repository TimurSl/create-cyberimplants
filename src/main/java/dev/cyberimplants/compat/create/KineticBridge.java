package dev.cyberimplants.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.OptionalDouble;

/** Speed, direction, network stress and overstress of any kinetic block entity. */
public interface KineticBridge {
    record NetworkStress(double used, double capacity) {
    }

    /** Signed rotational speed in RPM, or empty if the block is not kinetic or not loaded. */
    default OptionalDouble speed(Level level, BlockPos pos) {
        return OptionalDouble.empty();
    }

    default Optional<NetworkStress> networkStress(Level level, BlockPos pos) {
        return Optional.empty();
    }

    default boolean overstressed(Level level, BlockPos pos) {
        return false;
    }
}
