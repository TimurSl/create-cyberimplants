package dev.cyberimplants.synapse.vm;

import dev.cyberimplants.synapse.compiler.CompiledProgram;
import dev.cyberimplants.synapse.compiler.CompiledProgram.CompiledHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs one loaded program: queues handler tasks as events arrive and executes them within a per-tick cycle budget
 * (docs/design/03-cyberdeck.md, section 3.5). Layer 1 has a single program and a single thread.
 */
public final class ProgramRunner {
    public enum State { RUNNING, STOPPED, CRASHED }

    /** At most this many tasks wait per program; further events of a queued handler are coalesced. */
    public static final int MAX_QUEUE = 16;
    /** Cycles a WHEN filter may spend. */
    public static final int WHEN_BUDGET = 64;
    /** A task may use at most this many times the tick budget between yields. */
    public static final int WATCHDOG_FACTOR = 20;
    public static final int CRASH_ABORTS = 5;
    public static final int CRASH_WINDOW_TICKS = 200;

    private record Pending(CompiledHandler handler, Map<String, Object> payload) {
    }

    private final CompiledProgram program;
    private final Host host;
    private final Map<String, Object> globals = new HashMap<>();
    private final Deque<Pending> queue = new ArrayDeque<>();
    private final Deque<Long> abortTicks = new ArrayDeque<>();

    private State state = State.STOPPED;
    private Task current;
    private long tickCounter;
    private long totalCycles;
    private int dropped;
    private String lastError = "";

    public ProgramRunner(CompiledProgram program, Host host) {
        this.program = program;
        this.host = host;
    }

    public CompiledProgram program() {
        return program;
    }

    public State state() {
        return state;
    }

    public int droppedEvents() {
        return dropped;
    }

    public int queued() {
        return queue.size() + (current == null ? 0 : 1);
    }

    public long totalCycles() {
        return totalCycles;
    }

    public String lastError() {
        return lastError;
    }

    /** Initialises variables and fires {@code boot}. */
    public void start() {
        globals.clear();
        queue.clear();
        current = null;
        abortTicks.clear();
        state = State.RUNNING;
        try {
            Task init = new Task(program.init(), globals, Map.of(), host, program.isa());
            init.run(Integer.MAX_VALUE);
        } catch (VmException e) {
            fail(e);
            state = State.CRASHED;
            return;
        }
        fire("boot", null, Map.of());
    }

    public void stop() {
        state = State.STOPPED;
        queue.clear();
        current = null;
    }

    /** Numeric arguments handlers use with {@code event} (thresholds, key numbers). */
    public Set<Double> arguments(String event) {
        return program.arguments(event);
    }

    public boolean subscribes(String event) {
        return program.subscribes(event);
    }

    /**
     * Queues every handler that matches. {@code argument} selects handlers declared with that number
     * ({@code ON health_below 30}); pass null for events without one.
     */
    public void fire(String event, Double argument, Map<String, Object> payload) {
        if (state != State.RUNNING) {
            return;
        }
        for (CompiledHandler h : program.handlers()) {
            if (!h.event().equals(event)) {
                continue;
            }
            if (h.argument() != null && (argument == null || h.argument().doubleValue() != argument.doubleValue())) {
                continue;
            }
            if (h.argument() == null && argument != null) {
                continue;
            }
            if (h.from() != null && !java.util.Objects.equals(payload.get("source"), globals.get(h.from()))) {
                continue;
            }
            if (h.when() != null && !passesFilter(h, payload)) {
                continue;
            }
            enqueue(h, payload);
        }
    }

    private boolean passesFilter(CompiledHandler h, Map<String, Object> payload) {
        Task filter = new Task(h.when(), globals, payload, host, program.isa());
        try {
            Task.Status status = filter.run(WHEN_BUDGET);
            if (status != Task.Status.DONE) {
                return false;
            }
            Object result = filter.result();
            return result instanceof Boolean b && b;
        } catch (VmException e) {
            fail(e);
            return false;
        }
    }

    private void enqueue(CompiledHandler h, Map<String, Object> payload) {
        for (Pending p : queue) {
            if (p.handler() == h) {
                // Coalesce: keep only the latest payload.
                queue.remove(p);
                queue.add(new Pending(h, payload));
                return;
            }
        }
        if (queue.size() >= MAX_QUEUE) {
            dropped++;
            return;
        }
        queue.add(new Pending(h, payload));
    }

    /** Advances one tick with the given cycle budget. @return cycles actually used */
    public int tick(int budget) {
        tickCounter++;
        if (state != State.RUNNING) {
            return 0;
        }
        int used = 0;
        while (used < budget && state == State.RUNNING) {
            if (current == null) {
                Pending next = queue.poll();
                if (next == null) {
                    break;
                }
                current = new Task(next.handler().body(), globals, next.payload(), host, program.isa());
            }
            long before = current.cyclesUsed();
            try {
                Task.Status status = current.run(budget - used);
                used += (int) (current.cyclesUsed() - before);
                if (status == Task.Status.DONE) {
                    current = null;
                } else if (status == Task.Status.STOPPED) {
                    stop();
                } else {
                    if (current.sinceYield() > (long) WATCHDOG_FACTOR * budget) {
                        throw new VmException(VmError.WATCHDOG, "Task ran too long without yielding", 0);
                    }
                    break; // paused: resume next tick
                }
            } catch (VmException e) {
                used += (int) (current.cyclesUsed() - before);
                current = null;
                abort(e);
            }
        }
        totalCycles += used;
        return used;
    }

    private void abort(VmException e) {
        fail(e);
        host.taskAborted(e.error(), e.getMessage(), e.line());
        abortTicks.add(tickCounter);
        while (!abortTicks.isEmpty() && tickCounter - abortTicks.peekFirst() > CRASH_WINDOW_TICKS) {
            abortTicks.pollFirst();
        }
        if (abortTicks.size() >= CRASH_ABORTS) {
            state = State.CRASHED;
            queue.clear();
            host.log("[crash] " + program.name() + " aborted " + CRASH_ABORTS + " tasks in 10 s and was unloaded");
        }
    }

    private void fail(VmException e) {
        lastError = e.error() + (e.line() > 0 ? " (line " + e.line() + ")" : "") + ": " + e.getMessage();
        host.log("[error] " + lastError);
    }

    /** A copy of the program's variables, for the deck UI and tests. */
    public Map<String, Object> variables() {
        return java.util.Collections.unmodifiableMap(new HashMap<>(globals));
    }

    /** Events a handler list subscribes to, in declaration order and without duplicates. */
    public List<String> subscribedEvents() {
        List<String> out = new ArrayList<>();
        for (CompiledHandler h : program.handlers()) {
            if (!out.contains(h.event())) {
                out.add(h.event());
            }
        }
        return out;
    }
}
