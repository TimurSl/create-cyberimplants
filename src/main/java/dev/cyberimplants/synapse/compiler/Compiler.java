package dev.cyberimplants.synapse.compiler;

import dev.cyberimplants.synapse.IsaLevel;
import dev.cyberimplants.synapse.SynapseException;
import dev.cyberimplants.synapse.ast.Ast;
import dev.cyberimplants.synapse.ast.Ast.Expr;
import dev.cyberimplants.synapse.ast.Ast.Stmt;
import dev.cyberimplants.synapse.parser.Parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Compiles a Synapse syntax tree to bytecode and performs the semantic checks (names, ISA level). */
public final class Compiler {
    /** Builtin functions and their argument counts. */
    public static final Map<String, Integer> BUILTINS = Map.ofEntries(
            Map.entry("ABS", 1), Map.entry("MIN", 2), Map.entry("MAX", 2), Map.entry("CLAMP", 3),
            Map.entry("ROUND", 1), Map.entry("FLOOR", 1), Map.entry("SQRT", 1), Map.entry("LEN", 1),
            Map.entry("STR", 1), Map.entry("NUM", 1), Map.entry("DIST", 2), Map.entry("POS", 3),
            Map.entry("NOW", 0), Map.entry("RANDOM", 0),
            Map.entry("UPPER", 1), Map.entry("LOWER", 1), Map.entry("CONTAINS", 2), Map.entry("SUBSTR", 3));

    /** Builtins that operate on strings: ISA-2. */
    private static final Set<String> STRING_FUNCTIONS = Set.of("LEN", "UPPER", "LOWER", "CONTAINS", "SUBSTR");

    /** Hardware limits the compiler checks: the Processor's minimum {@code EVERY} interval, in ticks. */
    public record Limits(int minEveryTicks) {
        public static final Limits NONE = new Limits(1);
    }

    private final IsaLevel isa;
    private final Set<String> knownImplants;
    private final Limits limits;
    private final Set<String> globals = new LinkedHashSet<>();
    private final Set<String> constants = new HashSet<>();

    private List<Instr> out;
    private Deque<Map<String, Integer>> scopes;
    private int nextSlot;
    private final Deque<LoopLabels> loops = new ArrayDeque<>();

    /** Jump instructions of a loop body that still need their targets. */
    private static final class LoopLabels {
        final List<Integer> breaks = new ArrayList<>();
        final List<Integer> continues = new ArrayList<>();
    }

    private Compiler(IsaLevel isa, Set<String> knownImplants, Limits limits) {
        this.isa = isa;
        this.knownImplants = knownImplants;
        this.limits = limits;
    }

    /** Parses and compiles. {@code knownImplants} (lower-case names) enables unknown-implant errors; null skips them. */
    public static CompiledProgram compile(String source, IsaLevel isa, Set<String> knownImplants) {
        return compile(source, isa, knownImplants, Limits.NONE);
    }

    public static CompiledProgram compile(String source, IsaLevel isa, Set<String> knownImplants, Limits limits) {
        Ast.ProgramNode ast = Parser.parse(source, isa);
        return new Compiler(isa, knownImplants, limits).program(ast);
    }

    public static CompiledProgram compile(String source, IsaLevel isa) {
        return compile(source, isa, null);
    }

    private CompiledProgram program(Ast.ProgramNode ast) {
        // Declarations first so handlers can refer to them regardless of order.
        for (Ast.Const c : ast.consts()) {
            declareGlobal(c.name(), c.line());
            constants.add(c.name());
        }
        for (Ast.Var v : ast.vars()) {
            declareGlobal(v.name(), v.line());
        }
        for (Ast.Device d : ast.devices()) {
            declareGlobal(d.name(), d.line());
            constants.add(d.name());
        }

        begin();
        for (Ast.Const c : ast.consts()) {
            expr(c.value());
            emit(new Instr(Op.STORE_GLOBAL, 0, c.name(), c.line()));
        }
        for (Ast.Var v : ast.vars()) {
            expr(v.value());
            emit(new Instr(Op.STORE_GLOBAL, 0, v.name(), v.line()));
        }
        for (Ast.Device d : ast.devices()) {
            emit(new Instr(Op.DEVICE, 0, new DeviceInfo(d.kind(), d.label()), d.line()));
            emit(new Instr(Op.STORE_GLOBAL, 0, d.name(), d.line()));
        }
        Code init = finish();

        List<CompiledProgram.CompiledHandler> handlers = new ArrayList<>();
        for (Ast.Handler h : ast.handlers()) {
            handlers.add(handler(h));
        }
        return new CompiledProgram(ast.name(), isa, ast.requires(), ast.priority(), ast.autostart(), init,
                List.copyOf(globals), handlers);
    }

    private CompiledProgram.CompiledHandler handler(Ast.Handler h) {
        if (h.from() != null && !globals.contains(h.from())) {
            throw new SynapseException("FROM refers to unknown device '" + h.from() + "'", h.line(), 1);
        }
        if (h.event().equals(Ast.Handler.EVERY) && h.argument() < limits.minEveryTicks()) {
            throw new SynapseException("EVERY interval is below this Processor's minimum of " + limits.minEveryTicks()
                    + " ticks", h.line(), 1);
        }
        Code when = null;
        if (h.when() != null) {
            begin();
            expr(h.when());
            when = finish();
        }
        begin();
        block(h.body());
        Code body = finish();
        return new CompiledProgram.CompiledHandler(h.event().toLowerCase(Locale.ROOT), h.argument(), h.from(), when,
                h.debounceTicks(), body);
    }

    // ---------------------------------------------------------------- statements

    private void block(List<Stmt> stmts) {
        scopes.push(new HashMap<>());
        for (Stmt s : stmts) {
            stmt(s);
        }
        scopes.pop();
    }

    private void stmt(Stmt s) {
        switch (s) {
            case Ast.Let let -> {
                expr(let.value());
                int slot = nextSlot++;
                scopes.peek().put(let.name(), slot);
                emit(new Instr(Op.STORE_LOCAL, slot, null, let.line()));
            }
            case Ast.Set set -> assign(set);
            case Ast.If iff -> ifStatement(iff);
            case Ast.Activate a -> {
                String implant = implant(a.implant(), a.line());
                if (a.at() != null) {
                    expr(a.at());
                }
                List<String> keys = new ArrayList<>(a.with().keySet());
                for (Expr value : a.with().values()) {
                    expr(value);
                }
                emit(new Instr(Op.ACTIVATE, 0, new ActivateInfo(implant, a.at() != null, keys), a.line()));
            }
            case Ast.Deactivate d -> emit(new Instr(Op.DEACTIVATE, 0, implant(d.implant(), d.line()), d.line()));
            case Ast.Toggle t -> emit(new Instr(Op.TOGGLE, 0, implant(t.implant(), t.line()), t.line()));
            case Ast.Log l -> {
                expr(l.value());
                emit(new Instr(Op.LOG, l.line()));
            }
            case Ast.Alert a -> {
                expr(a.value());
                emit(new Instr(Op.ALERT, a.line()));
            }
            case Ast.Scan sc -> emit(new Instr(Op.SCAN, sc.line()));
            case Ast.Tag t -> {
                expr(t.entity());
                emit(new Instr(Op.TAG, t.line()));
            }
            case Ast.Untag u -> {
                expr(u.entity());
                emit(new Instr(Op.UNTAG, u.line()));
            }
            case Ast.Stop st -> emit(new Instr(Op.STOP, st.line()));
            case Ast.While w -> whileLoop(w);
            case Ast.Repeat r -> repeatLoop(r);
            case Ast.Break b -> {
                if (loops.isEmpty()) {
                    throw new SynapseException("BREAK outside a loop", b.line(), 1);
                }
                loops.peek().breaks.add(emit(new Instr(Op.JUMP, 0, null, b.line())));
            }
            case Ast.Continue c -> {
                if (loops.isEmpty()) {
                    throw new SynapseException("CONTINUE outside a loop", c.line(), 1);
                }
                loops.peek().continues.add(emit(new Instr(Op.JUMP, 0, null, c.line())));
            }
            case Ast.Wait w -> {
                expr(w.duration());
                emit(new Instr(Op.WAIT, w.line()));
            }
            case Ast.Configure c -> {
                String implant = implant(c.implant(), c.line());
                expr(c.value());
                emit(new Instr(Op.CONFIGURE, 0, new ConfigureInfo(implant, c.key()), c.line()));
            }
            case Ast.Manipulate m -> {
                expr(m.target());
                if (m.argument() != null) {
                    expr(m.argument());
                }
                emit(new Instr(Op.MANIPULATE, m.argument() != null ? 1 : 0, m.action(), m.line()));
            }
            case Ast.Read r -> read(r);
            case Ast.ExprStmt e -> {
                expr(e.value());
                emit(new Instr(Op.POP, e.line()));
            }
        }
    }

    private void whileLoop(Ast.While w) {
        int top = out.size();
        expr(w.condition());
        int exit = emit(new Instr(Op.JUMP_IF_FALSE, 0, null, w.line()));
        loops.push(new LoopLabels());
        block(w.body());
        LoopLabels labels = loops.pop();
        for (int c : labels.continues) {
            patch(c, top);
        }
        emit(new Instr(Op.JUMP, top, null, w.line()));
        patch(exit, out.size());
        for (int b : labels.breaks) {
            patch(b, out.size());
        }
    }

    /** REPEAT n: the count is evaluated once into a hidden local that counts down. */
    private void repeatLoop(Ast.Repeat r) {
        int counter = nextSlot++;
        expr(r.count());
        emit(new Instr(Op.STORE_LOCAL, counter, null, r.line()));
        int top = out.size();
        emit(new Instr(Op.LOAD_LOCAL, counter, null, r.line()));
        emit(new Instr(Op.PUSH, 0, 0.0, r.line()));
        emit(new Instr(Op.GT, r.line()));
        int exit = emit(new Instr(Op.JUMP_IF_FALSE, 0, null, r.line()));
        loops.push(new LoopLabels());
        block(r.body());
        LoopLabels labels = loops.pop();
        int step = out.size();
        for (int c : labels.continues) {
            patch(c, step);
        }
        emit(new Instr(Op.LOAD_LOCAL, counter, null, r.line()));
        emit(new Instr(Op.PUSH, 0, 1.0, r.line()));
        emit(new Instr(Op.SUB, r.line()));
        emit(new Instr(Op.STORE_LOCAL, counter, null, r.line()));
        emit(new Instr(Op.JUMP, top, null, r.line()));
        patch(exit, out.size());
        for (int b : labels.breaks) {
            patch(b, out.size());
        }
    }

    private void read(Ast.Read r) {
        expr(r.source().target());
        emit(new Instr(Op.READ, 0, r.source().name(), r.line()));
        if (r.into() == null) {
            emit(new Instr(Op.POP, r.line()));
            return;
        }
        Integer slot = findLocal(r.into());
        if (slot != null) {
            emit(new Instr(Op.STORE_LOCAL, slot, null, r.line()));
        } else if (constants.contains(r.into())) {
            throw new SynapseException("Cannot assign to constant '" + r.into() + "'", r.line(), 1);
        } else if (globals.contains(r.into())) {
            emit(new Instr(Op.STORE_GLOBAL, 0, r.into(), r.line()));
        } else {
            throw new SynapseException("Undeclared variable '" + r.into() + "'; declare it with VAR or LET", r.line(), 1);
        }
    }

    private String implant(String name, int line) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (knownImplants != null && !knownImplants.contains(lower)) {
            throw new SynapseException("Unknown implant '" + name + "'", line, 1);
        }
        return lower;
    }

    private void assign(Ast.Set set) {
        switch (set.target()) {
            case Ast.Name n -> {
                expr(set.value());
                Integer slot = findLocal(n.name());
                if (slot != null) {
                    emit(new Instr(Op.STORE_LOCAL, slot, null, set.line()));
                } else if (constants.contains(n.name())) {
                    throw new SynapseException("Cannot assign to constant '" + n.name() + "'", set.line(), 1);
                } else if (globals.contains(n.name())) {
                    emit(new Instr(Op.STORE_GLOBAL, 0, n.name(), set.line()));
                } else {
                    throw new SynapseException("Undeclared variable '" + n.name() + "'; declare it with VAR or LET",
                            set.line(), 1);
                }
            }
            case Ast.Member m -> {
                expr(m.target());
                expr(set.value());
                emit(new Instr(Op.SET_MEMBER, 0, m.name(), set.line()));
            }
            default -> throw new SynapseException("Invalid assignment target", set.line(), 1);
        }
    }

    private void ifStatement(Ast.If iff) {
        List<Integer> endJumps = new ArrayList<>();
        for (Ast.Branch b : iff.branches()) {
            expr(b.condition());
            int jumpFalse = emit(new Instr(Op.JUMP_IF_FALSE, 0, null, iff.line()));
            block(b.body());
            endJumps.add(emit(new Instr(Op.JUMP, 0, null, iff.line())));
            patch(jumpFalse, out.size());
        }
        if (!iff.orElse().isEmpty()) {
            block(iff.orElse());
        }
        for (int j : endJumps) {
            patch(j, out.size());
        }
    }

    // ---------------------------------------------------------------- expressions

    private void expr(Expr e) {
        switch (e) {
            case Ast.Num n -> emit(new Instr(Op.PUSH, 0, n.value(), n.line()));
            case Ast.Bool b -> emit(new Instr(Op.PUSH, 0, b.value(), b.line()));
            case Ast.Str s -> emit(new Instr(Op.PUSH, 0, s.value(), s.line()));
            case Ast.Null n -> emit(new Instr(Op.PUSH, 0, null, n.line()));
            case Ast.Dur d -> emit(new Instr(Op.PUSH, 0, new Ticks(d.ticks()), d.line()));
            case Ast.Name n -> {
                Integer slot = findLocal(n.name());
                if (slot != null) {
                    emit(new Instr(Op.LOAD_LOCAL, slot, null, n.line()));
                } else {
                    emit(new Instr(Op.LOAD_NAME, 0, n.name(), n.line()));
                }
            }
            case Ast.Member m -> {
                expr(m.target());
                emit(new Instr(Op.MEMBER, 0, m.name(), m.line()));
            }
            case Ast.Call c -> call(c);
            case Ast.Unary u -> {
                expr(u.operand());
                emit(new Instr(u.op().equals("-") ? Op.NEG : Op.NOT, u.line()));
            }
            case Ast.Binary b -> binary(b);
        }
    }

    private void call(Ast.Call c) {
        String name = c.function().toUpperCase(Locale.ROOT);
        Integer arity = BUILTINS.get(name);
        if (arity == null) {
            throw new SynapseException("Unknown function '" + c.function() + "'", c.line(), 1);
        }
        if (arity != c.args().size()) {
            throw new SynapseException(name + " takes " + arity + " argument(s)", c.line(), 1);
        }
        if (STRING_FUNCTIONS.contains(name) && !isa.atLeast(IsaLevel.ISA_2)) {
            throw new SynapseException(name + " (string operations) requires ISA-2 (this deck runs ISA-" + isa.number() + ")",
                    c.line(), 1);
        }
        for (Expr arg : c.args()) {
            expr(arg);
        }
        emit(new Instr(Op.CALL, arity, name, c.line()));
    }

    private void binary(Ast.Binary b) {
        if (b.op().equals("AND") || b.op().equals("OR")) {
            expr(b.left());
            int jump = emit(new Instr(b.op().equals("AND") ? Op.AND_JUMP : Op.OR_JUMP, 0, null, b.line()));
            expr(b.right());
            patch(jump, out.size());
            return;
        }
        expr(b.left());
        expr(b.right());
        Op op = switch (b.op()) {
            case "+" -> Op.ADD;
            case "-" -> Op.SUB;
            case "*" -> Op.MUL;
            case "/" -> Op.DIV;
            case "%" -> Op.MOD;
            case "==" -> Op.EQ;
            case "!=" -> Op.NE;
            case "<" -> Op.LT;
            case "<=" -> Op.LE;
            case ">" -> Op.GT;
            case ">=" -> Op.GE;
            default -> throw new SynapseException("Unknown operator " + b.op(), b.line(), 1);
        };
        emit(new Instr(op, b.line()));
    }

    // ---------------------------------------------------------------- plumbing

    private void declareGlobal(String name, int line) {
        if (!globals.add(name)) {
            throw new SynapseException("'" + name + "' is declared twice", line, 1);
        }
    }

    private Integer findLocal(String name) {
        for (Map<String, Integer> scope : scopes) {
            Integer slot = scope.get(name);
            if (slot != null) {
                return slot;
            }
        }
        return null;
    }

    private void begin() {
        out = new ArrayList<>();
        loops.clear();
        scopes = new ArrayDeque<>();
        scopes.push(new HashMap<>());
        nextSlot = 0;
    }

    private Code finish() {
        out.add(new Instr(Op.END, 0));
        return new Code(out, nextSlot);
    }

    private int emit(Instr i) {
        out.add(i);
        return out.size() - 1;
    }

    private void patch(int index, int target) {
        Instr old = out.get(index);
        out.set(index, new Instr(old.op(), target, old.k(), old.line()));
    }
}
