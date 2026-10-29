package dev.cyberimplants.synapse.parser;

import dev.cyberimplants.synapse.IsaLevel;
import dev.cyberimplants.synapse.SynapseException;
import dev.cyberimplants.synapse.ast.Ast;
import dev.cyberimplants.synapse.ast.Ast.Expr;
import dev.cyberimplants.synapse.ast.Ast.Stmt;
import dev.cyberimplants.synapse.lexer.Lexer;
import dev.cyberimplants.synapse.lexer.Token;
import dev.cyberimplants.synapse.lexer.TokenType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recursive-descent / Pratt parser for Synapse (docs/design/04-synapse-language.md, section 13).
 *
 * <p>Layer 1 implements ISA-1. Every statement and declaration that belongs to a higher ISA is recognised and
 * rejected with an error naming the level that introduces it, so players get a useful message instead of a
 * generic syntax error.
 */
public final class Parser {
    private final List<Token> tokens;
    private final IsaLevel isa;
    private int pos;

    private Parser(List<Token> tokens, IsaLevel isa) {
        this.tokens = tokens;
        this.isa = isa;
    }

    public static Ast.ProgramNode parse(String source, IsaLevel isa) {
        return new Parser(Lexer.tokenize(source), isa).program();
    }

    // ---------------------------------------------------------------- program

    private Ast.ProgramNode program() {
        skipNewlines();
        if (!peek().isKeyword("PROGRAM")) {
            throw error(peek(), "A program must start with PROGRAM \"name\"");
        }
        next();
        String name = expect(TokenType.STRING, "a program name in quotes").text();
        endOfLine();

        List<String> requires = new ArrayList<>();
        int priority = 5;
        boolean autostart = false;
        List<Ast.Const> consts = new ArrayList<>();
        List<Ast.Var> vars = new ArrayList<>();
        List<Ast.Handler> handlers = new ArrayList<>();

        skipNewlines();
        while (!peek().is(TokenType.EOF)) {
            Token t = peek();
            if (!t.is(TokenType.WORD)) {
                throw error(t, "Unexpected " + describe(t) + " at top level");
            }
            String kw = t.text().toUpperCase(Locale.ROOT);
            switch (kw) {
                case "REQUIRES" -> {
                    next();
                    do {
                        String req = expect(TokenType.WORD, "a hardware name").text();
                        if (peek().isOp(":")) {
                            next();
                            req += ":" + expect(TokenType.STRING, "a quoted name").text();
                        }
                        requires.add(req);
                    } while (acceptOp(","));
                    endOfLine();
                }
                case "PRIORITY" -> {
                    next();
                    Token n = expect(TokenType.NUMBER, "a priority from 0 to 9");
                    if (n.number() < 0 || n.number() > 9 || n.number() != Math.floor(n.number())) {
                        throw error(n, "PRIORITY must be a whole number from 0 to 9");
                    }
                    priority = (int) n.number();
                    endOfLine();
                }
                case "AUTOSTART" -> {
                    next();
                    autostart = true;
                    endOfLine();
                }
                case "CONST" -> {
                    next();
                    Token id = expect(TokenType.WORD, "a constant name");
                    expectOp("=");
                    consts.add(new Ast.Const(id.text(), expression(), id.line()));
                    endOfLine();
                }
                case "VAR" -> {
                    next();
                    Token id = expect(TokenType.WORD, "a variable name");
                    expectOp("=");
                    vars.add(new Ast.Var(id.text(), expression(), id.line()));
                    endOfLine();
                }
                case "PERSIST" -> throw needs(t, "PERSIST", IsaLevel.ISA_5);
                case "DEVICE", "CHANNEL", "SENSOR" -> throw needs(t, kw, IsaLevel.ISA_2);
                case "FUNC" -> throw needs(t, "FUNC", IsaLevel.ISA_3);
                case "EVERY" -> throw needs(t, "EVERY", IsaLevel.ISA_2);
                case "ON" -> handlers.add(handler());
                default -> throw error(t, "Unexpected '" + t.text() + "' at top level; behaviour belongs in ON handlers");
            }
            skipNewlines();
        }
        return new Ast.ProgramNode(name, requires, priority, autostart, consts, vars, handlers);
    }

    private Ast.Handler handler() {
        Token on = next();
        Token event = expect(TokenType.WORD, "an event name");
        String eventName = event.text();
        if (eventName.equalsIgnoreCase("message")) {
            throw needs(event, "ON message", IsaLevel.ISA_3);
        }
        Double argument = null;
        if (peek().is(TokenType.NUMBER)) {
            argument = next().number();
        }
        Expr when = null;
        while (!peek().isOp(":")) {
            Token t = peek();
            if (t.isKeyword("WHEN") && when == null) {
                next();
                when = expression();
            } else if (t.isKeyword("FROM")) {
                throw needs(t, "FROM <device>", IsaLevel.ISA_2);
            } else if (t.isKeyword("DEBOUNCE")) {
                throw needs(t, "DEBOUNCE", IsaLevel.ISA_2);
            } else {
                throw error(t, "Expected ':' to open the handler, found " + describe(t));
            }
        }
        List<Stmt> body = block();
        return new Ast.Handler(eventName, argument, when, body, on.line());
    }

    // ---------------------------------------------------------------- statements

    private List<Stmt> block() {
        expectOp(":");
        if (!peek().is(TokenType.NEWLINE)) {
            throw error(peek(), "Expected a new line and an indented block after ':'");
        }
        skipNewlines();
        if (!peek().is(TokenType.INDENT)) {
            throw error(peek(), "Expected an indented block");
        }
        next();
        List<Stmt> body = new ArrayList<>();
        while (!peek().is(TokenType.DEDENT) && !peek().is(TokenType.EOF)) {
            if (peek().is(TokenType.NEWLINE)) {
                next();
                continue;
            }
            body.add(statement());
        }
        if (body.isEmpty()) {
            throw error(peek(), "A block needs at least one statement");
        }
        if (peek().is(TokenType.DEDENT)) {
            next();
        }
        return body;
    }

    private Stmt statement() {
        Token t = peek();
        if (t.is(TokenType.WORD)) {
            String kw = t.text().toUpperCase(Locale.ROOT);
            Token after = peekAt(1);
            // 'scan.entities' etc. is an expression even though SCAN is also a statement keyword.
            boolean memberAccess = after.isOp(".") || after.isOp("(");
            if (!memberAccess) {
                switch (kw) {
                    case "LET" -> {
                        next();
                        Token id = expect(TokenType.WORD, "a variable name");
                        expectOp("=");
                        Stmt s = new Ast.Let(id.text(), expression(), t.line());
                        endOfLine();
                        return s;
                    }
                    case "SET" -> {
                        next();
                        Expr target = postfix();
                        if (!(target instanceof Ast.Name) && !(target instanceof Ast.Member)) {
                            throw error(t, "SET needs a variable or a property on the left");
                        }
                        expectOp("=");
                        Stmt s = new Ast.Set(target, expression(), t.line());
                        endOfLine();
                        return s;
                    }
                    case "IF" -> {
                        return ifStatement();
                    }
                    case "ACTIVATE" -> {
                        return activate();
                    }
                    case "DEACTIVATE" -> {
                        next();
                        Stmt s = new Ast.Deactivate(expect(TokenType.WORD, "an implant name").text(), t.line());
                        endOfLine();
                        return s;
                    }
                    case "TOGGLE" -> {
                        next();
                        Stmt s = new Ast.Toggle(expect(TokenType.WORD, "an implant name").text(), t.line());
                        endOfLine();
                        return s;
                    }
                    case "LOG" -> {
                        next();
                        Stmt s = new Ast.Log(expression(), t.line());
                        endOfLine();
                        return s;
                    }
                    case "ALERT" -> {
                        next();
                        Stmt s = new Ast.Alert(expression(), t.line());
                        endOfLine();
                        return s;
                    }
                    case "SCAN" -> {
                        next();
                        endOfLine();
                        return new Ast.Scan(t.line());
                    }
                    case "TAG" -> {
                        next();
                        Stmt s = new Ast.Tag(expression(), t.line());
                        endOfLine();
                        return s;
                    }
                    case "UNTAG" -> {
                        next();
                        Stmt s = new Ast.Untag(expression(), t.line());
                        endOfLine();
                        return s;
                    }
                    case "STOP" -> {
                        next();
                        endOfLine();
                        return new Ast.Stop(t.line());
                    }
                    case "ELIF", "ELSE" -> throw error(t, kw + " without a matching IF");
                    case "WHILE", "REPEAT", "WAIT", "BREAK", "CONTINUE", "CONFIGURE", "SEND", "TRANSMIT", "READ", "MANIPULATE" ->
                            throw needs(t, kw, IsaLevel.ISA_2);
                    case "FOR", "RETURN", "EMIT", "BIND" -> throw needs(t, kw, IsaLevel.ISA_3);
                    case "TRY" -> throw needs(t, kw, IsaLevel.ISA_4);
                    case "AWAIT", "REQUEST", "OVERCLOCK" -> throw needs(t, kw, IsaLevel.ISA_5);
                    default -> {
                    }
                }
            }
        }
        Expr e = expression();
        Stmt s = new Ast.ExprStmt(e, t.line());
        endOfLine();
        return s;
    }

    private Stmt ifStatement() {
        Token start = next(); // IF
        List<Ast.Branch> branches = new ArrayList<>();
        Expr cond = expression();
        branches.add(new Ast.Branch(cond, block()));
        List<Stmt> orElse = List.of();
        while (true) {
            skipNewlines();
            if (peek().isKeyword("ELIF")) {
                next();
                Expr c = expression();
                branches.add(new Ast.Branch(c, block()));
            } else if (peek().isKeyword("ELSE")) {
                next();
                orElse = block();
                break;
            } else {
                break;
            }
        }
        return new Ast.If(branches, orElse, start.line());
    }

    private Stmt activate() {
        Token start = next(); // ACTIVATE
        String implant = expect(TokenType.WORD, "an implant name").text();
        Expr at = null;
        Map<String, Expr> with = new LinkedHashMap<>();
        if (peek().isKeyword("AT")) {
            next();
            at = expression();
        }
        if (peek().isKeyword("WITH")) {
            next();
            do {
                Token key = expect(TokenType.WORD, "a parameter name");
                expectOp("=");
                with.put(key.text(), expression());
            } while (acceptOp(","));
        }
        endOfLine();
        return new Ast.Activate(implant, at, with, start.line());
    }

    // ---------------------------------------------------------------- expressions

    private Expr expression() {
        return or();
    }

    private Expr or() {
        Expr left = and();
        while (peek().isKeyword("OR")) {
            Token op = next();
            left = new Ast.Binary("OR", left, and(), op.line());
        }
        return left;
    }

    private Expr and() {
        Expr left = not();
        while (peek().isKeyword("AND")) {
            Token op = next();
            left = new Ast.Binary("AND", left, not(), op.line());
        }
        return left;
    }

    private Expr not() {
        if (peek().isKeyword("NOT")) {
            Token op = next();
            return new Ast.Unary("NOT", not(), op.line());
        }
        return comparison();
    }

    private Expr comparison() {
        Expr left = additive();
        while (true) {
            Token t = peek();
            if (t.is(TokenType.OP) && (t.text().equals("==") || t.text().equals("!=") || t.text().equals("<")
                    || t.text().equals("<=") || t.text().equals(">") || t.text().equals(">="))) {
                next();
                left = new Ast.Binary(t.text(), left, additive(), t.line());
            } else {
                return left;
            }
        }
    }

    private Expr additive() {
        Expr left = multiplicative();
        while (peek().isOp("+") || peek().isOp("-")) {
            Token op = next();
            left = new Ast.Binary(op.text(), left, multiplicative(), op.line());
        }
        return left;
    }

    private Expr multiplicative() {
        Expr left = unary();
        while (peek().isOp("*") || peek().isOp("/") || peek().isOp("%")) {
            Token op = next();
            left = new Ast.Binary(op.text(), left, unary(), op.line());
        }
        return left;
    }

    private Expr unary() {
        if (peek().isOp("-")) {
            Token op = next();
            Expr operand = unary();
            if (operand instanceof Ast.Num n) {
                return new Ast.Num(-n.value(), op.line());
            }
            return new Ast.Unary("-", operand, op.line());
        }
        return postfix();
    }

    private Expr postfix() {
        Expr e = primary();
        while (true) {
            if (peek().isOp(".")) {
                Token dot = next();
                Token name = expect(TokenType.WORD, "a property name");
                e = new Ast.Member(e, name.text(), dot.line());
            } else if (peek().isOp("[")) {
                throw needs(peek(), "indexing", IsaLevel.ISA_3);
            } else {
                return e;
            }
        }
    }

    private Expr primary() {
        Token t = next();
        switch (t.type()) {
            case NUMBER:
                return new Ast.Num(t.number(), t.line());
            case DURATION:
                return new Ast.Dur(t.number(), t.line());
            case STRING:
                return new Ast.Str(t.text(), t.line());
            case WORD:
                if (t.isKeyword("TRUE")) {
                    return new Ast.Bool(true, t.line());
                }
                if (t.isKeyword("FALSE")) {
                    return new Ast.Bool(false, t.line());
                }
                if (t.isKeyword("NULL")) {
                    return new Ast.Null(t.line());
                }
                if (peek().isOp("(")) {
                    next();
                    List<Expr> args = new ArrayList<>();
                    if (!peek().isOp(")")) {
                        do {
                            args.add(expression());
                        } while (acceptOp(","));
                    }
                    expectOp(")");
                    return new Ast.Call(t.text(), args, t.line());
                }
                return new Ast.Name(t.text(), t.line());
            case OP:
                if (t.text().equals("(")) {
                    Expr e = expression();
                    expectOp(")");
                    return e;
                }
                if (t.text().equals("[")) {
                    throw needs(t, "list literals", IsaLevel.ISA_3);
                }
                if (t.text().equals("{")) {
                    throw needs(t, "record literals", IsaLevel.ISA_4);
                }
                throw error(t, "Unexpected '" + t.text() + "' in expression");
            default:
                throw error(t, "Expected an expression, found " + describe(t));
        }
    }

    // ---------------------------------------------------------------- helpers

    private Token peek() {
        return tokens.get(pos);
    }

    private Token peekAt(int offset) {
        return tokens.get(Math.min(pos + offset, tokens.size() - 1));
    }

    private Token next() {
        Token t = tokens.get(pos);
        if (!t.is(TokenType.EOF)) {
            pos++;
        }
        return t;
    }

    private Token expect(TokenType type, String what) {
        Token t = peek();
        if (!t.is(type)) {
            throw error(t, "Expected " + what + ", found " + describe(t));
        }
        return next();
    }

    private void expectOp(String op) {
        Token t = peek();
        if (!t.isOp(op)) {
            throw error(t, "Expected '" + op + "', found " + describe(t));
        }
        next();
    }

    private boolean acceptOp(String op) {
        if (peek().isOp(op)) {
            next();
            return true;
        }
        return false;
    }

    private void endOfLine() {
        Token t = peek();
        if (t.is(TokenType.NEWLINE)) {
            next();
        } else if (!t.is(TokenType.EOF) && !t.is(TokenType.DEDENT)) {
            throw error(t, "Unexpected " + describe(t) + " at end of statement");
        }
    }

    private void skipNewlines() {
        while (peek().is(TokenType.NEWLINE)) {
            next();
        }
    }

    private static String describe(Token t) {
        return switch (t.type()) {
            case EOF -> "end of file";
            case NEWLINE -> "end of line";
            case INDENT -> "an unexpected indent";
            case DEDENT -> "a dedent";
            case STRING -> "a string";
            default -> "'" + t.text() + "'";
        };
    }

    private SynapseException error(Token at, String message) {
        return new SynapseException(message, at.line(), at.column());
    }

    private SynapseException needs(Token at, String feature, IsaLevel required) {
        if (!isa.atLeast(required)) {
            return error(at, feature + " requires " + label(required) + " (this deck runs " + label(isa) + ")");
        }
        return error(at, feature + " is not available in this version of the mod yet");
    }

    private static String label(IsaLevel level) {
        return "ISA-" + level.number();
    }
}
