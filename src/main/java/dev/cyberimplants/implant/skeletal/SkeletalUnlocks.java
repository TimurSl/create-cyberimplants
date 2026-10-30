package dev.cyberimplants.implant.skeletal;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;

/**
 * Tier V rows (Layer 5) of the passive skeletal implants, which have no behaviour class of their own: the Reinforced
 * Skeleton turns incoming critical hits into normal ones, Inertia Compensation halves explosion knockback.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class SkeletalUnlocks {
    private static final double BLAST_KNOCKBACK_KEPT = 0.5;

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (event.isCriticalHit() && event.getTarget() instanceof Player target
                && Stats.unlocked(target, ImplantIds.REINFORCED_SKELETON, "crit_immune")) {
            event.setCriticalHit(false);
        }
    }

    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        if (event.getAffectedEntity() instanceof Player player
                && Stats.unlocked(player, ImplantIds.INERTIA_COMPENSATION, "blast_resistant")) {
            event.setKnockbackVelocity(event.getKnockbackVelocity().scale(BLAST_KNOCKBACK_KEPT));
        }
    }

    private SkeletalUnlocks() {
    }
}
