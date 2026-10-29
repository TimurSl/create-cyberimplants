package dev.cyberimplants.gametest;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Limits;
import dev.cyberimplants.implant.PassiveModifiers;
import dev.cyberimplants.implant.Synergies;
import dev.cyberimplants.implant.neural.Kerenzikov;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Layer 4 GameTests for the limitations and synergies implemented so far. */
@GameTestHolder(CyberImplants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Layer4Tests {
    private record Fit(BodyRegion region, int index, ResourceLocation type, int tier) {
    }

    private static ServerPlayer playerWith(GameTestHelper helper, Fit... fits) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
        BodyState body = BodyState.EMPTY.withImplant(new SlotId(BodyRegion.CORE, 0), new InstalledImplant(ImplantIds.CORE, 5));
        for (Fit fit : fits) {
            body = body.withImplant(new SlotId(fit.region(), fit.index()), new InstalledImplant(fit.type(), fit.tier()));
        }
        BodyState.set(player, body.withCharge(200, 0));
        return player;
    }

    @GameTest(template = "empty")
    public static void denseFrameFromTierThree(GameTestHelper helper) {
        for (int tier = 2; tier <= 3; tier++) {
            ServerPlayer player = playerWith(helper, new Fit(BodyRegion.SKELETON, 0, ImplantIds.REINFORCED_SKELETON, tier));
            PassiveModifiers.apply(player);
            double gravity = player.getAttributeValue(Attributes.GRAVITY);
            double vanilla = player.getAttribute(Attributes.GRAVITY).getBaseValue();
            helper.assertTrue(tier == 2 ? Math.abs(gravity - vanilla) < 1.0E-9 : gravity > vanilla,
                    "Tier " + tier + " gravity " + gravity + " vs " + vanilla);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void servoWhineIsCancelledBySilentActuatorsTierFour(GameTestHelper helper) {
        ServerPlayer loud = playerWith(helper, new Fit(BodyRegion.SKELETON, 0, ImplantIds.MICRO_ACTUATORS, 2));
        loud.setSprinting(true);
        helper.assertTrue(Limits.servoWhine(loud), "a sprinting Tier II Micro-Actuators user whines");
        loud.setSprinting(false);
        helper.assertTrue(!Limits.servoWhine(loud), "walking is quiet");

        ServerPlayer tier4 = playerWith(helper, new Fit(BodyRegion.SKELETON, 0, ImplantIds.MICRO_ACTUATORS, 2),
                new Fit(BodyRegion.LEGS, 0, ImplantIds.SILENT_ACTUATORS, 4));
        tier4.setSprinting(true);
        helper.assertTrue(!Limits.servoWhine(tier4), "Silent Actuators Tier IV cancels the whine");

        ServerPlayer smooth = playerWith(helper, new Fit(BodyRegion.SKELETON, 0, ImplantIds.MICRO_ACTUATORS, 4));
        smooth.setSprinting(true);
        helper.assertTrue(!Limits.servoWhine(smooth), "Micro-Actuators Tier IV no longer whines");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fallsFeedTheMomentumBooster(GameTestHelper helper) {
        ServerPlayer tier1 = playerWith(helper, new Fit(BodyRegion.LEGS, 0, ImplantIds.MOMENTUM_BOOSTER, 1));
        helper.assertTrue(Synergies.fallMomentum(tier1, 10.0) == 0.0, "Tier I has only the sprint source");

        ServerPlayer tier2 = playerWith(helper, new Fit(BodyRegion.LEGS, 0, ImplantIds.MOMENTUM_BOOSTER, 2));
        double plain = Synergies.fallMomentum(tier2, 10.0);
        helper.assertTrue(Math.abs(plain - 7 * Synergies.FALL_MOMENTUM_PER_BLOCK) < 1.0E-9, "7 blocks beyond the safe fall, got " + plain);

        ServerPlayer dampened = playerWith(helper, new Fit(BodyRegion.LEGS, 0, ImplantIds.MOMENTUM_BOOSTER, 2),
                new Fit(BodyRegion.SKELETON, 0, ImplantIds.IMPACT_DAMPENERS, 2));
        helper.assertTrue(Synergies.fallMomentum(dampened, 10.0) > plain, "Impact Dampeners store more of the landing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aesEvasionOpensAKerenzikovWindow(GameTestHelper helper) {
        ServerPlayer player = playerWith(helper, new Fit(BodyRegion.CORTEX, 0, ImplantIds.AES, 1),
                new Fit(BodyRegion.CORTEX, 1, ImplantIds.KERENZIKOV, 1));
        helper.assertTrue(!Kerenzikov.windowActive(player), "no window before the evasion");
        Synergies.aesEvaded(player);
        helper.assertTrue(Kerenzikov.windowActive(player), "the evasion opened a window");
        helper.succeed();
    }

    private Layer4Tests() {
    }
}
