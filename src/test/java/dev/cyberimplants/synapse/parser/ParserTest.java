package dev.cyberimplants.synapse.parser;

import dev.cyberimplants.synapse.IsaLevel;
import dev.cyberimplants.synapse.SynapseException;
import dev.cyberimplants.synapse.ast.Ast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    private static Ast.ProgramNode parse(String src) {
        return Parser.parse(src, IsaLevel.ISA_1);
    }

    @Test
    void parsesTheFirstDesignExample() {
        Ast.ProgramNode p = parse("""
                PROGRAM "Close-Quarters Chrono"
                REQUIRES entity_scanner, sandevistan

                ON enemy_detected:
                    IF distance < 8:
                        ACTIVATE sandevistan WITH duration = 3s
                """);
        assertEquals("Close-Quarters Chrono", p.name());
        assertEquals(java.util.List.of("entity_scanner", "sandevistan"), p.requires());
        assertEquals(5, p.priority());
        assertFalse(p.autostart());
        assertEquals(1, p.handlers().size());

        Ast.Handler h = p.handlers().get(0);
        assertEquals("enemy_detected", h.event());
        Ast.If iff = assertInstanceOf(Ast.If.class, h.body().get(0));
        Ast.Activate act = assertInstanceOf(Ast.Activate.class, iff.branches().get(0).body().get(0));
        assertEquals("sandevistan", act.implant());
        assertEquals(60.0, assertInstanceOf(Ast.Dur.class, act.with().get("duration")).ticks());
    }

    @Test
    void parsesHeaderOptionsAndDeclarations() {
        Ast.ProgramNode p = parse("""
                PROGRAM "x"
                PRIORITY 7
                AUTOSTART
                CONST LIMIT = 30
                VAR count = 0
                ON boot:
                    LOG "up"
                """);
        assertEquals(7, p.priority());
        assertTrue(p.autostart());
        assertEquals("LIMIT", p.consts().get(0).name());
        assertEquals("count", p.vars().get(0).name());
    }

    @Test
    void handlerArgumentAndWhenFilter() {
        Ast.ProgramNode p = parse("""
                PROGRAM "x"
                ON health_below 30 WHEN self.charge > 10:
                    ALERT "low"
                ON key 1:
                    TOGGLE thermal
                """);
        assertEquals(30.0, p.handlers().get(0).argument());
        assertNotNull(p.handlers().get(0).when());
        assertEquals(1.0, p.handlers().get(1).argument());
    }

    @Test
    void elifAndElseChain() {
        Ast.ProgramNode p = parse("""
                PROGRAM "x"
                ON boot:
                    IF a:
                        LOG 1
                    ELIF b:
                        LOG 2
                    ELSE:
                        LOG 3
                """);
        Ast.If iff = assertInstanceOf(Ast.If.class, p.handlers().get(0).body().get(0));
        assertEquals(2, iff.branches().size());
        assertEquals(1, iff.orElse().size());
    }

    @Test
    void keywordsAreCaseInsensitiveAndScanIsContextual() {
        Ast.ProgramNode p = parse("""
                program "x"
                on boot:
                    scan
                    let n = scan.nearest
                    log n
                """);
        var body = p.handlers().get(0).body();
        assertInstanceOf(Ast.Scan.class, body.get(0));
        Ast.Let let = assertInstanceOf(Ast.Let.class, body.get(1));
        assertInstanceOf(Ast.Member.class, let.value());
    }

    @Test
    void operatorPrecedence() {
        Ast.ProgramNode p = parse("""
                PROGRAM "x"
                ON boot:
                    LET v = 1 + 2 * 3 == 7 AND NOT FALSE OR TRUE
                """);
        Ast.Let let = (Ast.Let) p.handlers().get(0).body().get(0);
        Ast.Binary or = assertInstanceOf(Ast.Binary.class, let.value());
        assertEquals("OR", or.op());
        Ast.Binary and = assertInstanceOf(Ast.Binary.class, or.left());
        assertEquals("AND", and.op());
        Ast.Binary eq = assertInstanceOf(Ast.Binary.class, and.left());
        assertEquals("==", eq.op());
        Ast.Binary plus = assertInstanceOf(Ast.Binary.class, eq.left());
        assertEquals("+", plus.op());
        assertEquals("*", assertInstanceOf(Ast.Binary.class, plus.right()).op());
    }

    @Test
    void negativeLiteralsFold() {
        Ast.ProgramNode p = parse("PROGRAM \"x\"\nCONST A = -3\nON boot:\n    LOG A\n");
        assertEquals(-3.0, assertInstanceOf(Ast.Num.class, p.consts().get(0).value()).value());
    }

    @Test
    void higherIsaFeaturesAreRejectedWithTheLevelThatIntroducesThem() {
        assertIsaError("PROGRAM \"x\"\nON boot:\n    WHILE TRUE:\n        LOG 1\n", "ISA-2");
        assertIsaError("PROGRAM \"x\"\nEVERY 2s:\n    LOG 1\n", "ISA-2");
        assertIsaError("PROGRAM \"x\"\nCHANNEL c = link \"A\"\n", "ISA-2");
        assertIsaError("PROGRAM \"x\"\nON boot:\n    CONFIGURE aes x = 1\n", "ISA-2");
        assertIsaError("PROGRAM \"x\"\nON boot:\n    FOR i IN xs:\n        LOG i\n", "ISA-3");
        assertIsaError("PROGRAM \"x\"\nFUNC f(x):\n    RETURN x\n", "ISA-3");
        assertIsaError("PROGRAM \"x\"\nON boot:\n    LET a = [1, 2]\n", "ISA-3");
        assertIsaError("PROGRAM \"x\"\nON boot:\n    TRY:\n        LOG 1\n", "ISA-4");
        assertIsaError("PROGRAM \"x\"\nPERSIST VAR a = 1\n", "ISA-5");
    }

    @Test
    void parsesDevicesAndFromClauses() {
        Ast.ProgramNode p = parse("""
                PROGRAM "Overspeed Watch"
                DEVICE press = port "Press Line A"
                ON rotational_speed_changed FROM press:
                    LOG press.speed
                """);
        assertEquals("Press Line A", p.devices().get(0).label());
        assertEquals("press", p.devices().get(0).name());
        assertEquals("press", p.handlers().get(0).from());
    }

    @Test
    void onlyPortDevicesExistInLayerOne() {
        assertThrows(SynapseException.class, () -> parse("PROGRAM \"x\"\nDEVICE p = sensor \"A\"\n"));
    }

    @Test
    void reportsPositionOfSyntaxErrors() {
        SynapseException e = assertThrows(SynapseException.class,
                () -> parse("PROGRAM \"x\"\nON boot:\n    LET = 3\n"));
        assertEquals(3, e.line());
    }

    @Test
    void programMustStartWithProgram() {
        assertThrows(SynapseException.class, () -> parse("ON boot:\n    LOG 1\n"));
    }

    @Test
    void emptyBlockIsAnError() {
        assertThrows(SynapseException.class, () -> parse("PROGRAM \"x\"\nON boot:\nON key 1:\n    LOG 1\n"));
    }

    @Test
    void priorityOutOfRangeIsAnError() {
        assertThrows(SynapseException.class, () -> parse("PROGRAM \"x\"\nPRIORITY 12\n"));
    }

    private static void assertIsaError(String src, String level) {
        SynapseException e = assertThrows(SynapseException.class, () -> parse(src), src);
        assertTrue(e.getMessage().contains(level), e.getMessage());
    }
}
