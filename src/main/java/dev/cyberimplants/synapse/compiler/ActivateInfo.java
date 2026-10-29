package dev.cyberimplants.synapse.compiler;

import java.util.List;

/** Operand of {@link Op#ACTIVATE}: the implant, whether an AT target is on the stack, and the WITH keys in order. */
public record ActivateInfo(String implant, boolean hasTarget, List<String> keys) {
}
