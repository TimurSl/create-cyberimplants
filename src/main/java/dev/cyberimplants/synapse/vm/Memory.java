package dev.cyberimplants.synapse.vm;

/** Memory accounting in cells (docs/design/04-synapse-language.md, section 8.2). */
public final class Memory {
    /** Cells one value occupies. */
    public static int cells(Object v) {
        if (v instanceof String s) {
            return 1 + (s.length() + 7) / 8;
        }
        if (v instanceof dev.cyberimplants.synapse.vm.Position || v instanceof Handle) {
            return 2;
        }
        return 1; // number, bool, null, duration
    }

    private Memory() {
    }
}
