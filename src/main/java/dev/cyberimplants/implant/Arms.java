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
        return moduleTier(s, side, module) > 0;
    }

    /** Tier of the module in any of the given arm's sockets (1-3 depending on the frame), or 0. */
    public static int moduleTier(BodyState s, Side side, ResourceLocation module) {
        BodyRegion region = socketRegion(side);
        for (int i = 0; i < region.slots(); i++) {
            var installed = s.installed().get(new SlotId(region, i));
            if (installed != null && installed.type().equals(module)) {
                return installed.tier();
            }
        }
        return 0;
    }

    /** True when both arms carry a frame of the same tier (the bilateral bonus). */
    public static boolean bilateral(BodyState s) {
        var left = s.installed().get(new SlotId(BodyRegion.LEFT_ARM, 0));
        var right = s.installed().get(new SlotId(BodyRegion.RIGHT_ARM, 0));
        return left != null && right != null && left.tier() == right.tier();
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
