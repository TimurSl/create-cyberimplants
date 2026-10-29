package dev.cyberimplants.implant.circulatory;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Energy Buffer: a reserve pool that only Emergency activations may draw on. It fills only when the battery is full. */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class EnergyBuffer {
    public static double capacity(Player player) {
        return Body.bufferCapacity(BodyState.get(player));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        BodyState s = BodyState.get(player);
        int tier = s.tierOf(ImplantIds.ENERGY_BUFFER);
        if (tier == 0 || !Body.chargeEnabled()) {
            return;
        }
        double cap = Body.bufferCapacity(s);
        if (s.buffer() >= cap || s.charge() < Body.maxCharge(s) - 0.01) {
            return;
        }
        double add = Math.min(cap - s.buffer(), Stats.of(ImplantIds.ENERGY_BUFFER, tier).value("fill_rate", 1.0) * 0.5);
        Body.updateQuiet(player, st -> st.withCharge(st.charge(), st.buffer() + add));
    }

    private EnergyBuffer() {
    }
}
