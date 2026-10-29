package dev.cyberimplants.implant.skeletal;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.deck.EntityHandle;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Power Actuators: the attack damage bonus (and its unarmed doubling) is an attribute modifier; a fully charged hit
 * becomes a Power Strike with extra knockback, paid for with Charge. Without Charge the hit lands normally.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class PowerActuators {
    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.POWER_ACTUATORS);
        if (tier == 0) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.POWER_ACTUATORS, tier);
        boolean fullyCharged = player.getAttackStrengthScale(0.5f) >= 0.9f;
        if (fullyCharged && Body.spend(player, stats.chargeCost())) {
            PENDING.add(player.getUUID());
        } else {
            PENDING.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player
                || !PENDING.remove(player.getUUID())) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.POWER_ACTUATORS);
        if (tier == 0) {
            return;
        }
        LivingEntity target = event.getEntity();
        double extra = Stats.of(ImplantIds.POWER_ACTUATORS, tier).value("knockback", 0.2) * 0.4;
        float yaw = player.getYRot() * ((float) Math.PI / 180f);
        target.knockback(extra, Mth.sin(yaw), -Mth.cos(yaw));
        ImplantFx.powerStrike(player, target);
        DeckEvents.fire(player, "power_strike", Map.of("target", EntityHandle.of(player, target)));
    }

    private PowerActuators() {
    }
}
