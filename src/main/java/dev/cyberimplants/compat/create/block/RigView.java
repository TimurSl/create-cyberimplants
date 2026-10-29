package dev.cyberimplants.compat.create.block;

import dev.cyberimplants.body.SlotId;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * What a client knows about an Implantation Rig: read by the Rig screen, the HUD and the renderer without touching
 * Create classes.
 *
 * @param rpm          shaft speed (absolute)
 * @param overStressed the network is overstressed
 * @param locked       a redstone signal locks the chair
 * @param gel          gel in the tank, in mB
 * @param neuralGel    the tank holds Neural Gel (otherwise Bio-Interface Gel, or nothing)
 * @param operation    the running operation, or null
 */
public record RigView(float rpm, boolean overStressed, boolean locked, int gel, boolean neuralGel, Operation operation) {
    public static final RigView NONE = new RigView(0, false, false, 0, false, null);

    /** A running operation. {@code elapsed} and {@code total} are in ticks. */
    public record Operation(UUID patient, boolean install, SlotId slot, int tier, ResourceLocation type, int elapsed, int total) {
        public float progress(float partialTick) {
            return Math.min(1f, (elapsed + partialTick) / Math.max(1, total));
        }

        public int secondsLeft() {
            return Math.max(0, (total - elapsed + 19) / 20);
        }
    }

    public boolean powered() {
        return rpm > 0 && !overStressed;
    }

    public int gelCapacity() {
        return RigBlockEntity.TANK_CAPACITY;
    }
}
