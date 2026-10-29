package dev.cyberimplants.implant.limbs;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.PassiveModifiers;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import net.minecraft.server.level.ServerPlayer;

/**
 * Retractable Blade: deploy toggle. While deployed and the main hand is empty, PassiveModifiers turns the hand into
 * a 6-damage weapon; the unarmed Power Actuators bonus applies on top because the hand is still empty.
 */
public final class RetractableBlade {
    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            boolean next = !Toggles.isOn(player, ImplantIds.RETRACTABLE_BLADE);
            Toggles.set(player, ImplantIds.RETRACTABLE_BLADE, next);
            PassiveModifiers.apply(player);
            ImplantFx.blade(player, next);
            return UseResult.OK;
        }

        @Override
        public boolean isActive(ServerPlayer player) {
            return Toggles.isOn(player, ImplantIds.RETRACTABLE_BLADE);
        }

        @Override
        public boolean stop(ServerPlayer player) {
            if (!isActive(player)) {
                return false;
            }
            Toggles.set(player, ImplantIds.RETRACTABLE_BLADE, false);
            PassiveModifiers.apply(player);
            return true;
        }
    };

    private RetractableBlade() {
    }
}
