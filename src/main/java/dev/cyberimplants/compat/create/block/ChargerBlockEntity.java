package dev.cyberimplants.compat.create.block;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.cyberimplants.body.Body;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Charges the Core battery of players standing on the pad at {@code |RPM| / 8} C per second. */
public class ChargerBlockEntity extends KineticBlockEntity {
    public static final float STRESS_IMPACT = 4.0f;
    public static final float MIN_RPM = 16f;

    public ChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = STRESS_IMPACT;
        return STRESS_IMPACT;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        float rpm = Math.abs(getSpeed());
        if (rpm < MIN_RPM || isOverStressed()) {
            return;
        }
        AABB pad = new AABB(worldPosition.getX(), worldPosition.getY() + 0.25, worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 2.0, worldPosition.getZ() + 1);
        double perTick = rpm / 8.0 / 20.0;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, pad)) {
            Body.addCharge(player, perTick);
        }
    }
}
