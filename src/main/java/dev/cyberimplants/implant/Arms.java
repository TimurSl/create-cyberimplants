package dev.cyberimplants.implant;

import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.SlotId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/** Maps the body's left/right arms to the player's hands (the right arm follows the main hand for right-handers). */
public final class Arms {
    public enum Side { LEFT, RIGHT }

    public static BodyRegion frameRegion(Side side) {
        return side == Side.LEFT ? BodyRegion.LEFT_ARM : BodyRegion.RIGHT_ARM;
    }

    public static BodyRegion socketRegion(Side side) {
        return side == Side.LEFT ? BodyRegion.LEFT_SOCKET : BodyRegion.RIGHT_SOCKET;
    }

    public static Side mainSide(Player player) {
        return player.getMainArm() == HumanoidArm.RIGHT ? Side.RIGHT : Side.LEFT;
    }

    public static InteractionHand handOf(Player player, Side side) {
        return side == mainSide(player) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static boolean hasFrame(BodyState s, Side side) {
        return s.installed().containsKey(new SlotId(frameRegion(side), 0));
    }

    public static boolean hasModule(BodyState s, Side side, ResourceLocation module) {
        var installed = s.installed().get(new SlotId(socketRegion(side), 0));
        return installed != null && installed.type().equals(module);
    }

    /** Tier of the module in the given arm's socket, or 0. */
    public static int moduleTier(BodyState s, Side side, ResourceLocation module) {
        var installed = s.installed().get(new SlotId(socketRegion(side), 0));
        return installed != null && installed.type().equals(module) ? installed.tier() : 0;
    }

    /** The first arm whose socket holds this module and whose hand is empty, or null. */
    public static Side freeHandWith(Player player, ResourceLocation module) {
        BodyState s = BodyState.get(player);
        for (Side side : Side.values()) {
            if (hasModule(s, side, module) && player.getItemInHand(handOf(player, side)).isEmpty()) {
                return side;
            }
        }
        return null;
    }

    private Arms() {
    }
}
