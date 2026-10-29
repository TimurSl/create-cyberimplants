package dev.cyberimplants.gametest;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.subdermal.EmergencyShell;
import dev.cyberimplants.implant.subdermal.KineticShield;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Layer 3 GameTests for the tier-gated unlock features implemented so far. */
@GameTestHolder(CyberImplants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Layer3Tests {
    private static ServerPlayer playerWith(GameTestHelper helper, BodyRegion region, ResourceLocation type, int tier) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
        BodyState body = BodyState.EMPTY
                .withImplant(new SlotId(BodyRegion.CORE, 0), new InstalledImplant(ImplantIds.CORE, 5))
                .withImplant(new SlotId(region, 0), new InstalledImplant(type, tier))
                .withCharge(200, 0);
        BodyState.set(player, body);
        return player;
    }

    @GameTest(template = "empty")
    public static void unlockRowsFollowTheTier(GameTestHelper helper) {
        helper.assertTrue(!Stats.unlocked(ImplantIds.REGENERATOR, 1, "works_when_starving"), "Tier I has no unlocks");
        helper.assertTrue(Stats.unlocked(ImplantIds.REGENERATOR, 2, "works_when_starving"), "Tier II works when starving");
        helper.assertTrue(!Stats.unlocked(ImplantIds.REGENERATOR, 2, "cures_poison"), "Tier II does not cure poison yet");
        helper.assertTrue(Stats.unlocked(ImplantIds.REGENERATOR, 4, "cures_poison") && Stats.unlocked(ImplantIds.REGENERATOR, 4, "cures_wither_hunger"),
                "Tier IV keeps the lower unlocks and adds its own");
        helper.assertTrue(!Stats.unlocked(ImplantIds.REGENERATOR, 0, "cures_poison"), "an implant that is not installed unlocks nothing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void kineticShieldAbsorbsProjectilesFullyFromTierThree(GameTestHelper helper) {
        Arrow arrow = helper.spawn(EntityType.ARROW, 3, 2, 3);
        for (int tier = 2; tier <= 3; tier++) {
            ServerPlayer player = playerWith(helper, BodyRegion.INTEGUMENT, ImplantIds.KINETIC_SHIELD, tier);
            float left = KineticShield.absorb(player, player.damageSources().arrow(arrow, null), 2.0f);
            if (tier == 2) {
                helper.assertTrue(Math.abs(left - 1.0f) < 1.0E-4f, "Tier II absorbs half of a projectile, " + left + " left");
            } else {
                helper.assertTrue(left < 1.0E-4f, "Tier III absorbs the whole projectile, " + left + " left");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emergencyShellKnockbackImmunityAndCleanse(GameTestHelper helper) {
        ServerPlayer tier3 = playerWith(helper, BodyRegion.INTEGUMENT, ImplantIds.EMERGENCY_SHELL, 3);
        helper.assertTrue(EmergencyShell.trigger(tier3), "the Tier III Shell triggers");
        helper.assertTrue(tier3.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0, "knockback immunity while active");

        ServerPlayer tier4 = playerWith(helper, BodyRegion.INTEGUMENT, ImplantIds.EMERGENCY_SHELL, 4);
        tier4.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        tier4.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
        tier4.setRemainingFireTicks(100);
        helper.assertTrue(EmergencyShell.trigger(tier4), "the Tier IV Shell triggers");
        helper.assertTrue(!tier4.hasEffect(MobEffects.POISON), "harmful effects are cleansed");
        helper.assertTrue(tier4.hasEffect(MobEffects.MOVEMENT_SPEED), "beneficial effects stay");
        helper.assertTrue(!tier4.isOnFire(), "the fire is out");
        helper.succeed();
    }

    private Layer3Tests() {
    }
}
