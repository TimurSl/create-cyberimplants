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
 * (docs/design/03-cyberdeck.md, section 3.5). One runner is one thread; a deck with Coprocessors runs several. Tasks
 * that {@code WAIT} sleep without blocking the queue; {@code EVERY} timers and {@code DEBOUNCE} are kept here.
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

    private record Sleeper(Task task, CompiledHandler handler, long wakeAt) {
    }

    private final CompiledProgram program;
    private final Host host;
    private final Map<String, Object> globals = new HashMap<>();
    private final Deque<Pending> queue = new ArrayDeque<>();
    private final Deque<Long> abortTicks = new ArrayDeque<>();
    private final Deque<Sleeper> woken = new ArrayDeque<>();

    private final int ramCells;
    private final List<Sleeper> sleeping = new ArrayList<>();
    private final Map<CompiledHandler, Long> lastFired = new HashMap<>();
    private final Map<CompiledHandler, Long> nextTimer = new HashMap<>();

    private State state = State.STOPPED;
    private Task current;
    private CompiledHandler currentHandler;
    private long tickCounter;
    private long totalCycles;
    private int dropped;
    private String lastError = "";

    public ProgramRunner(CompiledProgram program, Host host) {
        this(program, host, Integer.MAX_VALUE);
    }

    /** {@code ramCells} is the RAM reservation: a task that pushes the program above it dies with OUT_OF_MEMORY. */
    public ProgramRunner(CompiledProgram program, Host host, int ramCells) {
        this.program = program;
        this.host = host;
        this.ramCells = ramCells;
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
        return queue.size() + sleeping.size() + (current == null ? 0 : 1);
    }

    /** Tasks that are sleeping in a {@code WAIT}. */
    public int sleeping() {
        return sleeping.size();
    }

    /** Live memory use in cells: code, subscriptions, variables and the frames of running tasks. */
    public int usedCells() {
        int cells = program.footprintCells() - program.globals().size();
        for (Object value : globals.values()) {
            cells += Memory.cells(value);
        }
        if (current != null) {
            cells += current.frameCells();
        }
        for (Sleeper s : sleeping) {
            cells += s.task().frameCells();
        }
        return cells;
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
        sleeping.clear();
        lastFired.clear();
        nextTimer.clear();
        current = null;
        currentHandler = null;
        abortTicks.clear();
        state = State.RUNNING;
        for (CompiledHandler timer : program.timers()) {
            nextTimer.put(timer, tickCounter + Math.max(1, timer.argument().longValue()));
        }
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
        sleeping.clear();
        current = null;
        currentHandler = null;
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
            if (h.event().equals("@every")) {
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
            if (h.debounceTicks() > 0) {
                Long last = lastFired.get(h);
                if (last != null && tickCounter - last < h.debounceTicks()) {
                    continue;
                }
            }
            if (h.when() != null && !passesFilter(h, payload)) {
                continue;
            }
            lastFired.put(h, tickCounter);
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
        if (queued() >= MAX_QUEUE) {
            dropped++;
            return;
        }
        queue.add(new Pending(h, payload));
    }

    private boolean running(CompiledHandler h) {
        if (currentHandler == h) {
            return true;
        }
        for (Sleeper s : sleeping) {
            if (s.handler() == h) {
                return true;
            }
        }
        for (Pending p : queue) {
            if (p.handler() == h) {
                return true;
            }
        }
        return false;
    }

    private void fireTimers() {
        for (CompiledHandler timer : program.timers()) {
            long due = nextTimer.getOrDefault(timer, Long.MAX_VALUE);
            if (tickCounter < due) {
                continue;
            }
            nextTimer.put(timer, tickCounter + Math.max(1, timer.argument().longValue()));
            if (!running(timer) && queued() < MAX_QUEUE) {
                queue.add(new Pending(timer, Map.of()));
            }
        }
    }

    /** Wakes sleepers whose time has come and puts them at the front of the line. */
    private void wake() {
        for (int i = sleeping.size() - 1; i >= 0; i--) {
            Sleeper s = sleeping.get(i);
            if (s.wakeAt() <= tickCounter) {
                sleeping.remove(i);
                woken.addFirst(s);
            }
        }
    }

    /** Advances one tick with the given cycle budget. @return cycles actually used */
    public int tick(int budget) {
        tickCounter++;
        if (state != State.RUNNING) {
            return 0;
        }
        fireTimers();
        wake();
        int used = 0;
        while (used < budget && state == State.RUNNING) {
            if (current == null) {
                Sleeper resumed = woken.pollFirst();
                if (resumed != null) {
                    current = resumed.task();
                    currentHandler = resumed.handler();
                } else {
                    Pending next = queue.poll();
                    if (next == null) {
                        break;
                    }
                    current = new Task(next.handler().body(), globals, next.payload(), host, program.isa());
                    currentHandler = next.handler();
                }
            }
            long before = current.cyclesUsed();
            try {
                Task.Status status = current.run(budget - used);
                used += (int) (current.cyclesUsed() - before);
                if (usedCells() > ramCells) {
                    throw new VmException(VmError.OUT_OF_MEMORY,
                            "The program needs " + usedCells() + " cells but has " + ramCells, 0);
                }
                switch (status) {
                    case DONE -> clearCurrent();
                    case STOPPED -> {
                        stop();
                    }
                    case WAITING -> {
                        sleeping.add(new Sleeper(current, currentHandler, tickCounter + (long) current.waitTicks()));
                        current = null;
                        currentHandler = null;
                    }
                    case PAUSED -> {
                        if (current.sinceYield() > (long) WATCHDOG_FACTOR * budget) {
                            throw new VmException(VmError.WATCHDOG, "Task ran too long without yielding", 0);
                        }
                        totalCycles += used;
                        return used; // paused: resume next tick
                    }
                }
            } catch (VmException e) {
                used += (int) (current.cyclesUsed() - before);
                clearCurrent();
                abort(e);
            }
        }
        totalCycles += used;
        return used;
    }

    private void clearCurrent() {
        current = null;
        currentHandler = null;
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
            sleeping.clear();
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
