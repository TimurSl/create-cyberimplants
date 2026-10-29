package dev.cyberimplants.implant.circulatory;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Map;

/**
 * Emergency Defibrillator: cheats death once per long cooldown. Doesn't trigger on damage that bypasses invulnerability
 * (void, /kill), and fails if the battery plus the Energy Buffer can't pay for it. A held Totem of Undying is
 * consumed by vanilla before death is announced, so the totem naturally takes priority.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Defibrillator {
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.DEFIBRILLATOR);
        if (tier == 0 || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !Body.ready(player, ImplantIds.DEFIBRILLATOR)) {
            return;
        }
        TierStats stats = Stats.of(ImplantIds.DEFIBRILLATOR, tier);
        if (!Body.spendEmergency(player, stats.chargeCost())) {
            return;
        }
        event.setCanceled(true);
        player.setHealth((float) stats.value("health_restored", 2.0));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) stats.value("resistance_ticks", 40),
                (int) stats.value("resistance_amplifier", 0)));
        ImplantFx.defib(player);
        Body.startCooldown(player, ImplantIds.DEFIBRILLATOR, stats.cooldownTicks());
        DeckEvents.fire(player, "defib_fired", Map.of());
    }

    private Defibrillator() {
    }
}
