package dev.cyberimplants.implant.circulatory;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.combat.DamagePipeline;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.deck.EntityHandle;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Map;

/** Biomonitor: the data source for health-related deck events. The vitals HUD is client-side. */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Biomonitor {
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !BodyState.get(player).has(ImplantIds.BIOMONITOR)) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", (double) event.getNewDamage());
        payload.put("type", source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("unknown"));
        payload.put("family", DamagePipeline.family(source));
        payload.put("attacker", attacker == null ? null : EntityHandle.of(player, attacker));
        DeckEvents.fire(player, "damaged", payload);
    }

    private Biomonitor() {
    }
}
