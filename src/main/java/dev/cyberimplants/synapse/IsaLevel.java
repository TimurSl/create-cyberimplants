package dev.cyberimplants.synapse;

/** Language feature levels granted by the Processor module tier (docs/design/04-synapse-language.md, section 7). */
public enum IsaLevel {
    ISA_1,
    ISA_2,
    ISA_3,
    ISA_4,
    ISA_5;

    public int number() {
        return ordinal() + 1;
    }

    public boolean atLeast(IsaLevel other) {
        return compareTo(other) >= 0;
    }

    public static IsaLevel fromNumber(int n) {
        if (n < 1 || n > values().length) {
            throw new IllegalArgumentException("ISA level out of range: " + n);
        }
        return values()[n - 1];
    }
}
