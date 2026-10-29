package dev.cyberimplants.synapse.compiler;

import java.util.List;

/** A compiled unit: a handler body, a WHEN filter or the variable initialisers. */
public record Code(List<Instr> instrs, int localCount) {
    public Code {
        instrs = List.copyOf(instrs);
    }
}
