package dev.cyberimplants.implant;

import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

import static dev.cyberimplants.implant.ImplantIds.*;

/**
 * Passive implants that only change attributes (docs/design/08-technical-architecture.md, section 8.5). Re-applied
 * whenever the body changes and every few ticks, because hand contents decide the unarmed and blade bonuses.
 */
public final class PassiveModifiers {
    public static void apply(ServerPlayer player) {
        BodyState s = BodyState.get(player);

        // A. Reflex Booster: action speed, a little walking speed. Item-use speed is handled by ReflexBooster.
        int reflex = s.tierOf(REFLEX_BOOSTER);
        Attrs.multiplied(player, Attributes.ATTACK_SPEED, "reflex_attack_speed",
                Stats.of(REFLEX_BOOSTER, Math.max(reflex, 1)).value("attack_speed", 0.05), reflex > 0);
        Attrs.multiplied(player, Attributes.MOVEMENT_SPEED, "reflex_walk_speed",
                Stats.of(REFLEX_BOOSTER, Math.max(reflex, 1)).value("walk_speed", 0.02), reflex > 0);

        boolean handEmpty = player.getMainHandItem().isEmpty();

        // C. Skeletal & muscular
        int power = s.tierOf(POWER_ACTUATORS);
        double bonus = Stats.of(POWER_ACTUATORS, Math.max(power, 1)).value("attack_damage", 1.0);
        double unarmedMultiplier = Stats.of(POWER_ACTUATORS, Math.max(power, 1)).value("unarmed_multiplier", 2.0);
        Attrs.added(player, Attributes.ATTACK_DAMAGE, "power_actuators_damage", handEmpty ? bonus * unarmedMultiplier : bonus, power > 0);

        int skeleton = s.tierOf(REINFORCED_SKELETON);
        Attrs.added(player, Attributes.MAX_HEALTH, "skeleton_health",
                Stats.of(REINFORCED_SKELETON, Math.max(skeleton, 1)).value("max_health", 0.0), skeleton > 0);

        // Layer 4 limit: the Dense frame is heavy (+5% gravity, so the user also sinks in water).
        Attrs.multiplied(player, Attributes.GRAVITY, "dense_frame_gravity", Limits.DENSE_FRAME_GRAVITY, Limits.denseFrame(s));

        int tendons = s.tierOf(KINETIC_TENDONS);
        Attrs.added(player, Attributes.JUMP_STRENGTH, "kinetic_tendons_jump",
                Stats.of(KINETIC_TENDONS, Math.max(tendons, 1)).value("jump_strength", 0.042), tendons > 0);

        int micro = s.tierOf(MICRO_ACTUATORS);
        Attrs.multiplied(player, Attributes.MOVEMENT_SPEED, "micro_actuators_sprint",
                Stats.of(MICRO_ACTUATORS, Math.max(micro, 1)).value("sprint_speed", 0.05), micro > 0);

        int dampeners = s.tierOf(IMPACT_DAMPENERS);
        TierDamp d = new TierDamp(Stats.of(IMPACT_DAMPENERS, Math.max(dampeners, 1)).value("safe_fall", 2.0),
                Stats.of(IMPACT_DAMPENERS, Math.max(dampeners, 1)).value("fall_reduction", 0.15));
        Attrs.added(player, Attributes.SAFE_FALL_DISTANCE, "impact_dampeners_safe_fall", d.safeFall(), dampeners > 0);
        Attrs.added(player, Attributes.FALL_DAMAGE_MULTIPLIER, "impact_dampeners_fall_damage", -d.reduction(), dampeners > 0);

        int inertia = s.tierOf(INERTIA_COMPENSATION);
        Attrs.added(player, Attributes.KNOCKBACK_RESISTANCE, "inertia_knockback",
                Stats.of(INERTIA_COMPENSATION, Math.max(inertia, 1)).value("knockback_resistance", 0.10), inertia > 0);

        // D. Arms: the frame in the main-hand arm speeds up mining; an empty hand with a deployed blade becomes a weapon.
        Arms.Side main = Arms.mainSide(player);
        int frame = s.installed().containsKey(new dev.cyberimplants.body.SlotId(Arms.frameRegion(main), 0))
                ? s.installed().get(new dev.cyberimplants.body.SlotId(Arms.frameRegion(main), 0)).tier() : 0;
        var frameStats = Stats.of(CYBERNETIC_ARM, Math.max(frame, 1));
        double bilateral = Arms.bilateral(s) ? frameStats.value("bilateral_break_speed", 0.0) : 0.0;
        Attrs.multiplied(player, Attributes.BLOCK_BREAK_SPEED, "arm_break_speed",
                frameStats.value("break_speed", 0.10) + bilateral, frame > 0);
        Attrs.added(player, Attributes.BLOCK_INTERACTION_RANGE, "arm_reach", frameStats.value("reach", 0.0), frame > 0);

        int bladeTier = Arms.moduleTier(s, main, RETRACTABLE_BLADE);
        boolean bladeOut = bladeTier > 0 && handEmpty && Toggles.isOn(player, RETRACTABLE_BLADE);
        var blade = Stats.of(RETRACTABLE_BLADE, Math.max(bladeTier, 1));
        // The player's base attack damage is 1 and base attack speed 4.
        Attrs.added(player, Attributes.ATTACK_DAMAGE, "blade_damage", blade.value("damage", 6.0) - 1.0, bladeOut);
        Attrs.added(player, Attributes.ATTACK_SPEED, "blade_speed", blade.value("attack_speed", 1.8) - 4.0, bladeOut);

        // G. Armor Plating
        int plating = s.tierOf(ARMOR_PLATING);
        var platingStats = Stats.of(ARMOR_PLATING, Math.max(plating, 1));
        Attrs.added(player, Attributes.ARMOR, "plating_armor", platingStats.value("armor", 2.0), plating > 0);
        Attrs.added(player, Attributes.ARMOR_TOUGHNESS, "plating_toughness", platingStats.value("toughness", 0.0), plating > 0);
        Attrs.multiplied(player, Attributes.MOVEMENT_SPEED, "plating_weight", platingStats.value("weight", 0.0), plating > 0);
    }

    private record TierDamp(double safeFall, double reduction) {
    }

    private PassiveModifiers() {
    }
}
