package dev.cyberimplants.client;

import dev.cyberimplants.compat.create.block.RigBlockEntity;
import dev.cyberimplants.compat.create.block.RigView;
import dev.cyberimplants.content.RigSeatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

/** The local player's side of the Implantation Rig: input lock while strapped in, and the Rig they are strapped into. */
public final class ClientRig {
    /** True while the local player sits in a locked Rig seat. */
    public static boolean restrained() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && RigSeatEntity.restrained(player);
    }

    /** The Rig the local player is strapped into, or null. */
    public static BlockPos strappedInto() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getVehicle() instanceof RigSeatEntity seat ? seat.rigPos() : null;
    }

    /** The client's view of the Rig at {@code pos}, or {@link RigView#NONE}. */
    public static RigView view(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (pos != null && mc.level != null && mc.level.getBlockEntity(pos) instanceof RigBlockEntity rig) {
            return rig.view();
        }
        return RigView.NONE;
    }

    /** A strapped-in patient cannot walk, jump or sneak (sneaking would ask the server to stand up). */
    static void onMovementInput(Input input) {
        if (!restrained()) {
            return;
        }
        input.leftImpulse = 0;
        input.forwardImpulse = 0;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    private ClientRig() {
    }
}
