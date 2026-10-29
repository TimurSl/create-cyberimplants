package dev.cyberimplants.implant.neural;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Reflex Booster: attack speed and walk speed are attribute modifiers (see PassiveModifiers); this class speeds up
 * item use (bows, crossbows, food, potions) by finishing a share of use ticks early.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class ReflexBooster {
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.REFLEX_BOOSTER);
        if (tier == 0) {
            return;
        }
        double rate = Stats.of(ImplantIds.REFLEX_BOOSTER, tier).value("item_use_speed", 0.10);
        // Deterministic on both sides: skip one extra tick whenever the running total crosses a whole number.
        int t = player.tickCount;
        if (Math.floor((t + 1) * rate) > Math.floor(t * rate) && event.getDuration() > 1) {
            event.setDuration(event.getDuration() - 1);
        }
    }

    private ReflexBooster() {
    }
}
