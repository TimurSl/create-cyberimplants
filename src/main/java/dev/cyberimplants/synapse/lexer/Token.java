package dev.cyberimplants.synapse.lexer;

/**
 * One lexical token.
 *
 * @param text   source text for WORD/OP, decoded contents for STRING, empty for layout tokens
 * @param number value for NUMBER, ticks for DURATION, otherwise 0
 */
public record Token(TokenType type, String text, double number, int line, int column) {
    public boolean is(TokenType t) {
        return type == t;
    }

    public boolean isOp(String op) {
        return type == TokenType.OP && text.equals(op);
    }

    /** True for a WORD that equals {@code keyword}, ignoring case. */
    public boolean isKeyword(String keyword) {
        return type == TokenType.WORD && text.equalsIgnoreCase(keyword);
    }

    @Override
    public String toString() {
        return type + (text.isEmpty() ? "" : "(" + text + ")") + "@" + line + ":" + column;
    }
}
