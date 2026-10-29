package dev.cyberimplants.implant;

import java.util.Map;

/**
 * Who is using an implant and how.
 *
 * @param scripted  true when a Synapse program issued the activation (input-bound abilities refuse it)
 * @param emergency true when an Emergency Response System or a triggered rule fires it: may draw on the Energy Buffer
 * @param target    optional AT target from a script (entity handle value or null)
 * @param args      optional WITH parameters from a script
 */
public record UseContext(boolean scripted, boolean emergency, Object target, Map<String, Object> args) {
    public static final UseContext MANUAL = new UseContext(false, false, null, Map.of());
    public static final UseContext EMERGENCY = new UseContext(false, true, null, Map.of());

    public static UseContext scripted(Object target, Map<String, Object> args) {
        return new UseContext(true, false, target, args);
    }
}
