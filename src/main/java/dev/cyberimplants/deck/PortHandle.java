package dev.cyberimplants.deck;

import dev.cyberimplants.compat.create.CreateCompat;
import dev.cyberimplants.content.NeuralPortBlock;
import dev.cyberimplants.content.NeuralPortBlockEntity;
import dev.cyberimplants.registry.ModAttachments;
import dev.cyberimplants.synapse.vm.Handle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * A script's view of a bound Neural Port ({@code DEVICE press = port "Press Line A"}). It resolves the binding on
 * every read, so it reads {@code NULL} (and {@code online} is false) when the port is unbound, unloaded, out of
 * range or in another dimension. Layer 1 members: {@code speed}, {@code abs_speed}, {@code online}.
 */
final class PortHandle implements Handle {
    private final ServerPlayer player;
    private final String name;

    PortHandle(ServerPlayer player, String name) {
        this.player = player;
        this.name = name;
    }

    String name() {
        return name;
    }

    /** The block the port reads, or null if the port is offline. */
    private BlockPos target() {
        GlobalPos bound = player.getData(ModAttachments.DECK_DATA).ports().get(name);
        if (bound == null || !bound.dimension().equals(player.level().dimension())) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = bound.pos();
        if (!level.isLoaded(pos) || player.blockPosition().distSqr(pos) > DeckBindings.DIRECT_RANGE * DeckBindings.DIRECT_RANGE) {
            return null;
        }
        if (!(level.getBlockEntity(pos) instanceof NeuralPortBlockEntity)) {
            return null;
        }
        return NeuralPortBlock.target(pos, level.getBlockState(pos));
    }

    @Override
    public Object get(String member) {
        BlockPos target = target();
        if (member.equals("online")) {
            return target != null;
        }
        if (target == null) {
            return null;
        }
        OptionalDouble speed = CreateCompat.kinetic().speed(player.level(), target);
        return switch (member) {
            case "speed" -> speed.isPresent() ? speed.getAsDouble() : null;
            case "abs_speed" -> speed.isPresent() ? Math.abs(speed.getAsDouble()) : null;
            default -> null;
        };
    }

    @Override
    public int readCost() {
        return 4; // device property read, cached per tick in later layers
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PortHandle h && h.name.equals(name) && h.player.getUUID().equals(player.getUUID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, player.getUUID());
    }
}
