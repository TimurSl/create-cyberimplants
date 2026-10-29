package dev.cyberimplants.synapse;

/** A compile-time error in a Synapse program, carrying a source position for the editor. */
public class SynapseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public SynapseException(String message, int line, int column) {
        super(line + ":" + column + ": " + message);
        this.line = line;
        this.column = column;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }
}
