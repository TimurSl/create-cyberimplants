package dev.cyberimplants.synapse.lexer;

import dev.cyberimplants.synapse.SynapseException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Converts Synapse source to tokens, including INDENT/DEDENT layout tokens.
 *
 * <p>Rules (docs/design/04-synapse-language.md, section 3): {@code #} comments, indentation-structured blocks
 * (spaces or tabs, but one kind per file), numbers, duration literals ({@code 500ms 3s 20t 2m}), strings with
 * {@code \" \\ \n} escapes, and operators. Newlines inside (), [] and {} do not end a logical line.
 */
public final class Lexer {
    private static final String[] TWO_CHAR_OPS = {"==", "!=", "<=", ">="};
    private static final String ONE_CHAR_OPS = "+-*/%<>=()[]{},.:";

    private final String src;
    private final List<Token> out = new ArrayList<>();
    private final Deque<Integer> indents = new ArrayDeque<>();

    private int pos;
    private int line = 1;
    private int lineStart;
    private int bracketDepth;
    private char indentChar; // 0 until the first indented line fixes it

    private Lexer(String source) {
        this.src = source.replace("\r\n", "\n").replace('\r', '\n');
        indents.push(0);
    }

    public static List<Token> tokenize(String source) {
        return new Lexer(source).run();
    }

    private List<Token> run() {
        boolean atLineStart = true;
        while (pos < src.length()) {
            if (atLineStart && bracketDepth == 0) {
                if (!handleLineStart()) {
                    continue; // blank or comment-only line was consumed
                }
                atLineStart = false;
                continue;
            }

            char c = src.charAt(pos);
            if (c == '\n') {
                if (bracketDepth == 0) {
                    emit(TokenType.NEWLINE, "", 0);
                    atLineStart = true;
                }
                newline();
            } else if (c == ' ' || c == '\t') {
                pos++;
            } else if (c == '#') {
                skipComment();
            } else if (isDigit(c)) {
                number();
            } else if (isWordStart(c)) {
                word();
            } else if (c == '"') {
                string();
            } else {
                operator();
            }
        }

        if (bracketDepth != 0) {
            throw error("Unclosed bracket at end of file");
        }
        if (!out.isEmpty() && out.get(out.size() - 1).type() != TokenType.NEWLINE) {
            emit(TokenType.NEWLINE, "", 0);
        }
        while (indents.size() > 1) {
            indents.pop();
            emit(TokenType.DEDENT, "", 0);
        }
        emit(TokenType.EOF, "", 0);
        return out;
    }

    /**
     * Measures indentation at the start of a physical line and emits INDENT/DEDENT.
     *
     * @return true if a logical line starts here, false if the line was blank or a comment and was consumed
     */
    private boolean handleLineStart() {
        int width = 0;
        int start = pos;
        while (pos < src.length() && (src.charAt(pos) == ' ' || src.charAt(pos) == '\t')) {
            char c = src.charAt(pos);
            if (indentChar == 0) {
                indentChar = c;
            } else if (c != indentChar) {
                throw error("Mixed tabs and spaces in indentation");
            }
            width++;
            pos++;
        }

        if (pos >= src.length()) {
            return false;
        }
        char c = src.charAt(pos);
        if (c == '\n') {
            newline();
            return false;
        }
        if (c == '#') {
            skipComment();
            if (pos < src.length() && src.charAt(pos) == '\n') {
                newline();
            }
            return false;
        }

        int col = pos - lineStart + 1;
        if (width > indents.peek()) {
            indents.push(width);
            out.add(new Token(TokenType.INDENT, "", 0, line, col));
        } else {
            while (width < indents.peek()) {
                indents.pop();
                out.add(new Token(TokenType.DEDENT, "", 0, line, col));
            }
            if (width != indents.peek()) {
                pos = start;
                throw error("Inconsistent dedent");
            }
        }
        return true;
    }

    private void newline() {
        pos++;
        line++;
        lineStart = pos;
    }

    private void skipComment() {
        while (pos < src.length() && src.charAt(pos) != '\n') {
            pos++;
        }
    }

    private void number() {
        int start = pos;
        int col = column();
        while (pos < src.length() && isDigit(src.charAt(pos))) {
            pos++;
        }
        if (pos + 1 < src.length() && src.charAt(pos) == '.' && isDigit(src.charAt(pos + 1))) {
            pos++;
            while (pos < src.length() && isDigit(src.charAt(pos))) {
                pos++;
            }
        }
        double value = Double.parseDouble(src.substring(start, pos));

        String unit = durationUnit();
        if (unit != null) {
            pos += unit.length();
            double ticks = switch (unit) {
                case "ms" -> value / 50.0;
                case "s" -> value * 20.0;
                case "t" -> value;
                default -> value * 1200.0; // "m"
            };
            out.add(new Token(TokenType.DURATION, src.substring(start, pos), Math.rint(ticks), line, col));
        } else {
            if (pos < src.length() && isWordPart(src.charAt(pos))) {
                throw error("Unknown number suffix");
            }
            out.add(new Token(TokenType.NUMBER, src.substring(start, pos), value, line, col));
        }
    }

    /** The duration unit directly after the number, or null. A unit must not run into further word characters. */
    private String durationUnit() {
        for (String unit : new String[]{"ms", "s", "t", "m"}) {
            if (src.startsWith(unit, pos)) {
                int end = pos + unit.length();
                if (end >= src.length() || !isWordPart(src.charAt(end))) {
                    return unit;
                }
            }
        }
        return null;
    }

    private void word() {
        int start = pos;
        int col = column();
        while (pos < src.length() && isWordPart(src.charAt(pos))) {
            pos++;
        }
        out.add(new Token(TokenType.WORD, src.substring(start, pos), 0, line, col));
    }

    private void string() {
        int col = column();
        pos++; // opening quote
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length() || src.charAt(pos) == '\n') {
                throw new SynapseException("Unterminated string", line, col);
            }
            char c = src.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                if (pos >= src.length()) {
                    throw new SynapseException("Unterminated string", line, col);
                }
                char e = src.charAt(pos++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    default -> throw new SynapseException("Unknown escape \\" + e, line, pos - lineStart);
                }
            } else {
                sb.append(c);
            }
        }
        out.add(new Token(TokenType.STRING, sb.toString(), 0, line, col));
    }

    private void operator() {
        int col = column();
        for (String op : TWO_CHAR_OPS) {
            if (src.startsWith(op, pos)) {
                pos += 2;
                out.add(new Token(TokenType.OP, op, 0, line, col));
                return;
            }
        }
        char c = src.charAt(pos);
        if (ONE_CHAR_OPS.indexOf(c) >= 0) {
            pos++;
            if (c == '(' || c == '[' || c == '{') {
                bracketDepth++;
            } else if (c == ')' || c == ']' || c == '}') {
                if (bracketDepth == 0) {
                    throw new SynapseException("Unmatched '" + c + "'", line, col);
                }
                bracketDepth--;
            }
            out.add(new Token(TokenType.OP, String.valueOf(c), 0, line, col));
            return;
        }
        throw new SynapseException("Unexpected character '" + c + "'", line, col);
    }

    private void emit(TokenType type, String text, double number) {
        out.add(new Token(type, text, number, line, column()));
    }

    private int column() {
        return pos - lineStart + 1;
    }

    private SynapseException error(String message) {
        return new SynapseException(message, line, column());
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isWordStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isWordPart(char c) {
        return isWordStart(c) || isDigit(c);
    }
}
