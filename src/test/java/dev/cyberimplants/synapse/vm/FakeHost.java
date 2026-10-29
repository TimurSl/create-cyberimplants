package dev.cyberimplants.synapse.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A scriptable host for VM tests. */
final class FakeHost implements Host {
    record Activation(String implant, Object at, Map<String, Object> with) {
    }

    final Map<String, Object> globals = new HashMap<>();
    final List<Activation> activations = new ArrayList<>();
    final List<String> logs = new ArrayList<>();
    final List<String> alerts = new ArrayList<>();
    final List<String> actions = new ArrayList<>();
    final List<VmError> aborts = new ArrayList<>();
    long now;

    /** A handle with a fixed member map that accepts writes. */
    static Handle object(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return new Handle() {
            @Override
            public Object get(String member) {
                return m.get(member);
            }

            @Override
            public boolean set(String member, Object value) {
                if (!m.containsKey(member)) {
                    return false;
                }
                m.put(member, value);
                return true;
            }
        };
    }

    final Map<String, Object> devices = new HashMap<>();

    @Override
    public Object device(String kind, String label) {
        return devices.get(kind + ":" + label);
    }

    @Override
    public Object global(String name) {
        return globals.get(name);
    }

    @Override
    public boolean activate(String implant, Object at, Map<String, Object> with) {
        activations.add(new Activation(implant, at, with));
        return true;
    }

    @Override
    public boolean deactivate(String implant) {
        actions.add("deactivate " + implant);
        return true;
    }

    @Override
    public boolean toggle(String implant) {
        actions.add("toggle " + implant);
        return true;
    }

    @Override
    public boolean scan() {
        actions.add("scan");
        return true;
    }

    @Override
    public boolean tag(Object entity) {
        actions.add("tag");
        return true;
    }

    @Override
    public boolean untag(Object entity) {
        actions.add("untag");
        return true;
    }

    @Override
    public void log(String line) {
        logs.add(line);
    }

    @Override
    public void alert(String message) {
        alerts.add(message);
    }

    @Override
    public long now() {
        return now;
    }

    @Override
    public double random() {
        return 0.25;
    }

    @Override
    public void taskAborted(VmError error, String detail, int line) {
        aborts.add(error);
    }
}
