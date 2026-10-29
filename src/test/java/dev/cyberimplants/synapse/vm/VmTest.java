package dev.cyberimplants.synapse.vm;

import dev.cyberimplants.synapse.IsaLevel;
import dev.cyberimplants.synapse.SynapseException;
import dev.cyberimplants.synapse.compiler.CompiledProgram;
import dev.cyberimplants.synapse.compiler.Compiler;
import dev.cyberimplants.synapse.compiler.Ticks;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VmTest {
    private static ProgramRunner load(String src, FakeHost host) {
        return load(src, host, IsaLevel.ISA_1);
    }

    private static ProgramRunner load(String src, FakeHost host, IsaLevel isa) {
        CompiledProgram program = Compiler.compile(src, isa);
        ProgramRunner runner = new ProgramRunner(program, host);
        runner.start();
        return runner;
    }

    private static void drain(ProgramRunner runner) {
        for (int i = 0; i < 50 && runner.queued() > 0; i++) {
            runner.tick(1000);
        }
    }

    @Test
    void firstDesignExampleActivatesSandevistanWhenAnEnemyIsClose() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "Close-Quarters Chrono"
                REQUIRES entity_scanner, sandevistan

                ON enemy_detected:
                    IF distance < 8:
                        ACTIVATE sandevistan WITH duration = 3s
                """, host);

        r.fire("enemy_detected", null, Map.of("distance", 12.0));
        drain(r);
        assertTrue(host.activations.isEmpty());

        r.fire("enemy_detected", null, Map.of("distance", 5.0));
        drain(r);
        assertEquals(1, host.activations.size());
        assertEquals("sandevistan", host.activations.get(0).implant());
        assertEquals(new Ticks(60), host.activations.get(0).with().get("duration"));
    }

    @Test
    void bootHandlerRunsAtStartAndVariablesPersistBetweenTasks() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "counter"
                VAR hits = 0
                CONST STEP = 2
                ON boot:
                    LOG "online"
                ON damaged:
                    SET hits = hits + STEP
                    LOG "hits " + STR(hits)
                """, host, IsaLevel.ISA_2);
        drain(r);
        assertEquals(List.of("online"), host.logs);

        r.fire("damaged", null, Map.of());
        drain(r);
        r.fire("damaged", null, Map.of());
        drain(r);
        assertEquals(4.0, r.variables().get("hits"));
        assertEquals("hits 4", host.logs.get(2));
    }

    @Test
    void thresholdHandlersMatchOnTheirArgument() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "t"
                ON health_below 30:
                    LOG "thirty"
                ON health_below 50:
                    LOG "fifty"
                """, host);
        assertEquals(Set.of(30.0, 50.0), r.arguments("health_below"));
        r.fire("health_below", 50.0, Map.of());
        drain(r);
        assertEquals(List.of("fifty"), host.logs);
    }

    @Test
    void whenFilterIsEvaluatedBeforeQueueing() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "w"
                ON enemy_detected WHEN kind == "hostile":
                    LOG "hostile"
                """, host);
        r.fire("enemy_detected", null, Map.of("kind", "passive"));
        assertEquals(0, r.queued());
        r.fire("enemy_detected", null, Map.of("kind", "hostile"));
        assertEquals(1, r.queued());
    }

    @Test
    void implantPropertiesAndWritesGoThroughHandles() {
        FakeHost host = new FakeHost();
        Handle reflex = FakeHost.object("overdrive", false, "tier", 1.0);
        host.globals.put("reflex", reflex);
        ProgramRunner r = load("""
                PROGRAM "p"
                ON key 1:
                    IF reflex.overdrive == FALSE:
                        SET reflex.overdrive = TRUE
                    LOG reflex.overdrive
                """, host);
        r.fire("key", 1.0, Map.of());
        drain(r);
        assertEquals(Boolean.TRUE, reflex.get("overdrive"));
        assertEquals(List.of("TRUE"), host.logs);
    }

    @Test
    void missingHardwareYieldsNullAndMemberAccessOnNullAbortsOnlyTheTask() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "p"
                ON key 1:
                    LOG sandevistan.ready
                ON key 2:
                    LOG "fine"
                """, host);
        r.fire("key", 1.0, Map.of());
        drain(r);
        assertEquals(List.of(VmError.NULL_ACCESS), host.aborts);
        assertEquals(ProgramRunner.State.RUNNING, r.state());

        r.fire("key", 2.0, Map.of());
        drain(r);
        assertEquals("fine", host.logs.get(host.logs.size() - 1));
    }

    @Test
    void nullComparisonLetsScriptsGuardAgainstMissingHardware() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "p"
                ON key 1:
                    IF sandevistan != NULL AND sandevistan.ready:
                        LOG "go"
                    ELSE:
                        LOG "no"
                """, host);
        r.fire("key", 1.0, Map.of());
        drain(r);
        assertEquals(List.of("no"), host.logs);
    }

    @Test
    void durationsCompareAcrossUnits() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "d"
                ON boot:
                    IF 3s == 60t AND 1m > 59s AND 500ms < 1s:
                        LOG "ok"
                """, host);
        drain(r);
        assertEquals(List.of("ok"), host.logs);
    }

    @Test
    void builtinFunctions() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "f"
                ON boot:
                    LOG ABS(-3)
                    LOG MIN(4, 2)
                    LOG MAX(4, 2)
                    LOG CLAMP(15, 0, 10)
                    LOG ROUND(2.6)
                    LOG FLOOR(2.6)
                    LOG SQRT(16)
                    LOG NUM("12")
                    LOG DIST(POS(0, 0, 0), POS(3, 4, 0))
                    LOG NOW()
                    LOG RANDOM()
                """, host);
        host.now = 7;
        drain(r);
        assertEquals(List.of("3", "2", "4", "10", "3", "2", "4", "12", "5", "7", "0.25"), host.logs);
    }

    @Test
    void stringConcatenationNeedsIsa2() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("PROGRAM \"s\"\nON boot:\n    LOG \"a\" + \"b\"\n", host, IsaLevel.ISA_1);
        drain(r);
        assertEquals(List.of(VmError.TYPE), host.aborts);

        FakeHost host2 = new FakeHost();
        ProgramRunner r2 = load("PROGRAM \"s\"\nON boot:\n    LOG \"a\" + \"b\"\n", host2, IsaLevel.ISA_2);
        drain(r2);
        assertEquals(List.of("ab"), host2.logs);
    }

    @Test
    void divisionByZeroAbortsTheTask() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("PROGRAM \"z\"\nON boot:\n    LOG 1 / 0\n", host);
        drain(r);
        assertEquals(List.of(VmError.TYPE), host.aborts);
    }

    @Test
    void stopUnloadsTheProgram() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("PROGRAM \"s\"\nON key 1:\n    LOG \"a\"\n    STOP\n    LOG \"b\"\n", host);
        r.fire("key", 1.0, Map.of());
        drain(r);
        assertEquals(List.of("a"), host.logs);
        assertEquals(ProgramRunner.State.STOPPED, r.state());
    }

    @Test
    void actionStatementsReachTheHost() {
        FakeHost host = new FakeHost();
        host.globals.put("self", FakeHost.object("charge", 50.0));
        ProgramRunner r = load("""
                PROGRAM "a"
                ON key 1:
                    SCAN
                    TAG target
                    UNTAG target
                    TOGGLE thermal
                    DEACTIVATE camo
                    ACTIVATE sandevistan AT target WITH duration = 2s, radius = 4
                    ALERT "charge " + STR(self.charge)
                """, host, IsaLevel.ISA_2);
        r.fire("key", 1.0, Map.of("target", FakeHost.object("type", "minecraft:zombie")));
        drain(r);
        assertEquals(List.of("scan", "tag", "untag", "toggle thermal", "deactivate camo"), host.actions);
        FakeHost.Activation a = host.activations.get(0);
        assertNotNull(a.at());
        assertEquals(List.of("duration", "radius"), List.copyOf(a.with().keySet()));
        assertEquals(4.0, a.with().get("radius"));
        assertEquals(List.of("charge 50"), host.alerts);
    }

    @Test
    void localsAreBlockScoped() {
        assertThrows(SynapseException.class, () -> Compiler.compile("""
                PROGRAM "l"
                ON boot:
                    IF TRUE:
                        LET x = 1
                    SET x = 2
                """, IsaLevel.ISA_1));
    }

    @Test
    void compileTimeChecks() {
        assertThrows(SynapseException.class, () -> Compiler.compile(
                "PROGRAM \"c\"\nCONST A = 1\nON boot:\n    SET A = 2\n", IsaLevel.ISA_1));
        assertThrows(SynapseException.class, () -> Compiler.compile(
                "PROGRAM \"c\"\nON boot:\n    LOG NOPE(1)\n", IsaLevel.ISA_1));
        assertThrows(SynapseException.class, () -> Compiler.compile(
                "PROGRAM \"c\"\nON boot:\n    LOG ABS(1, 2)\n", IsaLevel.ISA_1));
        assertThrows(SynapseException.class, () -> Compiler.compile(
                "PROGRAM \"c\"\nON boot:\n    ACTIVATE warp_drive\n", IsaLevel.ISA_1, Set.of("sandevistan")));
        assertThrows(SynapseException.class, () -> Compiler.compile(
                "PROGRAM \"c\"\nVAR a = 1\nVAR a = 2\n", IsaLevel.ISA_1));
    }

    @Test
    void cycleBudgetPausesAndResumesATask() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "b"
                ON key 1:
                    ACTIVATE a
                    ACTIVATE b
                    ACTIVATE c
                """, host);
        r.fire("key", 1.0, Map.of());
        int first = r.tick(15); // one ACTIVATE costs 10 cycles
        assertEquals(1, host.activations.size());
        assertTrue(first >= 10);
        assertEquals(1, r.queued());
        r.tick(15);
        r.tick(15);
        assertEquals(3, host.activations.size());
        assertEquals(0, r.queued());
    }

    @Test
    void anInstructionLargerThanTheBudgetStillRuns() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("PROGRAM \"b\"\nON key 1:\n    ACTIVATE a\n", host);
        r.fire("key", 1.0, Map.of());
        r.tick(5);
        assertEquals(1, host.activations.size());
    }

    @Test
    void eventsForAQueuedHandlerAreCoalesced() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "c"
                ON damaged:
                    LOG amount
                """, host);
        r.fire("damaged", null, Map.of("amount", 1.0));
        r.fire("damaged", null, Map.of("amount", 2.0));
        r.fire("damaged", null, Map.of("amount", 3.0));
        assertEquals(1, r.queued());
        drain(r);
        assertEquals(List.of("3"), host.logs);
    }

    @Test
    void queueIsCappedAndCountsDroppedEvents() {
        FakeHost host = new FakeHost();
        StringBuilder src = new StringBuilder("PROGRAM \"q\"\n");
        for (int i = 0; i < 20; i++) {
            src.append("ON ev").append(i).append(":\n    LOG ").append(i).append('\n');
        }
        ProgramRunner r = load(src.toString(), host);
        for (int i = 0; i < 20; i++) {
            r.fire("ev" + i, null, Map.of());
        }
        assertEquals(ProgramRunner.MAX_QUEUE, r.queued());
        assertEquals(4, r.droppedEvents());
    }

    @Test
    void fiveAbortsInTenSecondsCrashTheProgram() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("PROGRAM \"x\"\nON key 1:\n    LOG nothing.here\n", host);
        for (int i = 0; i < 5; i++) {
            r.fire("key", 1.0, Map.of());
            r.tick(100);
        }
        assertEquals(ProgramRunner.State.CRASHED, r.state());
        r.fire("key", 1.0, Map.of());
        assertEquals(0, r.queued());
    }

    @Test
    void slowAbortsDoNotCrashTheProgram() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("PROGRAM \"x\"\nON key 1:\n    LOG nothing.here\n", host);
        for (int i = 0; i < 8; i++) {
            r.fire("key", 1.0, Map.of());
            r.tick(100);
            for (int t = 0; t < 100; t++) {
                r.tick(100);
            }
        }
        assertEquals(ProgramRunner.State.RUNNING, r.state());
    }

    @Test
    void footprintGrowsWithCodeVariablesAndSubscriptions() {
        CompiledProgram small = Compiler.compile("PROGRAM \"a\"\nON boot:\n    LOG 1\n", IsaLevel.ISA_1);
        CompiledProgram bigger = Compiler.compile(
                "PROGRAM \"a\"\nVAR x = 0\nVAR y = 0\nON boot:\n    LOG 1\nON key 1:\n    LET a = 1\n    LOG a\n", IsaLevel.ISA_1);
        assertTrue(bigger.footprintCells() > small.footprintCells());
    }

    @Test
    void shortCircuitSkipsTheRightOperand() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "sc"
                ON boot:
                    IF FALSE AND missing.thing:
                        LOG "no"
                    IF TRUE OR missing.thing:
                        LOG "yes"
                """, host);
        drain(r);
        assertEquals(List.of("yes"), host.logs);
        assertTrue(host.aborts.isEmpty());
    }

    @Test
    void devicesAreBoundAtLoadAndReadThroughTheirHandle() {
        FakeHost host = new FakeHost();
        Handle press = FakeHost.object("speed", 64.0, "type", "port");
        host.devices.put("port:Press Line A", press);
        ProgramRunner r = load("""
                PROGRAM "d"
                DEVICE press = port "Press Line A"
                ON key 1:
                    LOG press.speed
                """, host);
        r.fire("key", 1.0, Map.of());
        drain(r);
        assertEquals(List.of("64"), host.logs);
    }

    @Test
    void fromClauseOnlyMatchesEventsFromThatDevice() {
        FakeHost host = new FakeHost();
        Handle a = FakeHost.object("speed", 1.0);
        Handle b = FakeHost.object("speed", 2.0);
        host.devices.put("port:A", a);
        host.devices.put("port:B", b);
        ProgramRunner r = load("""
                PROGRAM "f"
                DEVICE first = port "A"
                DEVICE second = port "B"
                ON rotational_speed_changed FROM first:
                    LOG "first " + STR(first.speed)
                ON rotational_speed_changed FROM second:
                    LOG "second " + STR(second.speed)
                """, host, IsaLevel.ISA_2);
        r.fire("rotational_speed_changed", null, Map.of("source", b));
        drain(r);
        assertEquals(List.of("second 2"), host.logs);
    }

    @Test
    void deviceNamesCannotBeAssignedAndFromMustNameADevice() {
        assertThrows(SynapseException.class, () -> Compiler.compile(
                "PROGRAM \"c\"\nDEVICE p = port \"A\"\nON boot:\n    SET p = 1\n", IsaLevel.ISA_1));
        assertThrows(SynapseException.class, () -> Compiler.compile(
                "PROGRAM \"c\"\nON boot FROM nothing:\n    LOG 1\n", IsaLevel.ISA_1));
    }

    @Test
    void eventPayloadIsAvailableThroughEventDotField() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("PROGRAM \"e\"\nON enemy_detected:\n    LOG event.distance\n", host);
        r.fire("enemy_detected", null, Map.of("distance", 9.0));
        drain(r);
        assertEquals(List.of("9"), host.logs);
    }
}
