package dev.cyberimplants.deck;

import dev.cyberimplants.implant.optical.EntityKinds;
import dev.cyberimplants.synapse.vm.Handle;
import dev.cyberimplants.synapse.vm.Position;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * A script's view of an entity (docs/design/04-synapse-language.md, section 10.3). It holds the UUID, not the
 * entity, so it becomes invalid (reads {@code NULL}) when the entity is gone.
 */
public final class EntityHandle implements Handle {
    private final ServerPlayer viewer;
    private final UUID uuid;

    private EntityHandle(ServerPlayer viewer, UUID uuid) {
        this.viewer = viewer;
        this.uuid = uuid;
    }

    public static EntityHandle of(ServerPlayer viewer, Entity entity) {
        return new EntityHandle(viewer, entity.getUUID());
    }

    public UUID uuid() {
        return uuid;
    }

    /** The entity, or null if it is not loaded or has been removed. */
    public Entity resolve() {
        Entity e = viewer.serverLevel().getEntity(uuid);
        return e == null || e.isRemoved() ? null : e;
    }

    @Override
    public Object get(String member) {
        Entity e = resolve();
        if (e == null) {
            return member.equals("alive") ? Boolean.FALSE : null;
        }
        return switch (member) {
            case "type" -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
            case "kind" -> EntityKinds.classify(e);
            case "distance" -> (double) e.distanceTo(viewer);
            case "position" -> new Position(e.getX(), e.getY(), e.getZ());
            case "alive" -> e.isAlive();
            case "visible" -> viewer.hasLineOfSight(e);
            default -> null;
        };
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EntityHandle h && h.uuid.equals(uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
