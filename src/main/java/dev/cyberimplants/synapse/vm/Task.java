package dev.cyberimplants.synapse.vm;

import dev.cyberimplants.synapse.IsaLevel;
import dev.cyberimplants.synapse.compiler.ActivateInfo;
import dev.cyberimplants.synapse.compiler.Code;
import dev.cyberimplants.synapse.compiler.Compiler;
import dev.cyberimplants.synapse.compiler.ConfigureInfo;
import dev.cyberimplants.synapse.compiler.DeviceInfo;
import dev.cyberimplants.synapse.compiler.Instr;
import dev.cyberimplants.synapse.compiler.Op;
import dev.cyberimplants.synapse.compiler.Ticks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One running handler invocation: a resumable stack machine. {@link #run(int)} executes until the cycle budget is
 * spent (then the task is paused and can be resumed next tick), the code ends, the program stops or an error aborts it.
 */
public final class Task {
    public enum Status { DONE, PAUSED, STOPPED, WAITING }

    /** Strings longer than this abort the task: a single value must not outgrow any RAM reservation. */
    public static final int MAX_STRING = 4096;

    private final Code code;
    private final Map<String, Object> globals;
    private final Map<String, Object> payload;
    private final Host host;
    private final IsaLevel isa;

    private final List<Object> stack = new ArrayList<>();
    private final Object[] locals;
    private int pc;
    private long cyclesUsed;
    private long sinceYield;
    private double waitTicks;

    public Task(Code code, Map<String, Object> globals, Map<String, Object> payload, Host host, IsaLevel isa) {
        this.code = code;
        this.globals = globals;
        this.payload = payload;
        this.host = host;
        this.isa = isa;
        this.locals = new Object[code.localCount()];
    }

    public long cyclesUsed() {
        return cyclesUsed;
    }

    /** Ticks the task asked to sleep when {@link #run} returned {@link Status#WAITING}. */
    public double waitTicks() {
        return waitTicks;
    }

    /** Cells of this task's frame: 2 plus its locals (docs/design/04-synapse-language.md, section 8.2). */
    public int frameCells() {
        int cells = 2;
        for (Object local : locals) {
            cells += Memory.cells(local);
        }
        return cells + stack.size();
    }

    /** Cycles spent since the task last yielded (only {@code WAIT} yields), for the watchdog. */
    public long sinceYield() {
        return sinceYield;
    }

    /** The value left on the stack by a finished expression unit (a WHEN filter), or null. */
    public Object result() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    /**
     * Runs for at most {@code budget} cycles. An instruction that would exceed the budget is deferred to the next
     * call, except as the first instruction of a call, which always runs so that expensive instructions can't starve.
     */
    public Status run(int budget) {
        long spent = 0;
        List<Instr> instrs = code.instrs();
        while (true) {
            Instr in = instrs.get(pc);
            if (in.op() == Op.END) {
                return Status.DONE;
            }
            int cost = in.op().cycles();
            if (spent > 0 && spent + cost > budget) {
                return Status.PAUSED;
            }
            spent += cost;
            sinceYield += cost;
            cyclesUsed += cost;
            pc++;
            if (in.op() == Op.WAIT) {
                Object d = pop();
                Double ticks = numeric(d);
                if (ticks == null) {
                    throw error(VmError.TYPE, "WAIT needs a duration but found " + typeName(d), in);
                }
                waitTicks = Math.max(1, Math.ceil(ticks));
                sinceYield = 0;
                return Status.WAITING;
            }
            if (execute(in)) {
                return Status.STOPPED;
            }
        }
    }

    /** @return true if the program asked to stop */
    private boolean execute(Instr in) {
        switch (in.op()) {
            case PUSH -> push(in.k());
            case POP -> pop();
            case LOAD_LOCAL -> push(locals[in.a()]);
            case STORE_LOCAL -> locals[in.a()] = pop();
            case LOAD_NAME -> push(resolve((String) in.k()));
            case STORE_GLOBAL -> globals.put((String) in.k(), pop());
            case DEVICE -> {
                DeviceInfo info = (DeviceInfo) in.k();
                push(host.device(info.kind(), info.label()));
            }
            case MEMBER -> {
                Object target = pop();
                if (target == null) {
                    throw error(VmError.NULL_ACCESS, "Cannot read '" + in.k() + "' of NULL", in);
                }
                if (!(target instanceof Handle h)) {
                    throw error(VmError.TYPE, "Cannot read '" + in.k() + "' of " + typeName(target), in);
                }
                charge(h.readCost() - in.op().cycles());
                push(h.get((String) in.k()));
            }
            case SET_MEMBER -> {
                Object value = pop();
                Object target = pop();
                if (target == null) {
                    throw error(VmError.NULL_ACCESS, "Cannot set '" + in.k() + "' of NULL", in);
                }
                if (!(target instanceof Handle h) || !h.set((String) in.k(), value)) {
                    throw error(VmError.TYPE, "'" + in.k() + "' cannot be set", in);
                }
                charge(h.writeCost() - in.op().cycles());
            }
            case READ -> {
                Object target = pop();
                if (target == null) {
                    throw error(VmError.NULL_ACCESS, "Cannot read '" + in.k() + "' of NULL", in);
                }
                if (!(target instanceof Handle h)) {
                    throw error(VmError.TYPE, "Cannot read '" + in.k() + "' of " + typeName(target), in);
                }
                push(h.get((String) in.k()));
            }
            case CONFIGURE -> {
                ConfigureInfo info = (ConfigureInfo) in.k();
                host.configure(info.implant(), info.key(), pop());
            }
            case MANIPULATE -> {
                Object argument = in.a() == 1 ? pop() : null;
                host.manipulate(pop(), (String) in.k(), argument);
            }
            case NEG -> {
                Object v = pop();
                if (v instanceof Double d) {
                    push(-d);
                } else if (v instanceof Ticks t) {
                    push(new Ticks(-t.ticks()));
                } else {
                    throw error(VmError.TYPE, "Cannot negate " + typeName(v), in);
                }
            }
            case NOT -> push(!truthy(pop(), in));
            case ADD, SUB, MUL, DIV, MOD -> arithmetic(in);
            case EQ -> {
                Object b = pop();
                Object a = pop();
                push(valuesEqual(a, b));
            }
            case NE -> {
                Object b = pop();
                Object a = pop();
                push(!valuesEqual(a, b));
            }
            case LT, LE, GT, GE -> compare(in);
            case JUMP -> pc = in.a();
            case JUMP_IF_FALSE -> {
                if (!truthy(pop(), in)) {
                    pc = in.a();
                }
            }
            case AND_JUMP -> {
                if (!truthy(stack.get(stack.size() - 1), in)) {
                    pc = in.a();
                } else {
                    pop();
                }
            }
            case OR_JUMP -> {
                if (truthy(stack.get(stack.size() - 1), in)) {
                    pc = in.a();
                } else {
                    pop();
                }
            }
            case CALL -> push(call((String) in.k(), in.a(), in));
            case ACTIVATE -> {
                ActivateInfo info = (ActivateInfo) in.k();
                Object[] values = new Object[info.keys().size()];
                for (int i = values.length - 1; i >= 0; i--) {
                    values[i] = pop();
                }
                Map<String, Object> ordered = new LinkedHashMap<>();
                for (int i = 0; i < values.length; i++) {
                    ordered.put(info.keys().get(i), values[i]);
                }
                Object at = info.hasTarget() ? pop() : null;
                host.activate(info.implant(), at, ordered);
            }
            case DEACTIVATE -> host.deactivate((String) in.k());
            case TOGGLE -> host.toggle((String) in.k());
            case SCAN -> host.scan();
            case TAG -> host.tag(pop());
            case UNTAG -> host.untag(pop());
            case LOG -> host.log(stringify(pop()));
            case ALERT -> host.alert(stringify(pop()));
            case STOP -> {
                return true;
            }
            case END -> {
            }
        }
        return false;
    }

    /** Adds (or refunds) cycles beyond an instruction's base cost. */
    private void charge(int extra) {
        cyclesUsed += extra;
        sinceYield += extra;
    }

    // ---------------------------------------------------------------- values

    private Object resolve(String name) {
        if (payload.containsKey(name)) {
            return payload.get(name);
        }
        if (globals.containsKey(name)) {
            return globals.get(name);
        }
        if (name.equals("event")) {
            return Handle.ofMap(payload);
        }
        return host.global(name);
    }

    private void arithmetic(Instr in) {
        Object b = pop();
        Object a = pop();
        Op op = in.op();
        if (a instanceof Double x && b instanceof Double y) {
            switch (op) {
                case ADD -> push(x + y);
                case SUB -> push(x - y);
                case MUL -> push(x * y);
                case DIV -> {
                    if (y == 0) {
                        throw error(VmError.TYPE, "Division by zero", in);
                    }
                    push(x / y);
                }
                default -> {
                    if (y == 0) {
                        throw error(VmError.TYPE, "Modulo by zero", in);
                    }
                    push(x % y);
                }
            }
            return;
        }
        if (a instanceof Ticks x && b instanceof Ticks y && (op == Op.ADD || op == Op.SUB)) {
            push(new Ticks(op == Op.ADD ? x.ticks() + y.ticks() : x.ticks() - y.ticks()));
            return;
        }
        if (a instanceof Ticks x && b instanceof Double y && (op == Op.MUL || op == Op.DIV)) {
            if (op == Op.DIV && y == 0) {
                throw error(VmError.TYPE, "Division by zero", in);
            }
            push(new Ticks(op == Op.MUL ? x.ticks() * y : x.ticks() / y));
            return;
        }
        if (a instanceof Double x && b instanceof Ticks y && op == Op.MUL) {
            push(new Ticks(x * y.ticks()));
            return;
        }
        if (op == Op.ADD && (a instanceof String || b instanceof String)) {
            if (!isa.atLeast(IsaLevel.ISA_2)) {
                throw error(VmError.TYPE, "String operations require ISA-2", in);
            }
            String joined = stringify(a) + stringify(b);
            if (joined.length() > MAX_STRING) {
                throw error(VmError.OUT_OF_MEMORY, "A string may hold at most " + MAX_STRING + " characters", in);
            }
            push(joined);
            return;
        }
        throw error(VmError.TYPE, "Cannot apply " + in.op() + " to " + typeName(a) + " and " + typeName(b), in);
    }

    private void compare(Instr in) {
        Object b = pop();
        Object a = pop();
        Double x = numeric(a);
        Double y = numeric(b);
        if (x == null || y == null) {
            throw error(VmError.TYPE, "Cannot compare " + typeName(a) + " with " + typeName(b), in);
        }
        push(switch (in.op()) {
            case LT -> x < y;
            case LE -> x <= y;
            case GT -> x > y;
            default -> x >= y;
        });
    }

    /** Numbers and durations compare by value (durations in ticks). */
    private static Double numeric(Object v) {
        if (v instanceof Double d) {
            return d;
        }
        if (v instanceof Ticks t) {
            return t.ticks();
        }
        return null;
    }

    private static boolean valuesEqual(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        Double x = numeric(a);
        Double y = numeric(b);
        if (x != null && y != null) {
            return x.doubleValue() == y.doubleValue();
        }
        return a.equals(b);
    }

    private boolean truthy(Object v, Instr in) {
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        throw error(VmError.TYPE, "Expected a TRUE/FALSE value but found " + typeName(v), in);
    }

    private Object call(String fn, int argc, Instr in) {
        Object[] args = new Object[argc];
        for (int i = argc - 1; i >= 0; i--) {
            args[i] = pop();
        }
        return switch (fn) {
            case "ABS" -> Math.abs(num(args[0], in));
            case "MIN" -> Math.min(num(args[0], in), num(args[1], in));
            case "MAX" -> Math.max(num(args[0], in), num(args[1], in));
            case "CLAMP" -> Math.max(num(args[1], in), Math.min(num(args[2], in), num(args[0], in)));
            case "ROUND" -> (double) Math.round(num(args[0], in));
            case "FLOOR" -> Math.floor(num(args[0], in));
            case "SQRT" -> {
                double v = num(args[0], in);
                if (v < 0) {
                    throw error(VmError.TYPE, "SQRT of a negative number", in);
                }
                yield Math.sqrt(v);
            }
            case "LEN" -> {
                if (args[0] instanceof String s) {
                    yield (double) s.length();
                }
                throw error(VmError.TYPE, "LEN needs a string", in);
            }
            case "UPPER" -> text(args[0], in).toUpperCase(java.util.Locale.ROOT);
            case "LOWER" -> text(args[0], in).toLowerCase(java.util.Locale.ROOT);
            case "CONTAINS" -> text(args[0], in).contains(text(args[1], in));
            case "SUBSTR" -> {
                String s = text(args[0], in);
                int from = (int) Math.max(0, Math.min(s.length(), num(args[1], in)));
                int to = (int) Math.max(from, Math.min(s.length(), from + num(args[2], in)));
                yield s.substring(from, to);
            }
            case "STR" -> stringify(args[0]);
            case "NUM" -> {
                if (args[0] instanceof Double) {
                    yield args[0];
                }
                if (args[0] instanceof Ticks t) {
                    yield t.ticks();
                }
                if (args[0] instanceof String s) {
                    try {
                        yield Double.parseDouble(s.trim());
                    } catch (NumberFormatException e) {
                        yield null;
                    }
                }
                yield null;
            }
            case "DIST" -> {
                Position p = position(args[0], in);
                Position q = position(args[1], in);
                yield p.distanceTo(q);
            }
            case "POS" -> new Position(num(args[0], in), num(args[1], in), num(args[2], in));
            case "NOW" -> (double) host.now();
            case "RANDOM" -> host.random();
            default -> throw error(VmError.TYPE, "Unknown function " + fn, in);
        };
    }

    private Position position(Object v, Instr in) {
        if (v instanceof Position p) {
            return p;
        }
        if (v instanceof Handle h && h.get("position") instanceof Position p) {
            return p;
        }
        throw error(VmError.TYPE, "Expected a position or entity", in);
    }

    private String text(Object v, Instr in) {
        if (v instanceof String s) {
            return s;
        }
        throw error(VmError.TYPE, "Expected a string but found " + typeName(v), in);
    }

    private double num(Object v, Instr in) {
        Double d = numeric(v);
        if (d == null) {
            throw error(VmError.TYPE, "Expected a number but found " + typeName(v), in);
        }
        return d;
    }

    /** Converts a value to text for {@code LOG}, {@code ALERT} and {@code STR}. */
    public static String stringify(Object v) {
        if (v == null) {
            return "NULL";
        }
        if (v instanceof Double d) {
            if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                return Long.toString((long) d.doubleValue());
            }
            return Double.toString(d);
        }
        if (v instanceof Boolean b) {
            return b ? "TRUE" : "FALSE";
        }
        if (v instanceof Ticks t) {
            return stringify(t.ticks()) + "t";
        }
        if (v instanceof Position p) {
            return "(" + stringify(p.x()) + ", " + stringify(p.y()) + ", " + stringify(p.z()) + ")";
        }
        if (v instanceof Handle h) {
            Object type = h.get("type");
            return type == null ? "<object>" : "<" + type + ">";
        }
        return v.toString();
    }

    private static String typeName(Object v) {
        if (v == null) {
            return "NULL";
        }
        if (v instanceof Double) {
            return "number";
        }
        if (v instanceof Boolean) {
            return "bool";
        }
        if (v instanceof String) {
            return "string";
        }
        if (v instanceof Ticks) {
            return "duration";
        }
        if (v instanceof Position) {
            return "position";
        }
        return "object";
    }

    private void push(Object v) {
        stack.add(v);
    }

    private Object pop() {
        return stack.remove(stack.size() - 1);
    }

    private VmException error(VmError code, String message, Instr in) {
        return new VmException(code, message, in.line());
    }

    /** Referenced to keep {@link Compiler#BUILTINS} and the runtime builtin switch in step (see tests). */
    static boolean isBuiltin(String name) {
        return Compiler.BUILTINS.containsKey(name);
    }
}
