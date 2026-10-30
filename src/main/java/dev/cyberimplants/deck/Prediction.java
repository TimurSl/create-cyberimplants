package dev.cyberimplants.deck;

import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.synapse.IsaLevel;
import dev.cyberimplants.synapse.compiler.Ticks;
import dev.cyberimplants.synapse.vm.Handle;
import dev.cyberimplants.synapse.vm.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * The Combat Predictor's script methods (docs/design/implants/a-neural.md): {@code predict.position(entity, seconds)}
 * extrapolates the entity's motion up to the predictor's horizon (horizons over 1 s need ISA-5), and
 * {@code predict.windup(entity)} reports a creeper fuse or a skeleton's draw as {@code {attack, eta}}, otherwise NULL.
 */
final class Prediction {
    /** A skeleton releases its arrow after drawing for this many ticks. */
    private static final int BOW_DRAW_TICKS = 20;
    /** A creeper's fuse in ticks. */
    private static final int CREEPER_FUSE_TICKS = 30;

    static Object invoke(ServerPlayer player, int tier, String method, List<Object> args, IsaLevel isa) {
        return switch (method) {
            case "position" -> {
                if (args.size() != 2) {
                    throw new IllegalArgumentException("predict.position takes an entity and a time in seconds");
                }
                Entity e = entity(args.get(0));
                double seconds = seconds(args.get(1));
                if (seconds > 1.0 && !isa.atLeast(IsaLevel.ISA_5)) {
                    throw new IllegalArgumentException("predict horizons over 1 s require ISA-5");
                }
                double horizon = Stats.of(ImplantIds.COMBAT_PREDICTOR, tier).value("horizon_ticks", 10) / 20.0;
                yield e == null ? null : position(e, Math.max(0.0, Math.min(seconds, horizon)) * 20.0);
            }
            case "windup" -> {
                if (args.size() != 1) {
                    throw new IllegalArgumentException("predict.windup takes an entity");
                }
                Entity e = entity(args.get(0));
                yield e == null ? null : windup(e);
            }
            default -> throw new UnsupportedOperationException(method);
        };
    }

    /** Where the entity will be after {@code ticks}, from its current motion; grounded entities keep their height. */
    static Position position(Entity e, double ticks) {
        Vec3 velocity = e instanceof Player
                ? new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo) // players move client-side
                : e.getDeltaMovement();
        if (e.onGround()) {
            velocity = new Vec3(velocity.x, 0, velocity.z);
        }
        Vec3 at = e.position().add(velocity.scale(ticks));
        return new Position(at.x, at.y, at.z);
    }

    private static Object windup(Entity e) {
        if (e instanceof Creeper creeper && creeper.getSwellDir() > 0) {
            double eta = (1.0 - creeper.getSwelling(1.0f)) * CREEPER_FUSE_TICKS;
            return Handle.ofMap(Map.of("attack", "fuse", "eta", new Ticks(Math.max(0, Math.round(eta)))));
        }
        if (e instanceof AbstractSkeleton skeleton && skeleton.isUsingItem()) {
            int eta = Math.max(0, BOW_DRAW_TICKS - skeleton.getTicksUsingItem());
            return Handle.ofMap(Map.of("attack", "draw", "eta", new Ticks(eta)));
        }
        return null;
    }

    private static Entity entity(Object v) {
        if (v instanceof EntityHandle h) {
            return h.resolve();
        }
        if (v instanceof SelfHandle self) {
            return self.player();
        }
        throw new IllegalArgumentException("predict needs an entity");
    }

    private static double seconds(Object v) {
        if (v instanceof Double d) {
            return d;
        }
        if (v instanceof Ticks t) {
            return t.ticks() / 20.0;
        }
        throw new IllegalArgumentException("predict needs a time in seconds or a duration");
    }

    private Prediction() {
    }
}
