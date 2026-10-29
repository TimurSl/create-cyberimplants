package dev.cyberimplants.synapse.vm;

import java.util.Map;

/**
 * The game side of a running program. The VM never touches Minecraft; everything it can sense or do goes through
 * this interface. Actions that can fail return false and record the reason in {@code last_error}.
 */
public interface Host {
    /** Resolves a global name (an implant such as {@code sandevistan}, {@code self}, {@code deck}, {@code last_error}). */
    Object global(String name);

    /** The device handle for a {@code DEVICE} declaration. It resolves its binding lazily, so it may be offline. */
    default Object device(String kind, String label) {
        return null;
    }

    boolean activate(String implant, Object at, Map<String, Object> with);

    boolean deactivate(String implant);

    boolean toggle(String implant);

    /** ISA-2: persistent implant setting (e.g. {@code CONFIGURE nano formula = "blast"}). */
    default boolean configure(String implant, String key, Object value) {
        return false;
    }

    /** ISA-2: a Remote Manipulator action on a bound port or position. {@code argument} is null except SET_VALUE. */
    default boolean manipulate(Object target, String action, Object argument) {
        return false;
    }

    boolean scan();

    boolean tag(Object entity);

    boolean untag(Object entity);

    void log(String line);

    void alert(String message);

    /** Ticks since the deck booted. */
    long now();

    /** A value in [0, 1), seeded per deck. */
    double random();

    /** Called when a task dies with a runtime error; the runner has already logged it. */
    default void taskAborted(VmError error, String detail, int line) {
    }
}
