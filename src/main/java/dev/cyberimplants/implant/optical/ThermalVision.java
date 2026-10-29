package dev.cyberimplants.implant.optical;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Thermal Vision: a toggle that drains 1 C/s. The overlay itself is rendered client-side (see ClientOptics). */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class ThermalVision {
    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            if (Toggles.isOn(player, ImplantIds.THERMAL_VISION)) {
                Toggles.set(player, ImplantIds.THERMAL_VISION, false);
                return UseResult.OK;
            }
            if (!Body.canAfford(player, Stats.of(player, ImplantIds.THERMAL_VISION).value("upkeep_per_second", 1.0))) {
                return UseResult.NO_CHARGE;
            }
            Toggles.set(player, ImplantIds.THERMAL_VISION, true);
            return UseResult.OK;
        }

        @Override
        public boolean isActive(ServerPlayer player) {
            return Toggles.isOn(player, ImplantIds.THERMAL_VISION);
        }

        @Override
        public boolean stop(ServerPlayer player) {
            if (!isActive(player)) {
                return false;
            }
            Toggles.set(player, ImplantIds.THERMAL_VISION, false);
            return true;
        }
    };

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0
                || !Toggles.isOn(player, ImplantIds.THERMAL_VISION)) {
            return;
        }
        double upkeep = Stats.of(ImplantIds.THERMAL_VISION, BodyState.get(player).tierOf(ImplantIds.THERMAL_VISION))
                .value("upkeep_per_second", 1.0);
        if (!Body.spend(player, upkeep)) {
            Toggles.set(player, ImplantIds.THERMAL_VISION, false);
        }
    }

    private ThermalVision() {
    }
}
