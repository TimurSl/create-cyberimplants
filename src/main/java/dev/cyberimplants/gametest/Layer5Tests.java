package dev.cyberimplants.gametest;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantActions;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Synergies;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import dev.cyberimplants.implant.circulatory.Regenerator;
import dev.cyberimplants.implant.limbs.CyberArm;
import dev.cyberimplants.implant.mobility.HydraulicLegs;
import dev.cyberimplants.implant.mobility.InertialStabilizer;
import dev.cyberimplants.implant.optical.EntityScanner;
import dev.cyberimplants.implant.subdermal.EmergencyShell;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Layer 5 GameTests: the Tier V unlock rows and the actions they add. */
@GameTestHolder(CyberImplants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Layer5Tests {
    private static ServerPlayer player(GameTestHelper helper, BodyState body) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
        BodyState.set(player, body.withImplant(new SlotId(BodyRegion.CORE, 0), new InstalledImplant(ImplantIds.CORE, 5)).withCharge(200, 0));
        return player;
    }

    @GameTest(template = "empty")
    public static void tierFiveRowsNeedTierFive(GameTestHelper helper) {
        helper.assertTrue(!Stats.unlocked(ImplantIds.SANDEVISTAN, 4, "temporal_anchor"), "Tier IV has no Temporal Anchor");
        helper.assertTrue(Stats.unlocked(ImplantIds.SANDEVISTAN, 5, "temporal_anchor"), "Tier V has the Temporal Anchor");
        helper.assertTrue(Stats.unlocked(ImplantIds.SHOCK_CIRCUIT, 5, "chain_one") && Stats.unlocked(ImplantIds.SHOCK_CIRCUIT, 5, "grounded"),
                "Tier V keeps the Tier III chain and adds Grounded");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unlockActionsJoinTheWheelAtTierFive(GameTestHelper helper) {
        helper.assertTrue(!ImplantActions.wheelEligible(ImplantIds.REGENERATOR, 4), "a Tier IV Regenerator has nothing to activate");
        helper.assertTrue(ImplantActions.wheelEligible(ImplantIds.REGENERATOR, 5), "a Tier V Regenerator has Burst");
        helper.assertTrue(ImplantActions.wheelEligible(ImplantIds.SANDEVISTAN, 1), "Active implants stay eligible at every tier");

        ServerPlayer tier4 = player(helper, BodyState.EMPTY.withImplant(new SlotId(BodyRegion.CIRCULATORY, 0),
                new InstalledImplant(ImplantIds.REGENERATOR, 4)));
        helper.assertTrue(ImplantActions.use(tier4, ImplantIds.REGENERATOR, UseContext.MANUAL) == UseResult.DENIED,
                "Burst is refused below Tier V");
        ServerPlayer tier5 = player(helper, BodyState.EMPTY.withImplant(new SlotId(BodyRegion.CIRCULATORY, 0),
                new InstalledImplant(ImplantIds.REGENERATOR, 5)));
        helper.assertTrue(ImplantActions.use(tier5, ImplantIds.REGENERATOR, UseContext.MANUAL).ok(), "Burst starts at Tier V");
        helper.assertTrue(Regenerator.bursting(tier5), "the Regenerator is bursting");
        helper.assertTrue(ImplantActions.use(tier5, ImplantIds.REGENERATOR, UseContext.MANUAL) == UseResult.BUSY, "one Burst at a time");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void manualShellAtAnyHealth(GameTestHelper helper) {
        ServerPlayer player = player(helper, BodyState.EMPTY.withImplant(new SlotId(BodyRegion.INTEGUMENT, 0),
                new InstalledImplant(ImplantIds.EMERGENCY_SHELL, 5)));
        helper.assertTrue(player.getHealth() >= player.getMaxHealth(), "the user is at full health");
        helper.assertTrue(ImplantActions.use(player, ImplantIds.EMERGENCY_SHELL, UseContext.MANUAL).ok(), "the Shell hardens by hand");
        helper.assertTrue(EmergencyShell.active(player), "the Shell is active");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recoilStorageDoublesFallMomentum(GameTestHelper helper) {
        BodyState legs = BodyState.EMPTY.withImplant(new SlotId(BodyRegion.LEGS, 0), new InstalledImplant(ImplantIds.MOMENTUM_BOOSTER, 2));
        ServerPlayer tier4 = player(helper, legs.withImplant(new SlotId(BodyRegion.SKELETON, 0), new InstalledImplant(ImplantIds.IMPACT_DAMPENERS, 4)));
        ServerPlayer tier5 = player(helper, legs.withImplant(new SlotId(BodyRegion.SKELETON, 0), new InstalledImplant(ImplantIds.IMPACT_DAMPENERS, 5)));
        double base = (10 - 3) * Synergies.FALL_MOMENTUM_PER_BLOCK;
        helper.assertTrue(Math.abs(Synergies.fallMomentum(tier4, 10) - base * Synergies.DAMPENER_MOMENTUM_FACTOR) < 1.0E-6, "Tier IV dampeners give x1.5");
        helper.assertTrue(Math.abs(Synergies.fallMomentum(tier5, 10) - base * Synergies.RECOIL_STORAGE_FACTOR) < 1.0E-6, "Tier V Recoil Storage gives x2");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void remainingTierFiveRowsNeedTierFive(GameTestHelper helper) {
        Object[][] rows = {
                {ImplantIds.KINETIC_TENDONS, "elastic_recoil"}, {ImplantIds.MICROGRIP, "ceiling_hang"},
                {ImplantIds.THERMAL_VISION, "heat_trails"}, {ImplantIds.ENTITY_SCANNER, "implant_reading"},
                {ImplantIds.COMBAT_PREDICTOR, "precog"}, {ImplantIds.HYDRAULIC_LEGS, "rebound"},
                {ImplantIds.INERTIAL_STABILIZER, "redirect"}, {ImplantIds.SILENT_ACTUATORS, "ghost_step"},
                {ImplantIds.CYBERNETIC_ARM, "haptic_precision"}};
        for (Object[] row : rows) {
            ResourceLocation id = (ResourceLocation) row[0];
            String unlock = (String) row[1];
            helper.assertTrue(!Stats.unlocked(id, 4, unlock), id + " has no " + unlock + " at Tier IV");
            helper.assertTrue(Stats.unlocked(id, 5, unlock), id + " has " + unlock + " at Tier V");
        }
        helper.assertTrue(Stats.unlocked(ImplantIds.HYDRAULIC_LEGS, 3, "ground_slam") && !Stats.unlocked(ImplantIds.HYDRAULIC_LEGS, 3, "slam_scaling"),
                "the slam arrives at Tier III, its scaling at Tier IV");
        helper.assertTrue(Stats.unlocked(ImplantIds.ENTITY_SCANNER, 3, "auto_scan"), "auto-scan arrives at Tier III");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void slamDamageScalesFromTierFour(GameTestHelper helper) {
        helper.assertTrue(HydraulicLegs.slamDamage(10, false) == 3.0f, "Tier III slams deal a flat 3");
        helper.assertTrue(HydraulicLegs.slamDamage(10, true) == 5.0f, "Tier IV adds 1 per 4 blocks");
        helper.assertTrue(HydraulicLegs.slamDamage(100, true) == 9.0f, "the bonus stops at +6");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armHarvestLevels(GameTestHelper helper) {
        helper.assertTrue(!CyberArm.harvests(0, Blocks.STONE.defaultBlockState()), "a Tier I frame harvests nothing");
        helper.assertTrue(CyberArm.harvests(1, Blocks.STONE.defaultBlockState()), "stone level mines stone");
        helper.assertTrue(!CyberArm.harvests(1, Blocks.IRON_ORE.defaultBlockState()), "stone level cannot mine iron ore");
        helper.assertTrue(CyberArm.harvests(2, Blocks.IRON_ORE.defaultBlockState()), "iron level mines iron ore");
        helper.assertTrue(!CyberArm.harvests(2, Blocks.OBSIDIAN.defaultBlockState()) && CyberArm.harvests(3, Blocks.OBSIDIAN.defaultBlockState()),
                "obsidian needs the diamond level");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hapticPrecisionIsAWheelToggle(GameTestHelper helper) {
        helper.assertTrue(!ImplantActions.wheelEligible(ImplantIds.CYBERNETIC_ARM, 4), "a Tier IV frame has nothing to toggle");
        helper.assertTrue(ImplantActions.wheelEligible(ImplantIds.CYBERNETIC_ARM, 5), "a Tier V frame has Haptic precision");
        ServerPlayer player = player(helper, BodyState.EMPTY.withImplant(new SlotId(BodyRegion.RIGHT_ARM, 0),
                new InstalledImplant(ImplantIds.CYBERNETIC_ARM, 5)));
        helper.assertTrue(ImplantActions.use(player, ImplantIds.CYBERNETIC_ARM, UseContext.MANUAL).ok(), "the toggle switches on");
        helper.assertTrue(Toggles.isOn(player, ImplantIds.CYBERNETIC_ARM), "Haptic precision is on");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scannerReadsImplantCategories(GameTestHelper helper) {
        ServerPlayer player = player(helper, BodyState.EMPTY
                .withImplant(new SlotId(BodyRegion.OCULAR, 0), new InstalledImplant(ImplantIds.THERMAL_VISION, 1))
                .withImplant(new SlotId(BodyRegion.LEGS, 0), new InstalledImplant(ImplantIds.SILENT_ACTUATORS, 1)));
        helper.assertTrue(EntityScanner.implantLetters(player).equals("EH"), "optics and mobility read as E and H, the Core is left out");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void redirectOncePerAirtime(GameTestHelper helper) {
        ServerPlayer player = player(helper, BodyState.EMPTY.withImplant(new SlotId(BodyRegion.LEGS, 0),
                new InstalledImplant(ImplantIds.INERTIAL_STABILIZER, 5)));
        player.setOnGround(false);
        helper.assertTrue(InertialStabilizer.redirect(player), "the first redirect in the air works");
        helper.assertTrue(!InertialStabilizer.redirect(player), "the second one in the same airtime is refused");
        helper.succeed();
    }

    private Layer5Tests() {
    }
}
