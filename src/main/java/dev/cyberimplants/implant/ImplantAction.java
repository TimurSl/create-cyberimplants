package dev.cyberimplants.implant;

import net.minecraft.server.level.ServerPlayer;

/** Behaviour of an Active or Toggle implant: what happens when the player (or a deck, or the ERS) uses it. */
public interface ImplantAction {
    /** Uses the implant. The caller has already checked that it is installed and that scripts may use it. */
    UseResult use(ServerPlayer player, UseContext context);

    /** Whether the implant is currently running (a field, a toggle, camouflage). */
    default boolean isActive(ServerPlayer player) {
        return false;
    }

    /** Stops a running implant. Returns false if it was not running or cannot be stopped manually. */
    default boolean stop(ServerPlayer player) {
        return false;
    }
}
