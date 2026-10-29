package dev.cyberimplants.compat.create;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Moving contraptions and trains under an entity: velocity, for Magnetic Soles and Momentum Booster. */
public interface ContraptionBridge {
    /** Velocity (blocks/tick) of the contraption the entity is standing on, if any. */
    default Optional<Vec3> supportVelocity(Entity entity) {
        return Optional.empty();
    }
}
