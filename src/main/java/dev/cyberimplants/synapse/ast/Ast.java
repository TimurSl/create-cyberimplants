package dev.cyberimplants.synapse.ast;

import java.util.List;
import java.util.Map;

/** Synapse syntax tree. Layer 1 covers ISA-1; later layers add nodes. */
public final class Ast {
    private Ast() {
    }

    public sealed interface Expr permits Num, Bool, Str, Null, Dur, Name, Member, Call, Unary, Binary {
        int line();
    }

    public record Num(double value, int line) implements Expr {
    }

    public record Bool(boolean value, int line) implements Expr {
    }

    public record Str(String value, int line) implements Expr {
    }

    public record Null(int line) implements Expr {
    }

    /** A duration literal; {@code ticks} is already converted. */
    public record Dur(double ticks, int line) implements Expr {
    }

    public record Name(String name, int line) implements Expr {
    }

    public record Member(Expr target, String name, int line) implements Expr {
    }

    public record Call(String function, List<Expr> args, int line) implements Expr {
    }

    /** {@code op} is {@code -} or {@code NOT}. */
    public record Unary(String op, Expr operand, int line) implements Expr {
    }

    /** {@code op} is one of {@code + - * / % == != < <= > >= AND OR}. */
    public record Binary(String op, Expr left, Expr right, int line) implements Expr {
    }

    public sealed interface Stmt permits Let, Set, If, Activate, Deactivate, Toggle, Log, Alert, Scan, Tag, Untag, Stop, ExprStmt,
            While, Repeat, Break, Continue, Wait, Configure, Manipulate, Read {
        int line();
    }

    public record Let(String name, Expr value, int line) implements Stmt {
    }

    /** {@code target} is a {@link Name} or a {@link Member}. */
    public record Set(Expr target, Expr value, int line) implements Stmt {
    }

    public record Branch(Expr condition, List<Stmt> body) {
    }

    /** {@code orElse} is empty when there is no ELSE. */
    public record If(List<Branch> branches, List<Stmt> orElse, int line) implements Stmt {
    }

    public record Activate(String implant, Expr at, Map<String, Expr> with, int line) implements Stmt {
    }

    public record Deactivate(String implant, int line) implements Stmt {
    }

    public record Toggle(String implant, int line) implements Stmt {
    }

    public record Log(Expr value, int line) implements Stmt {
    }

    public record Alert(Expr value, int line) implements Stmt {
    }

    public record Scan(int line) implements Stmt {
    }

    public record Tag(Expr entity, int line) implements Stmt {
    }

    public record Untag(Expr entity, int line) implements Stmt {
    }

    public record Stop(int line) implements Stmt {
    }

    /** ISA-2: {@code WHILE cond:}. */
    public record While(Expr condition, List<Stmt> body, int line) implements Stmt {
    }

    /** ISA-2: {@code REPEAT n [TIMES]:}. */
    public record Repeat(Expr count, List<Stmt> body, int line) implements Stmt {
    }

    public record Break(int line) implements Stmt {
    }

    public record Continue(int line) implements Stmt {
    }

    /** ISA-2: {@code WAIT duration}. */
    public record Wait(Expr duration, int line) implements Stmt {
    }

    /** ISA-2: {@code CONFIGURE implant key = value}. */
    public record Configure(String implant, String key, Expr value, int line) implements Stmt {
    }

    /** ISA-2: {@code MANIPULATE target USE | WRENCH | SET_VALUE n | PULL}. {@code argument} is null except for SET_VALUE. */
    public record Manipulate(Expr target, String action, Expr argument, int line) implements Stmt {
    }

    /** ISA-2: {@code READ device.property [INTO name]}: a fresh sample. {@code into} is null when discarded. */
    public record Read(Member source, String into, int line) implements Stmt {
    }

    /** A bare expression, e.g. a call whose value is discarded. */
    public record ExprStmt(Expr value, int line) implements Stmt {
    }

    /**
     * An {@code ON} handler. {@code argument} is the numeric threshold / key number (null if none), {@code from} the
     * device name of a {@code FROM} clause (null if none) and {@code when} the optional filter expression.
     * {@code debounceTicks} is 0 when there is no DEBOUNCE. {@code EVERY d:} is the event {@link #EVERY} with the
     * interval in ticks as its argument.
     */
    public record Handler(String event, Double argument, String from, Expr when, double debounceTicks, List<Stmt> body, int line) {
        public static final String EVERY = "@every";

    }

    /** {@code DEVICE name = port "label"}: a device bound in the deck's Devices list (Layer 1: ports only). */
    public record Device(String name, String kind, String label, int line) {
    }

    public record Const(String name, Expr value, int line) {
    }

    public record Var(String name, Expr value, int line) {
    }

    public record ProgramNode(
            String name,
            List<String> requires,
            int priority,
            boolean autostart,
            List<Const> consts,
            List<Var> vars,
            List<Device> devices,
            List<Handler> handlers) {
    }
}
