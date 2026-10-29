package dev.cyberimplants.synapse.vm;

import java.util.Map;

/** An object scripts can read members from: an implant, an entity, the event payload. */
public interface Handle {
    /** The member's value, or null if absent. Values are Double, Boolean, String, Ticks, Position, Handle or null. */
    Object get(String member);

    /** Writes a member. Returns false if it is unknown or read-only. */
    default boolean set(String member, Object value) {
        return false;
    }

    /** Cycles charged for a read; hosts may raise this for expensive sources. */
    default int readCost() {
        return 2;
    }

    /** A read-only handle over a map, used for {@code event.<field>}. */
    static Handle ofMap(Map<String, Object> values) {
        return values::get;
    }
}
