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

    /** ISA-5: start the Processor overclock (Tier V Processor only). */
    default boolean overclock() {
        return false;
    }

    /** ISA-5: a Create 6 package request: {@code count} of {@code item} from a stock network to a package address. */
    default boolean request(String item, int count, String network, String address) {
        return false;
    }

    /** ISA-4 logistics read: how many of {@code item} a stock network holds, or null when it can't be read. */
    default Double stock(String network, String item) {
        return null;
    }

    /** ISA-5: the saved values of this program's {@code PERSIST VAR}s, by name. */
    default Map<String, Object> restorePersisted() {
        return Map.of();
    }

    /** ISA-5: saves the current values of this program's {@code PERSIST VAR}s (numbers, booleans, strings, NULL). */
    default void persist(Map<String, Object> values) {
    }

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
