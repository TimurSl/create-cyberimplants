package dev.cyberimplants.synapse.lexer;

import dev.cyberimplants.synapse.SynapseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.cyberimplants.synapse.lexer.TokenType.*;
import static org.junit.jupiter.api.Assertions.*;

class LexerTest {
    private static List<TokenType> types(String src) {
        return Lexer.tokenize(src).stream().map(Token::type).toList();
    }

    private static List<String> texts(String src) {
        return Lexer.tokenize(src).stream().filter(t -> !t.text().isEmpty()).map(Token::text).toList();
    }

    @Test
    void emptySourceYieldsOnlyEof() {
        assertEquals(List.of(EOF), types(""));
        assertEquals(List.of(EOF), types("\n\n   \n# just a comment\n"));
    }

    @Test
    void lexesTheBriefsFirstExampleProgram() {
        String src = """
                PROGRAM "Close-Quarters Chrono"
                REQUIRES entity_scanner, sandevistan

                ON enemy_detected:
                    IF distance < 8:
                        ACTIVATE sandevistan WITH duration = 3s
                """;
        List<Token> tokens = Lexer.tokenize(src);

        assertEquals(List.of(
                WORD, STRING, NEWLINE,
                WORD, WORD, OP, WORD, NEWLINE,
                WORD, WORD, OP, NEWLINE,
                INDENT, WORD, WORD, OP, NUMBER, OP, NEWLINE,
                INDENT, WORD, WORD, WORD, WORD, OP, DURATION, NEWLINE,
                DEDENT, DEDENT, EOF),
                tokens.stream().map(Token::type).toList());

        Token duration = tokens.stream().filter(t -> t.type() == DURATION).findFirst().orElseThrow();
        assertEquals(60.0, duration.number());
    }

    @Test
    void indentAndDedentAreBalanced() {
        String src = "A:\n    B:\n        C\n    D\nE\n";
        assertEquals(List.of(
                WORD, OP, NEWLINE,
                INDENT, WORD, OP, NEWLINE,
                INDENT, WORD, NEWLINE,
                DEDENT, WORD, NEWLINE,
                DEDENT, WORD, NEWLINE,
                EOF),
                types(src));
    }

    @Test
    void multipleDedentsAtOnce() {
        String src = "A:\n    B:\n        C\nD\n";
        List<TokenType> t = types(src);
        int firstDedent = t.indexOf(DEDENT);
        assertEquals(List.of(DEDENT, DEDENT, WORD), t.subList(firstDedent, firstDedent + 3));
    }

    @Test
    void unwindsOpenBlocksAtEndOfFileWithoutTrailingNewline() {
        assertEquals(List.of(WORD, OP, NEWLINE, INDENT, WORD, NEWLINE, DEDENT, EOF), types("A:\n  B"));
    }

    @Test
    void blankAndCommentLinesDoNotAffectIndentation() {
        String src = "A:\n    B\n\n  # odd comment\n        # deeper comment\n    C\n";
        assertEquals(List.of(
                WORD, OP, NEWLINE,
                INDENT, WORD, NEWLINE,
                WORD, NEWLINE,
                DEDENT, EOF),
                types(src));
    }

    @Test
    void trailingCommentsAreIgnored() {
        assertEquals(List.of("LET", "x", "=", "1"), texts("LET x = 1   # one\n"));
    }

    @Test
    void durationLiteralsAreConvertedToTicks() {
        assertDuration("500ms", 10);
        assertDuration("3s", 60);
        assertDuration("20t", 20);
        assertDuration("2m", 2400);
        assertDuration("0.5s", 10);
        assertDuration("20ms", 0); // 0.4 ticks rounds to the nearest tick
    }

    private static void assertDuration(String literal, double ticks) {
        Token t = Lexer.tokenize(literal).get(0);
        assertEquals(DURATION, t.type(), literal);
        assertEquals(ticks, t.number(), literal);
    }

    @Test
    void numbersAndDecimals() {
        List<Token> t = Lexer.tokenize("12 0.75 3.");
        assertEquals(12, t.get(0).number());
        assertEquals(0.75, t.get(1).number());
        assertEquals(3, t.get(2).number());
        assertTrue(t.get(3).isOp("."));
    }

    @Test
    void negativeNumbersAreMinusPlusNumber() {
        assertEquals(List.of(OP, NUMBER, NEWLINE, EOF), types("-3"));
    }

    @Test
    void numberSuffixThatIsNotADurationUnitIsAnError() {
        SynapseException e = assertThrows(SynapseException.class, () -> Lexer.tokenize("5min"));
        assertEquals(1, e.line());
    }

    @Test
    void keywordsAreCaseInsensitiveButIdentifiersKeepTheirCase() {
        List<Token> t = Lexer.tokenize("on Enemy_Detected");
        assertTrue(t.get(0).isKeyword("ON"));
        assertEquals("Enemy_Detected", t.get(1).text());
    }

    @Test
    void wordsAreNeverClassifiedByTheLexer() {
        // `scan` the object and `SCAN` the statement both lex as plain words; the parser disambiguates.
        List<Token> t = Lexer.tokenize("SCAN\nscan.entities");
        assertEquals(WORD, t.get(0).type());
        assertTrue(Keywords.isReserved("scan"));
        assertEquals(WORD, t.get(2).type());
    }

    @Test
    void operatorsIncludingTwoCharacterForms() {
        assertEquals(
                List.of("a", "==", "b", "!=", "c", "<=", "d", ">=", "e", "<", "f", ">", "g"),
                texts("a == b != c <= d >= e < f > g"));
    }

    @Test
    void stringEscapes() {
        Token t = Lexer.tokenize("\"say \\\"hi\\\"\\nbye\\\\\"").get(0);
        assertEquals(STRING, t.type());
        assertEquals("say \"hi\"\nbye\\", t.text());
    }

    @Test
    void unterminatedStringReportsItsStartPosition() {
        SynapseException e = assertThrows(SynapseException.class, () -> Lexer.tokenize("LET x = \"oops\n"));
        assertEquals(1, e.line());
        assertEquals(9, e.column());
    }

    @Test
    void unknownEscapeIsAnError() {
        assertThrows(SynapseException.class, () -> Lexer.tokenize("\"bad \\q\""));
    }

    @Test
    void newlinesInsideBracketsDoNotEndTheLine() {
        String src = "LET xs = [1,\n   2,\n 3]\nLET y = 1\n";
        List<TokenType> t = types(src);
        assertEquals(2, t.stream().filter(x -> x == NEWLINE).count());
        assertFalse(t.contains(INDENT));
    }

    @Test
    void inconsistentDedentIsAnError() {
        SynapseException e = assertThrows(SynapseException.class,
                () -> Lexer.tokenize("A:\n        B\n    C\n"));
        assertEquals(3, e.line());
    }

    @Test
    void mixingTabsAndSpacesIsAnError() {
        assertThrows(SynapseException.class, () -> Lexer.tokenize("A:\n    B\n\tC\n"));
    }

    @Test
    void tabsAloneAreAcceptedConsistently() {
        assertEquals(List.of(WORD, OP, NEWLINE, INDENT, WORD, NEWLINE, DEDENT, EOF), types("A:\n\tB\n"));
    }

    @Test
    void unmatchedAndUnclosedBracketsAreErrors() {
        assertThrows(SynapseException.class, () -> Lexer.tokenize("a)"));
        assertThrows(SynapseException.class, () -> Lexer.tokenize("(a"));
    }

    @Test
    void unexpectedCharacterReportsPosition() {
        SynapseException e = assertThrows(SynapseException.class, () -> Lexer.tokenize("LET x = 1 @"));
        assertEquals(1, e.line());
        assertEquals(11, e.column());
    }

    @Test
    void windowsLineEndingsAreNormalised() {
        assertEquals(types("A:\n  B\n"), types("A:\r\n  B\r\n"));
    }

    @Test
    void tokenPositionsAreOneBased() {
        List<Token> t = Lexer.tokenize("LET x\n  y");
        assertEquals(1, t.get(0).line());
        assertEquals(1, t.get(0).column());
        assertEquals(5, t.get(1).column());
        Token y = t.stream().filter(x -> x.text().equals("y")).findFirst().orElseThrow();
        assertEquals(2, y.line());
        assertEquals(3, y.column());
    }

    @Test
    void fuzzNeverThrowsAnythingButSynapseException() {
        java.util.Random rnd = new java.util.Random(42);
        String alphabet = "abcXYZ_019 \t\n\r\"\\#:.,()[]{}+-*/%<>=!@$sm";
        for (int i = 0; i < 5000; i++) {
            StringBuilder sb = new StringBuilder();
            int len = rnd.nextInt(40);
            for (int j = 0; j < len; j++) {
                sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
            }
            try {
                List<Token> tokens = Lexer.tokenize(sb.toString());
                assertEquals(EOF, tokens.get(tokens.size() - 1).type());
                long indents = tokens.stream().filter(t -> t.type() == INDENT).count();
                long dedents = tokens.stream().filter(t -> t.type() == DEDENT).count();
                assertEquals(indents, dedents, "unbalanced layout for: " + sb);
            } catch (SynapseException expected) {
                // fine: malformed input
            }
        }
    }
}
