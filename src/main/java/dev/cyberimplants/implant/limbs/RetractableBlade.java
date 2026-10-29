package dev.cyberimplants.implant.limbs;

import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.implant.Arms;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.PassiveModifiers;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Retractable Blade: deploy toggle. The module may sit in either arm's socket. While deployed and the main hand is empty,
 * PassiveModifiers turns the hand into a 6-damage weapon; the unarmed Power Actuators bonus applies on top because the
 * hand is still empty. Blades in both arms swing faster while both hands are empty.
 */
public final class RetractableBlade {
    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            boolean next = !Toggles.isOn(player, ImplantIds.RETRACTABLE_BLADE);
            Toggles.set(player, ImplantIds.RETRACTABLE_BLADE, next);
            PassiveModifiers.apply(player);
            ImplantFx.blade(player, next);
            if (next) {
                double damage = Stats.of(ImplantIds.RETRACTABLE_BLADE, Arms.anyModuleTier(BodyState.get(player), ImplantIds.RETRACTABLE_BLADE))
                        .value("damage", 6.0);
                player.displayClientMessage(player.getMainHandItem().isEmpty()
                        ? Component.translatable("cyberimplants.blade.deployed", Math.round(damage))
                        : Component.translatable("cyberimplants.blade.hand_full"), true);
            } else {
                player.displayClientMessage(Component.translatable("cyberimplants.blade.retracted"), true);
            }
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
