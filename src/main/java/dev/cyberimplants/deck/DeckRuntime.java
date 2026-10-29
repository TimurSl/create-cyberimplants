package dev.cyberimplants.deck;

import dev.cyberimplants.synapse.vm.ProgramRunner;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/** A loaded program with its host, log and bookkeeping. Transient: rebuilt on login. */
public final class DeckRuntime {
    private final String programName;
    private final ProgramRunner runner;
    private final Deque<String> log = new ArrayDeque<>();
    private final Random random;
    private final int logLimit;

    String lastError = "";
    double prevHealthPct = Double.NaN;
    double prevCharge = Double.NaN;
    final java.util.Map<String, Double> lastSpeeds = new java.util.HashMap<>();
    final java.util.Set<net.minecraft.resources.ResourceLocation> cooling = new java.util.HashSet<>();
    int lastCycles;
    double cycleDebt;
    long bootTick;

    DeckRuntime(String programName, ProgramRunner runner, long seed, int logLimit) {
        this.programName = programName;
        this.runner = runner;
        this.random = new Random(seed);
        this.logLimit = logLimit;
    }

    public String programName() {
        return programName;
    }

    public ProgramRunner runner() {
        return runner;
    }

    public int lastCycles() {
        return lastCycles;
    }

    public String lastError() {
        return lastError;
    }

    double nextRandom() {
        return random.nextDouble();
    }

    void log(String line) {
        log.addLast(line);
        while (log.size() > logLimit) {
            log.removeFirst();
        }
    }

    public List<String> logLines() {
        return List.copyOf(log);
    }
}
