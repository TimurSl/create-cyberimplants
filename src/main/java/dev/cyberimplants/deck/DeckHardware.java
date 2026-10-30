package dev.cyberimplants.deck;

import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.config.Layers;
import dev.cyberimplants.config.ServerConfig;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.synapse.IsaLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What the installed deck can do (docs/design/03-cyberdeck.md, sections 3.2-3.3). A chassis needs exactly one
 * Processor and at least one RAM and Storage module to boot; RAM and Storage stack; each Coprocessor adds threads.
 *
 * @param booted             all required parts are installed
 * @param chassisTier        tier of the chassis (0 if absent)
 * @param cyclesPerTick      Processor budget for thread 0
 * @param isa                language level: the Processor's, capped by {@code layers.maxEnabled}
 * @param ramCells           program memory in cells (1 KB = 64 cells), summed over RAM modules
 * @param storageBytes       program storage in bytes, summed over Storage modules
 * @param extraThreadCycles  cycle budget of each Coprocessor thread (thread 1, 2, ...)
 * @param latencyTicks       neural latency between a script's activation and the implant firing
 * @param macroKeys          usable deck macro keys
 * @param logLines           program log buffer
 * @param minEveryTicks      shortest {@code EVERY} interval the Processor allows
 * @param machineInterface   tier of the installed Machine Interface (0 = none; the chassis' built-in interface is T1)
 * @param processorTier      tier of the Processor (0 if absent); Tier V can overclock
 */
public record DeckHardware(boolean booted, int chassisTier, int cyclesPerTick, IsaLevel isa, int ramCells, int storageBytes,
                           List<Integer> extraThreadCycles, int latencyTicks, int macroKeys, int logLines,
                           int minEveryTicks, int machineInterface, int processorTier) {
    public static final DeckHardware NONE = new DeckHardware(false, 0, 0, IsaLevel.ISA_1, 0, 0, List.of(), 0, 0, 20, 1, 0, 0);

    public DeckHardware {
        extraThreadCycles = List.copyOf(extraThreadCycles);
    }

    public int threads() {
        return 1 + extraThreadCycles.size();
    }

    /** Cycles per tick of a thread (0 = Processor). */
    public int cyclesFor(int thread) {
        return thread == 0 ? cyclesPerTick : thread - 1 < extraThreadCycles.size() ? extraThreadCycles.get(thread - 1) : 0;
    }

    public static DeckHardware of(BodyState s) {
        InstalledImplant chassis = s.installed().get(new SlotId(BodyRegion.SPINE, 0));
        if (chassis == null) {
            return NONE;
        }
        var chassisStats = Stats.of(ImplantIds.DECK_CHASSIS, chassis.tier());
        int latency = (int) chassisStats.value("latency_ticks", 0);
        int macroKeys = (int) chassisStats.value("macro_keys", 2);
        int logLines = (int) chassisStats.value("log_lines", 20);

        int processor = 0;
        double ramKb = 0;
        double storageKb = 0;
        int machineInterface = 0;
        List<Integer> extra = new ArrayList<>();
        for (Map.Entry<SlotId, InstalledImplant> e : s.installed().entrySet()) {
            if (e.getKey().region() != BodyRegion.DECK) {
                continue;
            }
            InstalledImplant module = e.getValue();
            var stats = Stats.of(module.type(), module.tier());
            if (module.type().equals(ImplantIds.DECK_PROCESSOR)) {
                processor = module.tier();
            } else if (module.type().equals(ImplantIds.DECK_RAM)) {
                ramKb += stats.value("kb", 2);
            } else if (module.type().equals(ImplantIds.DECK_STORAGE)) {
                storageKb += stats.value("kb", 4);
            } else if (module.type().equals(ImplantIds.DECK_COPROCESSOR) && Layers.atLeast(2)) {
                for (int i = 0; i < (int) stats.value("extra_threads", 1); i++) {
                    extra.add((int) stats.value("cycles_per_tick", 10));
                }
            } else if (module.type().equals(ImplantIds.DECK_MACHINE_INTERFACE) && Layers.atLeast(2)) {
                machineInterface = Math.max(machineInterface, module.tier());
            }
        }
        if (processor == 0 || ramKb == 0 || storageKb == 0) {
            return new DeckHardware(false, chassis.tier(), 0, IsaLevel.ISA_1, 0, 0, List.of(), latency, macroKeys, logLines, 1, 0, processor);
        }
        var processorStats = Stats.of(ImplantIds.DECK_PROCESSOR, processor);
        int isaNumber = (int) processorStats.value("isa", 1);
        IsaLevel isa = IsaLevel.fromNumber(Math.max(1, Math.min(Math.min(isaNumber, layerCap()), 5)));
        int cap = ServerConfig.DECK_MAX_CYCLES_PER_PLAYER_PER_TICK.get();
        int cycles = (int) Math.min(processorStats.value("cycles_per_tick", 20), cap);
        List<Integer> capped = extra.stream().map(c -> Math.min(c, cap)).toList();
        return new DeckHardware(true, chassis.tier(), cycles, isa, (int) (ramKb * 64), (int) (storageKb * 1024), capped,
                latency, macroKeys, logLines, (int) processorStats.value("min_every_ticks", 1), machineInterface, processor);
    }

    /** The ISA a layer allows: layer 1 runs ISA-1, layer 2 ISA-2, and layers 3 and 4 share ISA-4 (layer 3 adds ISA-3). */
    private static int layerCap() {
        int layer = Layers.maxEnabled();
        return layer >= 3 ? layer + 1 : layer;
    }
}
