package dev.cyberimplants.deck;

import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.config.ServerConfig;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.synapse.IsaLevel;

/**
 * What the installed deck can do. A chassis needs exactly one Processor, one RAM and one Storage module to boot in
 * Layer 1 (docs/design/03-cyberdeck.md, section 3.2).
 *
 * @param booted       all required parts are installed
 * @param chassisTier  tier of the chassis (0 if absent)
 * @param cyclesPerTick Processor budget for the one thread
 * @param isa          language level: the Processor's, capped by {@code layers.maxEnabled}
 * @param ramCells     program memory in cells (1 KB = 64 cells)
 * @param storageBytes program storage in bytes
 */
public record DeckHardware(boolean booted, int chassisTier, int cyclesPerTick, IsaLevel isa, int ramCells, int storageBytes) {
    public static final DeckHardware NONE = new DeckHardware(false, 0, 0, IsaLevel.ISA_1, 0, 0);

    public static DeckHardware of(BodyState s) {
        InstalledImplant chassis = s.installed().get(new SlotId(BodyRegion.SPINE, 0));
        if (chassis == null) {
            return NONE;
        }
        int processor = s.tierOf(ImplantIds.DECK_PROCESSOR);
        int ram = s.tierOf(ImplantIds.DECK_RAM);
        int storage = s.tierOf(ImplantIds.DECK_STORAGE);
        if (processor == 0 || ram == 0 || storage == 0) {
            return new DeckHardware(false, chassis.tier(), 0, IsaLevel.ISA_1, 0, 0);
        }
        int isaNumber = (int) Stats.of(ImplantIds.DECK_PROCESSOR, processor).value("isa", 1);
        int cap = layerCap();
        IsaLevel isa = IsaLevel.fromNumber(Math.max(1, Math.min(Math.min(isaNumber, cap), 5)));
        int cycles = (int) Math.min(Stats.of(ImplantIds.DECK_PROCESSOR, processor).value("cycles_per_tick", 20),
                ServerConfig.DECK_MAX_CYCLES_PER_PLAYER_PER_TICK.get());
        int ramCells = (int) (Stats.of(ImplantIds.DECK_RAM, ram).value("kb", 2) * 64);
        int storageBytes = (int) (Stats.of(ImplantIds.DECK_STORAGE, storage).value("kb", 4) * 1024);
        return new DeckHardware(true, chassis.tier(), cycles, isa, ramCells, storageBytes);
    }

    /** Layer 1 runs ISA-1; higher layers raise the cap one level each (layers 3 and 4 share ISA-4). */
    private static int layerCap() {
        int layer = ServerConfig.LAYERS_MAX_ENABLED.get();
        return layer >= 3 ? layer + 1 : layer;
    }
}
