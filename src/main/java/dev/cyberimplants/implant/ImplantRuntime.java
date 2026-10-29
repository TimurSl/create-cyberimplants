package dev.cyberimplants.implant;

import dev.cyberimplants.body.BodyEvents;
import dev.cyberimplants.deck.DeckService;
import dev.cyberimplants.implant.neural.Kerenzikov;
import dev.cyberimplants.implant.neural.Sandevistan;
import dev.cyberimplants.implant.optical.OpticalCamouflage;
import dev.cyberimplants.implant.optical.TargetTracking;
import dev.cyberimplants.implant.subdermal.KineticShield;
import net.minecraft.server.level.ServerPlayer;

/** Hooks that run whenever a player's installed hardware changes (and when they log in or respawn). */
public final class ImplantRuntime {
    public static void bodyChanged(ServerPlayer player) {
        Toggles.reconcile(player);
        PassiveModifiers.apply(player);
        Sandevistan.onBodyChanged(player);
        OpticalCamouflage.onBodyChanged(player);
        TargetTracking.onBodyChanged(player);
        Kerenzikov.onBodyChanged(player);
        KineticShield.onBodyChanged(player);
        DeckService.onBodyChanged(player);
        BodyEvents.syncBody(player);
    }

    private ImplantRuntime() {
    }
}
