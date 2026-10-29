package dev.cyberimplants.synapse.lexer;

import java.util.Locale;
import java.util.Set;

/**
 * Reserved words. The lexer never classifies words; the parser treats a WORD as a keyword only where the grammar
 * expects one. That keeps names such as {@code scan} (the Entity Scanner object) and the {@code SCAN} statement
 * apart: a statement-leading {@code scan} followed by {@code .} is a member access, otherwise it is the statement.
 */
public final class Keywords {
    private static final Set<String> ALL = Set.of(
            "PROGRAM", "REQUIRES", "PRIORITY", "AUTOSTART", "CONST", "VAR", "PERSIST", "DEVICE", "CHANNEL", "SENSOR",
            "ON", "EVERY", "FROM", "WHEN", "DEBOUNCE", "TIMEOUT", "FUNC", "RETURN",
            "LET", "SET", "IF", "ELIF", "ELSE", "WHILE", "REPEAT", "TIMES", "FOR", "IN", "TO", "BREAK", "CONTINUE", "STOP",
            "ACTIVATE", "DEACTIVATE", "TOGGLE", "AT", "WITH", "CONFIGURE", "SEND", "STRENGTH", "TRANSMIT", "READ", "INTO",
            "SCAN", "TAG", "UNTAG", "MANIPULATE", "EMIT", "BIND", "AS", "WAIT", "REQUEST", "COUNT", "AWAIT", "ANY",
            "TRY", "ERROR", "OVERCLOCK", "LOG", "ALERT",
            "AND", "OR", "NOT", "TRUE", "FALSE", "NULL");

    public static boolean isReserved(String word) {
        return ALL.contains(word.toUpperCase(Locale.ROOT));
    }

    private Keywords() {
    }
}
