package dev.cyberimplants.compat.create.block;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Kinetic block entity of the Implantation Rig. Layer 1 only needs its shaft speed (nonzero = operational). */
public class RigBlockEntity extends KineticBlockEntity {
    /** Impact in SU per RPM (docs/design/05-create-integration.md, section 5.1). */
    public static final float STRESS_IMPACT = 8.0f;
    public static final double MAX_USE_DISTANCE = 6.0;

    public RigBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = STRESS_IMPACT;
        return STRESS_IMPACT;
    }

    /** Layer 1: any rotation at all lets the Rig operate; RPM gates per tier arrive with Layer 2. */
    public boolean operational() {
        return Math.abs(getSpeed()) > 0f && !isOverStressed();
    }

    /** True when the player is close enough to this Rig and it is running. */
    public static boolean canOperate(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_USE_DISTANCE * MAX_USE_DISTANCE) {
            return false;
        }
        return player.level().getBlockEntity(pos) instanceof RigBlockEntity rig && rig.operational();
    }
}
