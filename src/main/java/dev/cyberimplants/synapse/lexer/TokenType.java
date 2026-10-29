package dev.cyberimplants.synapse.lexer;

public enum TokenType {
    /** A bare word: keyword or identifier. The parser decides from context (see {@link Keywords}). */
    WORD,
    NUMBER,
    /** A duration literal such as {@code 3s}; {@link Token#number()} holds the value in ticks. */
    DURATION,
    STRING,
    /** An operator or punctuation: {@code == != <= >= + - * / % < > = ( ) [ ] { } , . :}. */
    OP,
    NEWLINE,
    INDENT,
    DEDENT,
    EOF
}
