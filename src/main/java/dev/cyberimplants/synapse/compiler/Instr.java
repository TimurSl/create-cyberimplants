package dev.cyberimplants.synapse.compiler;

/** One bytecode instruction. {@code line} is the source line for error messages. */
public record Instr(Op op, int a, Object k, int line) {
    public Instr(Op op, int line) {
        this(op, 0, null, line);
    }
}
