package dev.cyberimplants.synapse.compiler;

import java.util.List;

/**
 * Operand of {@link Op#AWAIT}: the events (name and optional numeric argument, null for none) and whether a timeout
 * is on the stack.
 */
public record AwaitInfo(List<Event> events, boolean hasTimeout) {
    public record Event(String name, Double argument) {
    }

    public AwaitInfo {
        events = List.copyOf(events);
    }

    /** Whether a fired event (with its argument, or null) is one this await is waiting for. */
    public boolean matches(String event, Double argument) {
        for (Event e : events) {
            if (e.name().equals(event) && (e.argument() == null ? argument == null
                    : argument != null && e.argument().doubleValue() == argument.doubleValue())) {
                return true;
            }
        }
        return false;
    }
}
