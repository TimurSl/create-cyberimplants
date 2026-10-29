package dev.cyberimplants.synapse.vm;

/** A runtime error that aborts the current task. */
public final class VmException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final VmError error;
    private final int line;

    public VmException(VmError error, String message, int line) {
        super(message);
        this.error = error;
        this.line = line;
    }

    public VmError error() {
        return error;
    }

    public int line() {
        return line;
    }
}
