package dev.cyberimplants.synapse.compiler;

import dev.cyberimplants.synapse.IsaLevel;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** The result of compiling a Synapse program. Immutable. */
public record CompiledProgram(
        String name,
        IsaLevel isa,
        List<String> requires,
        int priority,
        boolean autostart,
        Code init,
        List<String> globals,
        List<CompiledHandler> handlers) {

    public CompiledProgram {
        requires = List.copyOf(requires);
        globals = List.copyOf(globals);
        handlers = List.copyOf(handlers);
    }

    /**
     * A handler. {@code argument} is the numeric threshold / key number or null; {@code from} the device a FROM clause
     * names (or null); {@code when} is the optional filter, evaluated before a task is queued. Timer handlers
     * ({@code EVERY}) have the event {@code "@every"} and the interval in ticks as their argument.
     */
    public record CompiledHandler(String event, Double argument, String from, Code when, double debounceTicks, Code body) {
    }

    /** Distinct numeric arguments used with the given event name, e.g. the thresholds of {@code health_below}. */
    public Set<Double> arguments(String event) {
        Set<Double> out = new TreeSet<>();
        for (CompiledHandler h : handlers) {
            if (h.event().equals(event) && h.argument() != null) {
                out.add(h.argument());
            }
        }
        return out;
    }

    /** The timer handlers' intervals in ticks. */
    public List<CompiledHandler> timers() {
        return handlers.stream().filter(h -> h.event().equals("@every")).toList();
    }

    public boolean subscribes(String event) {
        return handlers.stream().anyMatch(h -> h.event().equals(event));
    }

    /**
     * Static memory estimate in cells (docs/design/04-synapse-language.md, section 8.2): code at 1 cell per 4
     * instructions, 1 per global, 2 per subscription and the largest task frame.
     */
    public int footprintCells() {
        int instrs = init.instrs().size();
        int frame = 0;
        for (CompiledHandler h : handlers) {
            instrs += h.body().instrs().size() + (h.when() == null ? 0 : h.when().instrs().size());
            frame = Math.max(frame, 2 + h.body().localCount());
        }
        return (instrs + 3) / 4 + globals.size() + handlers.size() * 2 + frame;
    }
}
