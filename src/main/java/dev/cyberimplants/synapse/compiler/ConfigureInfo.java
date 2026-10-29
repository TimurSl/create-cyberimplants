package dev.cyberimplants.synapse.compiler;

/** Operand of {@link Op#CONFIGURE}: the implant and the setting key; the value is on the stack. */
public record ConfigureInfo(String implant, String key) {
}
