package dev.cyberimplants.synapse.vm;

import dev.cyberimplants.synapse.IsaLevel;
import dev.cyberimplants.synapse.SynapseException;
import dev.cyberimplants.synapse.compiler.CompiledProgram;
import dev.cyberimplants.synapse.compiler.Compiler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Layer 5: ISA-5 AWAIT ANY, PERSIST, OVERCLOCK, REQUEST, logistics reads and method calls such as predict.*. */
class Isa5Test {
    private static ProgramRunner load(String src, FakeHost host) {
        CompiledProgram program = Compiler.compile(src, IsaLevel.ISA_5);
        ProgramRunner runner = new ProgramRunner(program, host, Integer.MAX_VALUE);
        runner.start();
        return runner;
    }

    private static void ticks(ProgramRunner r, int n) {
        for (int i = 0; i < n; i++) {
            r.tick(1000);
        }
    }

    @Test
    void isa5StatementsNeedIsa5() {
        for (String body : List.of("AWAIT ANY damaged", "OVERCLOCK", "REQUEST \"a\" COUNT 1 FROM \"b\" TO \"c\"")) {
            SynapseException e = assertThrows(SynapseException.class,
                    () -> Compiler.compile("PROGRAM \"x\"\nON boot:\n    " + body + "\n", IsaLevel.ISA_4));
            assertTrue(e.getMessage().contains("ISA-5"), e.getMessage());
        }
        SynapseException e = assertThrows(SynapseException.class,
                () -> Compiler.compile("PROGRAM \"x\"\nON boot:\n    LOG STOCK(\"a\", \"b\")\n", IsaLevel.ISA_2));
        assertTrue(e.getMessage().contains("ISA-4"), e.getMessage());
    }

    @Test
    void awaitResumesWithTheEventThatFired() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "await"
                VAR got = ""
                ON boot:
                    AWAIT ANY damaged, health_below 30 INTO ev
                    SET got = ev.name + ":" + STR(ev.amount)
                """, host);
        ticks(r, 2);
        assertEquals(1, r.awaiting());
        assertTrue(r.subscribes("damaged"), "awaited events count as subscriptions");
        r.fire("health_below", 50.0, Map.of());
        ticks(r, 1);
        assertEquals(1, r.awaiting(), "a threshold that doesn't match keeps waiting");
        r.fire("damaged", null, Map.of("amount", 4.0));
        ticks(r, 1);
        assertEquals(0, r.awaiting());
        assertEquals("damaged:4", r.variables().get("got"));
    }

    @Test
    void awaitTimesOutWithNull() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "timeout"
                VAR result = "pending"
                ON boot:
                    AWAIT ANY damaged TIMEOUT 5t INTO ev
                    IF ev == NULL:
                        SET result = "timeout"
                """, host);
        ticks(r, 3);
        assertEquals("pending", r.variables().get("result"));
        ticks(r, 5);
        assertEquals("timeout", r.variables().get("result"));
    }

    @Test
    void awaitIntoAConstantIsACompileError() {
        assertThrows(SynapseException.class,
                () -> Compiler.compile("PROGRAM \"x\"\nCONST K = 1\nON boot:\n    AWAIT ANY damaged INTO K\n", IsaLevel.ISA_5));
    }

    @Test
    void persistedVariablesSurviveARestart() {
        FakeHost host = new FakeHost();
        String src = """
                PROGRAM "laps"
                PERSIST VAR best = 0
                VAR scratch = 0
                ON key 1:
                    SET best = best + 1
                    SET scratch = scratch + 1
                """;
        ProgramRunner r = load(src, host);
        r.fire("key", 1.0, Map.of());
        ticks(r, 1);
        r.fire("key", 1.0, Map.of());
        ticks(r, 1);
        assertEquals(2.0, host.persisted.get("best"));
        assertFalse(host.persisted.containsKey("scratch"), "plain VARs are not persisted");

        ProgramRunner again = load(src, host);
        assertEquals(2.0, again.variables().get("best"), "the saved value replaces the initial one");
        assertEquals(0.0, again.variables().get("scratch"));
    }

    @Test
    void overclockRequestAndStockGoToTheHost() {
        FakeHost host = new FakeHost();
        host.stock.put("Armory/cyberimplants:flechette", 100.0);
        ProgramRunner r = load("""
                PROGRAM "Ammo Restock"
                ON boot:
                    IF STOCK("Armory", "cyberimplants:flechette") < 256:
                        REQUEST "cyberimplants:flechette" COUNT 128 FROM "Armory" TO "Player Locker"
                    OVERCLOCK
                """, host);
        ticks(r, 3);
        assertEquals(List.of("128 cyberimplants:flechette Armory -> Player Locker"), host.requests);
        assertEquals(1, host.overclocks);
    }

    @Test
    void requestNeedsAWholePositiveCount() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "bad"
                ON boot:
                    REQUEST "a" COUNT 0 FROM "b" TO "c"
                """, host);
        ticks(r, 2);
        assertEquals(List.of(VmError.TYPE), host.aborts);
    }

    @Test
    void methodCallsReachTheHandle() {
        FakeHost host = new FakeHost();
        host.globals.put("predict", new Handle() {
            @Override
            public Object get(String member) {
                return null;
            }

            @Override
            public Object invoke(String method, List<Object> args, IsaLevel isa) {
                if (!method.equals("position")) {
                    throw new UnsupportedOperationException(method);
                }
                double seconds = (Double) args.get(1);
                if (seconds > 1 && !isa.atLeast(IsaLevel.ISA_5)) {
                    throw new IllegalArgumentException("horizons over 1 s need ISA-5");
                }
                return new Position(seconds, 0, 0);
            }
        });
        ProgramRunner r = load("""
                PROGRAM "predict"
                VAR at = NULL
                ON boot:
                    SET at = predict.position(self, 2)
                ON key 1:
                    LOG predict.windup(self)
                """, host);
        ticks(r, 2);
        assertEquals(new Position(2, 0, 0), r.variables().get("at"));
        r.fire("key", 1.0, Map.of());
        ticks(r, 2);
        assertEquals(List.of(VmError.TYPE), host.aborts, "an unknown method aborts the task");
    }
}
