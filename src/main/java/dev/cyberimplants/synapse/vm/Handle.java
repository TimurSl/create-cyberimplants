package dev.cyberimplants.synapse.vm;

import dev.cyberimplants.synapse.IsaLevel;

import java.util.List;
import java.util.Map;

/** An object scripts can read members from: an implant, an entity, the event payload. */
public interface Handle {
    /** The member's value, or null if absent. Values are Double, Boolean, String, Ticks, Position, Handle or null. */
    Object get(String member);

    /** Writes a member. Returns false if it is unknown or read-only. */
    default boolean set(String member, Object value) {
        return false;
    }

    /** Cycles charged for a write (docs/design/04-synapse-language.md, section 8.1: 10 for implants, 8 for devices). */
    default int writeCost() {
        return 10;
    }

    /** Cycles charged for a read; hosts may raise this for expensive sources. */
    default int readCost() {
        return 2;
    }

    /**
     * Calls a method such as {@code predict.position(entity, 2)}. {@code isa} is the calling deck's level, for methods
     * whose range grows with it. Throw {@link UnsupportedOperationException} for an unknown method and
     * {@link IllegalArgumentException} (with a message for the player) for bad arguments.
     */
    default Object invoke(String method, List<Object> args, IsaLevel isa) {
        throw new UnsupportedOperationException(method);
    }

    /** A read-only handle over a map, used for {@code event.<field>}. */
    static Handle ofMap(Map<String, Object> values) {
        return values::get;
    }
}
