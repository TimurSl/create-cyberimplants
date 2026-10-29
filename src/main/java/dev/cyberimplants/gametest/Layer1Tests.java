package dev.cyberimplants.gametest;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyOps;
import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import dev.cyberimplants.implant.neural.Sandevistan;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Layer 1 exit-criteria GameTests: the Rig's install rules, the damage pipeline and the Sandevistan field.
 * These use the shared empty structure {@code cyberimplants:empty}.
 */
@GameTestHolder(CyberImplants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Layer1Tests {
    private static BodyState withCore(BodyState s) {
        return s.withImplant(new SlotId(BodyRegion.CORE, 0), new InstalledImplant(ImplantIds.CORE, 1)).withCharge(100, 0);
    }

    private static BodyState with(BodyState s, BodyRegion region, int index, ResourceLocation type) {
        return s.withImplant(new SlotId(region, index), new InstalledImplant(type, 1));
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
        return player;
    }

    @GameTest(template = "empty")
    public static void rigRefusesWithoutCoreAndInWrongSlots(GameTestHelper helper) {
        BodyState empty = BodyState.EMPTY;
        SlotId cortex = new SlotId(BodyRegion.CORTEX, 0);
        helper.assertTrue(BodyOps.canInstall(empty, ImplantIds.SANDEVISTAN, 1, cortex) == BodyOps.Result.NO_CORE,
                "an implant needs a Core first");
        BodyState cored = withCore(empty);
        helper.assertTrue(BodyOps.canInstall(cored, ImplantIds.SANDEVISTAN, 1, new SlotId(BodyRegion.LEGS, 0)) == BodyOps.Result.WRONG_SLOT,
                "Sandevistan belongs in the cortex");
        helper.assertTrue(BodyOps.canInstall(cored, ImplantIds.RETRACTABLE_BLADE, 1, new SlotId(BodyRegion.LEFT_SOCKET, 0)) == BodyOps.Result.NO_ARM_FRAME,
                "a limb module needs an arm frame");
        helper.assertTrue(BodyOps.canInstall(cored, ImplantIds.DECK_RAM, 1, new SlotId(BodyRegion.DECK, 0)) == BodyOps.Result.NO_CHASSIS,
                "a deck module needs a chassis");
        BodyState oneReflex = with(cored, BodyRegion.CORTEX, 0, ImplantIds.REFLEX_BOOSTER);
        helper.assertTrue(BodyOps.canInstall(oneReflex, ImplantIds.REFLEX_BOOSTER, 1, new SlotId(BodyRegion.CORTEX, 1)) == BodyOps.Result.DUPLICATE,
                "no duplicates within a region");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rigRejectsLoadAboveCapacityTimesMargin(GameTestHelper helper) {
        // Tier I Core: Capacity 30, +10% margin = 33. Eight implants worth 30 Load fit; a ninth worth 4 does not.
        BodyState s = withCore(BodyState.EMPTY);
        s = with(s, BodyRegion.CORTEX, 0, ImplantIds.SANDEVISTAN);      // 9
        s = with(s, BodyRegion.CORTEX, 1, ImplantIds.KERENZIKOV);       // 4  -> 13
        s = with(s, BodyRegion.OCULAR, 0, ImplantIds.OPTICAL_CAMOUFLAGE); // 6 -> 19
        s = with(s, BodyRegion.OCULAR, 1, ImplantIds.ENTITY_SCANNER);   // 3  -> 22
        s = with(s, BodyRegion.CIRCULATORY, 0, ImplantIds.DEFIBRILLATOR); // 4 -> 26
        s = with(s, BodyRegion.CIRCULATORY, 1, ImplantIds.BIOMONITOR);  // 1  -> 27
        s = with(s, BodyRegion.CIRCULATORY, 2, ImplantIds.ERS);         // 3  -> 30
        helper.assertTrue(BodyOps.canInstall(s, ImplantIds.POWER_ACTUATORS, 1, new SlotId(BodyRegion.SKELETON, 0)) == BodyOps.Result.OVERLOAD,
                "30 + 4 Load is above 33");
        helper.assertTrue(BodyOps.canInstall(s, ImplantIds.REINFORCED_SKELETON, 1, new SlotId(BodyRegion.SKELETON, 0)).ok(),
                "30 + 3 Load is exactly at the limit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void damagePipelineAppliesNanocoatingAndSkeleton(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        BodyState body = withCore(BodyState.EMPTY);
        body = with(body, BodyRegion.INTEGUMENT, 0, ImplantIds.NANOCOATING);
        body = with(body, BodyRegion.SKELETON, 0, ImplantIds.REINFORCED_SKELETON);
        BodyState.set(player, body);

        // Fire is a Nanocoating family: balanced formula takes 4% off. 10 damage becomes 9.6.
        float before = player.getHealth();
        player.hurt(player.damageSources().inFire(), 10.0f);
        float fireLoss = before - player.getHealth();
        helper.assertTrue(Math.abs(fireLoss - 9.6f) < 0.05f, "expected 9.6 fire damage, took " + fireLoss);

        // A mob melee hit is blunt: Reinforced Skeleton subtracts a flat 0.5 per hit.
        player.setHealth(20.0f);
        player.invulnerableTime = 0;
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 3, 1, 3);
        before = player.getHealth();
        player.hurt(player.damageSources().mobAttack(zombie), 4.0f);
        float bluntLoss = before - player.getHealth();
        helper.assertTrue(Math.abs(bluntLoss - 3.5f) < 0.05f, "expected 3.5 blunt damage, took " + bluntLoss);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void sandevistanFieldSlowsEntitiesInsideIt(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        BodyState body = withCore(BodyState.EMPTY);
        body = with(body, BodyRegion.CORTEX, 0, ImplantIds.SANDEVISTAN);
        BodyState.set(player, body);

        UseResult result = Sandevistan.ACTION.use(player, UseContext.MANUAL);
        helper.assertTrue(result.ok(), "Sandevistan should activate, got " + result);

        // Two falling items start at the same height: one inside the 3-block field, one well outside it.
        ItemEntity inside = helper.spawnItem(Items.STONE, 1.5f, 3.0f, 1.5f);
        ItemEntity outside = helper.spawnItem(Items.STONE, 7.5f, 3.0f, 7.5f);
        double startInside = inside.getY();
        double startOutside = outside.getY();

        helper.runAfterDelay(20, () -> {
            double droppedInside = startInside - inside.getY();
            double droppedOutside = startOutside - outside.getY();
            helper.assertTrue(droppedOutside > 1.0, "the control item should have fallen, dropped " + droppedOutside);
            helper.assertTrue(droppedInside < droppedOutside * 0.5,
                    "the item inside the field should fall much slower: inside " + droppedInside + ", outside " + droppedOutside);
            helper.succeed();
        });
    }

    private Layer1Tests() {
    }
}
