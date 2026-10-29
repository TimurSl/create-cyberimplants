package dev.cyberimplants.synapse.vm;

import dev.cyberimplants.synapse.IsaLevel;
import dev.cyberimplants.synapse.SynapseException;
import dev.cyberimplants.synapse.compiler.CompiledProgram;
import dev.cyberimplants.synapse.compiler.Compiler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Layer 2: ISA-2 loops, WAIT, EVERY, DEBOUNCE, CONFIGURE, device writes, READ, MANIPULATE, strings, RAM, watchdog. */
class Isa2Test {
    private static ProgramRunner load(String src, FakeHost host) {
        return load(src, host, Integer.MAX_VALUE);
    }

    private static ProgramRunner load(String src, FakeHost host, int ram) {
        CompiledProgram program = Compiler.compile(src, IsaLevel.ISA_2);
        ProgramRunner runner = new ProgramRunner(program, host, ram);
        runner.start();
        return runner;
    }

    private static void ticks(ProgramRunner r, int n) {
        for (int i = 0; i < n; i++) {
            r.tick(1000);
        }
    }

    @Test
    void whileRepeatBreakAndContinue() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "loops"
                VAR total = 0
                ON boot:
                    LET i = 0
                    WHILE i < 10:
                        SET i = i + 1
                        IF i == 3:
                            CONTINUE
                        IF i > 6:
                            BREAK
                        SET total = total + i
                    REPEAT 3 TIMES:
                        SET total = total + 100
                """, host);
        ticks(r, 2);
        assertEquals(1.0 + 2 + 4 + 5 + 6 + 300, (Double) r.variables().get("total"));
    }

    @Test
    void breakOutsideALoopIsACompileError() {
        assertThrows(SynapseException.class, () -> Compiler.compile("PROGRAM \"x\"\nON boot:\n    BREAK\n", IsaLevel.ISA_2));
    }

    @Test
    void waitSleepsWithoutBlockingOtherTasks() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "wait"
                ON boot:
                    LOG "a"
                    WAIT 3t
                    LOG "b"
                ON key 1:
                    LOG "key"
                """, host);
        ticks(r, 1);
        assertEquals(List.of("a"), host.logs);
        assertEquals(1, r.sleeping());
        r.fire("key", 1.0, Map.of());
        ticks(r, 1);
        assertEquals(List.of("a", "key"), host.logs);
        ticks(r, 5);
        assertEquals(List.of("a", "key", "b"), host.logs);
        assertEquals(0, r.queued());
    }

    @Test
    void everyFiresOnItsIntervalAndDoesNotPileUp() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "timer"
                VAR n = 0
                EVERY 5t:
                    SET n = n + 1
                """, host);
        ticks(r, 20);
        assertEquals(4.0, (Double) r.variables().get("n"));
    }

    @Test
    void everyBelowTheProcessorMinimumIsRejected() {
        String src = "PROGRAM \"x\"\nEVERY 2t:\n    LOG 1\n";
        assertThrows(SynapseException.class, () -> Compiler.compile(src, IsaLevel.ISA_2, null, new Compiler.Limits(10)));
        assertDoesNotThrow(() -> Compiler.compile(src, IsaLevel.ISA_2, null, new Compiler.Limits(2)));
    }

    @Test
    void debounceLimitsHowOftenAHandlerFires() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "debounce"
                VAR n = 0
                ON damaged DEBOUNCE 10t:
                    SET n = n + 1
                """, host);
        for (int i = 0; i < 25; i++) {
            r.fire("damaged", null, Map.of());
            r.tick(1000);
        }
        assertEquals(3.0, (Double) r.variables().get("n"));
    }

    @Test
    void configureSendReadAndManipulateReachTheHost() {
        FakeHost host = new FakeHost();
        Handle port = FakeHost.object("redstone_out", 0.0, "speed", 64.0);
        host.devices.put("port:Lamp", port);
        ProgramRunner r = load("""
                PROGRAM "io"
                DEVICE lamp = port "Lamp"
                VAR sample = 0
                ON boot:
                    CONFIGURE nanocoating formula = "blast"
                    SEND redstone_signal TO lamp STRENGTH 15
                    READ lamp.speed INTO sample
                    MANIPULATE lamp SET_VALUE 12
                    MANIPULATE lamp USE
                """, host);
        ticks(r, 2);
        assertEquals(15.0, port.get("redstone_out"));
        assertEquals(64.0, (Double) r.variables().get("sample"));
        assertEquals(List.of("configure nanocoating formula blast", "manipulate SET_VALUE 12.0", "manipulate USE null"), host.actions);
    }

    @Test
    void deviceWritesCostEightAndImplantWritesTen() {
        FakeHost host = new FakeHost();
        Handle port = new Handle() {
            @Override
            public Object get(String member) {
                return 0.0;
            }

            @Override
            public boolean set(String member, Object value) {
                return true;
            }

            @Override
            public int writeCost() {
                return 8;
            }
        };
        host.devices.put("port:P", port);
        ProgramRunner r = load("PROGRAM \"c\"\nDEVICE p = port \"P\"\nON boot:\n    SET p.x = 1\n", host);
        ticks(r, 1);
        // device lookup is part of init; the handler is LOAD_NAME(1) PUSH(1) SET_MEMBER(10 - 2 + 8) END(0)
        assertEquals(1 + 1 + 8, r.totalCycles());
    }

    @Test
    void stringFunctions() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "str"
                ON boot:
                    LOG UPPER("abc") + LOWER("DEF") + STR(LEN("four"))
                    LOG CONTAINS("hello", "ell")
                    LOG SUBSTR("abcdef", 2, 3)
                """, host);
        ticks(r, 1);
        assertEquals(List.of("ABCdef4", "TRUE", "cde"), host.logs);
    }

    @Test
    void stringFunctionsNeedIsa2() {
        assertThrows(SynapseException.class, () -> Compiler.compile("PROGRAM \"x\"\nON boot:\n    LOG UPPER(\"a\")\n", IsaLevel.ISA_1));
    }

    @Test
    void aTaskThatExceedsTheRamReservationDiesWithOutOfMemory() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "ram"
                VAR s = ""
                ON key 1:
                    REPEAT 200:
                        SET s = s + "xxxxxxxx"
                """, host, 40);
        r.fire("key", 1.0, Map.of());
        ticks(r, 3);
        assertEquals(List.of(VmError.OUT_OF_MEMORY), host.aborts);
    }

    @Test
    void theWatchdogKillsATaskThatNeverYields() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "spin"
                ON key 1:
                    WHILE TRUE:
                        LET x = 1
                """, host);
        r.fire("key", 1.0, Map.of());
        for (int i = 0; i < 100 && host.aborts.isEmpty(); i++) {
            r.tick(50);
        }
        assertEquals(List.of(VmError.WATCHDOG), host.aborts);
        assertEquals(ProgramRunner.State.RUNNING, r.state());
    }

    @Test
    void aLoopThatWaitsIsNotKilled() {
        FakeHost host = new FakeHost();
        ProgramRunner r = load("""
                PROGRAM "polite"
                VAR n = 0
                ON boot:
                    WHILE TRUE:
                        SET n = n + 1
                        WAIT 1t
                """, host);
        ticks(r, 200);
        assertTrue(host.aborts.isEmpty());
        assertTrue((Double) r.variables().get("n") > 150);
    }

    @Test
    void knownImplantsAreStillCheckedForConfigure() {
        assertThrows(SynapseException.class, () -> Compiler.compile(
                "PROGRAM \"x\"\nON boot:\n    CONFIGURE ghost mode = 1\n", IsaLevel.ISA_2, Set.of("aes")));
    }

    @Test
    void overspeedGuardRunsAgainstDevices() {
        FakeHost host = new FakeHost();
        Handle press = FakeHost.object("speed", 0.0);
        Handle alarm = FakeHost.object("redstone_out", 0.0);
        host.devices.put("port:Press Line A", press);
        host.devices.put("port:Alarm Lamp", alarm);
        ProgramRunner r = load("""
                PROGRAM "Overspeed Guard"
                DEVICE press = port "Press Line A"      # bound in the deck's Devices tab
                DEVICE alarm = port "Alarm Lamp"

                ON rotational_speed_changed FROM press:
                    IF ABS(rotational_speed) > 128:
                        SEND redstone_signal TO alarm STRENGTH 15
                    ELSE:
                        SEND redstone_signal TO alarm STRENGTH 0
                """, host);
        r.fire("rotational_speed_changed", null, Map.of("source", press, "rotational_speed", -200.0));
        ticks(r, 1);
        assertEquals(15.0, alarm.get("redstone_out"));
        r.fire("rotational_speed_changed", null, Map.of("source", press, "rotational_speed", 64.0));
        ticks(r, 1);
        assertEquals(0.0, alarm.get("redstone_out"));
    }
}
