package dev.cyberimplants.client;

import dev.cyberimplants.implant.Appearance;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** {@link Appearance} bits of every player this client has heard about, by network id. */
public final class ClientAppearance {
    private static final Map<Integer, Integer> BITS = new ConcurrentHashMap<>();

    public static void set(int entityId, int bits) {
        if (bits == 0) {
            BITS.remove(entityId);
        } else {
            BITS.put(entityId, bits);
        }
    }

    public static int of(Entity entity) {
        return BITS.getOrDefault(entity.getId(), 0);
    }

    public static boolean has(Entity entity, int flag) {
        return (of(entity) & flag) != 0;
    }

    public static void clear() {
        BITS.clear();
    }

    private ClientAppearance() {
    }
}
