package dev.cyberimplants.synapse.vm;

/** Runtime error codes (docs/design/04-synapse-language.md, section 11). */
public enum VmError {
    TYPE,
    NULL_ACCESS,
    WATCHDOG,
    OUT_OF_MEMORY,
    RECURSION
}
