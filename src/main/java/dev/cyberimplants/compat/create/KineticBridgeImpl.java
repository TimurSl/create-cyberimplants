package dev.cyberimplants.compat.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.OptionalDouble;

/** Reads Create kinetic block entities. Layer 1 needs only the shaft speed (Neural Port, rotation read-only). */
final class KineticBridgeImpl implements KineticBridge {
    @Override
    public OptionalDouble speed(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return OptionalDouble.empty();
        }
        if (level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) {
            return OptionalDouble.of(kinetic.getSpeed());
        }
        return OptionalDouble.empty();
    }

    @Override
    public java.util.Optional<NetworkStress> networkStress(Level level, BlockPos pos) {
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic) {
            var network = kinetic.getOrCreateNetwork();
            if (network != null) {
                return java.util.Optional.of(new NetworkStress(network.calculateStress(), network.calculateCapacity()));
            }
        }
        return java.util.Optional.empty();
    }

    @Override
    public boolean overstressed(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic && kinetic.isOverStressed();
    }
}
