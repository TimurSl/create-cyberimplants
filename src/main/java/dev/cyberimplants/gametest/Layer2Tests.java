package dev.cyberimplants.gametest;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyOps;
import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.deck.DeckHardware;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Layer 2 exit-criteria GameTests: five-tier data, socket growth with host tier, Strain and the deck's hardware.
 * They run on the shared empty structure and only touch pure state, so they need no kinetic setup.
 */
@GameTestHolder(CyberImplants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Layer2Tests {
    private static BodyState core(int tier) {
        return BodyState.EMPTY.withImplant(new SlotId(BodyRegion.CORE, 0), new InstalledImplant(ImplantIds.CORE, tier)).withCharge(100, 0);
    }

    private static BodyState put(BodyState s, BodyRegion region, int index, ResourceLocation type, int tier) {
        return s.withImplant(new SlotId(region, index), new InstalledImplant(type, tier));
    }

    @GameTest(template = "empty")
    public static void everyImplantHasDistinctDataForAllFiveTiers(GameTestHelper helper) {
        for (ImplantCatalog.Entry e : ImplantCatalog.ALL) {
            var stats = dev.cyberimplants.data.ImplantStatsRegistry.get(e.id());
            helper.assertTrue(stats.isPresent(), "no stats for " + e.id());
            helper.assertTrue(stats.get().tiers().size() == 5, e.id() + " must have five tiers");
            boolean differs = false;
            for (int t = 2; t <= 5; t++) {
                differs |= !stats.get().tier(t).equals(stats.get().tier(1));
            }
            helper.assertTrue(differs, e.id() + " repeats Tier I");
        }
        helper.assertTrue(Stats.of(ImplantIds.SANDEVISTAN, 5).value("radius", 0) == 7, "Sandevistan T5 radius is 7");
        helper.assertTrue(Stats.of(ImplantIds.KERENZIKOV, 1).value("distance", 0) == 2.0, "Kerenzikov T1 distance is 2");
        helper.assertTrue(Stats.of(ImplantIds.KERENZIKOV, 5).value("distance", 0) == 4.0, "Kerenzikov T5 distance is 4");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deckSocketsFollowTheChassisTier(GameTestHelper helper) {
        BodyState t1 = put(core(5), BodyRegion.SPINE, 0, ImplantIds.DECK_CHASSIS, 1);
        helper.assertTrue(BodyOps.effectiveSlots(t1, BodyRegion.DECK) == 3, "a Tier I chassis has three sockets");
        BodyState full = put(put(put(t1, BodyRegion.DECK, 0, ImplantIds.DECK_PROCESSOR, 1), BodyRegion.DECK, 1, ImplantIds.DECK_RAM, 1),
                BodyRegion.DECK, 2, ImplantIds.DECK_STORAGE, 1);
        helper.assertTrue(BodyOps.canInstall(full, ImplantIds.DECK_MACHINE_INTERFACE, 1, new SlotId(BodyRegion.DECK, 3)) == BodyOps.Result.NO_SOCKET,
                "a fourth module does not fit a Tier I chassis");
        BodyState t2 = put(full, BodyRegion.SPINE, 0, ImplantIds.DECK_CHASSIS, 2);
        helper.assertTrue(BodyOps.effectiveSlots(t2, BodyRegion.DECK) == 4, "a Tier II chassis has four sockets");
        helper.assertTrue(BodyOps.canInstall(t2, ImplantIds.DECK_MACHINE_INTERFACE, 1, new SlotId(BodyRegion.DECK, 3)).ok(),
                "a Machine Interface fits a Tier II chassis");
        helper.assertTrue(BodyOps.canInstall(t2, ImplantIds.DECK_RAM, 1, new SlotId(BodyRegion.DECK, 3)).ok(),
                "RAM modules stack");
        helper.assertTrue(BodyOps.canInstall(t2, ImplantIds.DECK_MACHINE_INTERFACE, 3, new SlotId(BodyRegion.DECK, 3)) == BodyOps.Result.HOST_TIER,
                "a module cannot outrank its chassis");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coprocessorsAddThreadsUpToTheChassisLimit(GameTestHelper helper) {
        BodyState s = put(core(5), BodyRegion.SPINE, 0, ImplantIds.DECK_CHASSIS, 2);
        s = put(put(put(s, BodyRegion.DECK, 0, ImplantIds.DECK_PROCESSOR, 2), BodyRegion.DECK, 1, ImplantIds.DECK_RAM, 2),
                BodyRegion.DECK, 2, ImplantIds.DECK_STORAGE, 2);
        s = put(s, BodyRegion.DECK, 3, ImplantIds.DECK_COPROCESSOR, 2);
        DeckHardware hw = DeckHardware.of(s);
        helper.assertTrue(hw.booted() && hw.threads() == 2, "one Coprocessor adds a thread, got " + hw.threads());
        helper.assertTrue(hw.cyclesFor(1) == 20, "a Tier II Coprocessor thread gets 20 cycles");
        helper.assertTrue(hw.isa().number() == 2, "a Tier II Processor speaks ISA-2");
        helper.assertTrue(hw.latencyTicks() == 3, "a Tier II chassis has 3 ticks of neural latency");
        helper.assertTrue(BodyOps.canInstall(s, ImplantIds.DECK_COPROCESSOR, 1, new SlotId(BodyRegion.DECK, 4)) == BodyOps.Result.NO_SOCKET
                        || BodyOps.canInstall(s, ImplantIds.DECK_COPROCESSOR, 1, new SlotId(BodyRegion.DECK, 4)) == BodyOps.Result.TOO_MANY,
                "a Tier II chassis carries one Coprocessor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armSocketsFollowTheFrameTier(GameTestHelper helper) {
        BodyState s = put(core(5), BodyRegion.LEFT_ARM, 0, ImplantIds.CYBERNETIC_ARM, 3);
        helper.assertTrue(BodyOps.effectiveSlots(s, BodyRegion.LEFT_SOCKET) == 2, "a Tier III frame has two sockets");
        s = put(s, BodyRegion.LEFT_SOCKET, 0, ImplantIds.RETRACTABLE_BLADE, 1);
        helper.assertTrue(BodyOps.canInstall(s, ImplantIds.MICROGRIP, 1, new SlotId(BodyRegion.LEFT_SOCKET, 1)).ok(),
                "a second module fits the second socket");
        helper.assertTrue(BodyOps.canInstall(s, ImplantIds.REMOTE_MANIPULATOR, 1, new SlotId(BodyRegion.LEFT_SOCKET, 2)) == BodyOps.Result.NO_SOCKET,
                "there is no third socket");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void strainBeginsAboveCapacityAndCostsTwentyPercentMore(GameTestHelper helper) {
        // Tier I Core: Capacity 30. Install 32 Load worth of implants directly (the Rig would allow up to 33).
        BodyState s = core(1);
        s = put(s, BodyRegion.CORTEX, 0, ImplantIds.SANDEVISTAN, 1);        // 9
        s = put(s, BodyRegion.CORTEX, 1, ImplantIds.KERENZIKOV, 1);         // 4
        s = put(s, BodyRegion.OCULAR, 0, ImplantIds.OPTICAL_CAMOUFLAGE, 1); // 6
        s = put(s, BodyRegion.OCULAR, 1, ImplantIds.ENTITY_SCANNER, 1);     // 3
        s = put(s, BodyRegion.CIRCULATORY, 0, ImplantIds.DEFIBRILLATOR, 1); // 4
        s = put(s, BodyRegion.CIRCULATORY, 1, ImplantIds.BIOMONITOR, 1);    // 1
        s = put(s, BodyRegion.CIRCULATORY, 2, ImplantIds.ERS, 1);           // 3  -> 30, stable
        helper.assertTrue(!Body.strained(s) && Body.chargeMultiplier(s) == 1.0, "30 of 30 Load is stable");
        s = put(s, BodyRegion.SKELETON, 0, ImplantIds.REINFORCED_SKELETON, 1); // 3 -> 33, strained
        helper.assertTrue(Body.strained(s), "33 of 30 Load is strained");
        helper.assertTrue(Math.abs(Body.chargeMultiplier(s) - 1.2) < 1e-9, "strained bodies pay 20% more Charge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rigTimesAndRequirementsScaleWithTier(GameTestHelper helper) {
        var rpm = dev.cyberimplants.compat.create.block.RigBlockEntity.MIN_RPM;
        var seconds = dev.cyberimplants.compat.create.block.RigBlockEntity.SECONDS;
        helper.assertTrue(rpm[0] == 16 && rpm[4] == 256, "16 to 256 RPM");
        helper.assertTrue(seconds[0] == 5 && seconds[4] == 20, "5 to 20 seconds");
        helper.succeed();
    }

    private Layer2Tests() {
    }
}
