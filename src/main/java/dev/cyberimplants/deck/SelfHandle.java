package dev.cyberimplants.deck;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.synapse.vm.Handle;
import dev.cyberimplants.synapse.vm.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** {@code self.*}: the user's own core state and proprioception. */
final class SelfHandle implements Handle {
    private final ServerPlayer player;

    SelfHandle(ServerPlayer player) {
        this.player = player;
    }

    ServerPlayer player() {
        return player;
    }

    @Override
    public Object get(String member) {
        BodyState s = BodyState.get(player);
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();
        return switch (member) {
            case "charge" -> s.charge();
            case "charge_max" -> Body.maxCharge(s);
            case "load" -> (double) Body.totalLoad(s);
            case "capacity" -> Body.capacity(s);
            case "strain" -> "stable";
            case "position" -> new Position(player.getX(), player.getY(), player.getZ());
            case "facing" -> new Position(look.x, look.y, look.z);
            case "velocity" -> new Position(motion.x, motion.y, motion.z);
            case "on_ground" -> player.onGround();
            case "sprinting" -> player.isSprinting();
            case "dimension" -> player.level().dimension().location().toString();
            default -> null;
        };
    }
}
