package dev.cyberimplants.combat;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Tags;
import dev.cyberimplants.implant.subdermal.EmergencyShell;
import dev.cyberimplants.implant.subdermal.KineticShield;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Mitigation order (docs/design/implants/g-subdermal.md): Nanocoating, Reinforced Skeleton / Inertial Stabilizer,
 * then vanilla armor, then Emergency Shell, then Kinetic Shield. The first group runs at the incoming-damage stage,
 * the rest after armor.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class DamagePipeline {
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        BodyState body = BodyState.get(player);
        if (body.installed().isEmpty()) {
            return;
        }
        DamageSource source = event.getSource();
        float amount = event.getAmount();

        // 1. Nanocoating: Layer 1 knows only the balanced formula, a small cut against every listed family.
        int nano = body.tierOf(ImplantIds.NANOCOATING);
        if (nano > 0 && isNanoFamily(source)) {
            amount *= (float) (1.0 - Stats.of(ImplantIds.NANOCOATING, nano).value("balanced", 0.04));
        }

        // 2. Reinforced Skeleton: flat reduction per blunt hit, percentage against machinery.
        int skeleton = body.tierOf(ImplantIds.REINFORCED_SKELETON);
        if (skeleton > 0) {
            var stats = Stats.of(ImplantIds.REINFORCED_SKELETON, skeleton);
            if (source.is(Tags.BLUNT)) {
                amount = Math.max(0.0f, amount - (float) stats.value("blunt_flat", 0.5));
            }
            if (source.is(Tags.MACHINERY)) {
                amount *= (float) (1.0 - stats.value("machinery", 0.20));
            }
        }

        // Inertial Stabilizer: surviving wall impacts at speed.
        int stabilizer = body.tierOf(ImplantIds.INERTIAL_STABILIZER);
        if (stabilizer > 0 && source.is(DamageTypes.FLY_INTO_WALL)) {
            amount *= (float) (1.0 - Stats.of(ImplantIds.INERTIAL_STABILIZER, stabilizer).value("wall_impact_reduction", 0.20));
        }

        if (amount != event.getAmount()) {
            event.setAmount(amount);
        }
    }

    @SubscribeEvent
    public static void onPreDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        float damage = event.getNewDamage();
        // 4. Emergency Shell, 5. Kinetic Shield (post-armor, like absorption hearts).
        damage = EmergencyShell.reduce(player, damage);
        damage = KineticShield.absorb(player, event.getSource(), damage);
        if (damage != event.getNewDamage()) {
            event.setNewDamage(damage);
        }
    }

    private static boolean isNanoFamily(DamageSource source) {
        return source.is(Tags.NANO_THERMAL) || source.is(Tags.NANO_KINETIC)
                || source.is(Tags.NANO_BLAST) || source.is(Tags.NANO_CHEMICAL);
    }

    /** The Nanocoating family of a damage source, or {@code "other"}; exposed to scripts on {@code damaged}. */
    public static String family(DamageSource source) {
        if (source.is(Tags.NANO_THERMAL)) {
            return "thermal";
        }
        if (source.is(Tags.NANO_KINETIC)) {
            return "kinetic";
        }
        if (source.is(Tags.NANO_BLAST)) {
            return "blast";
        }
        if (source.is(Tags.NANO_CHEMICAL)) {
            return "chemical";
        }
        return "other";
    }

    private DamagePipeline() {
    }
}
