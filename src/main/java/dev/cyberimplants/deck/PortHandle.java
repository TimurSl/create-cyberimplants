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

    /** The machine block this port is plugged into, or null when the port is offline. */
    BlockPos targetPos() {
        return target();
    }

    /** The block the port reads, or null if the port is offline. */
    private BlockPos target() {
        GlobalPos bound = player.getData(ModAttachments.DECK_DATA).ports().get(name);
        if (bound == null || !bound.dimension().equals(player.level().dimension())) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = bound.pos();
        double range = DeckBindings.directRange(player);
        if (!level.isLoaded(pos) || player.blockPosition().distSqr(pos) > range * range) {
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
            case "overstressed" -> speed.isPresent() ? CreateCompat.kinetic().overstressed(player.level(), target) : null;
            case "stress_used" -> stress(target, true);
            case "stress_capacity" -> stress(target, false);
            case "redstone_in" -> (double) player.level().getBestNeighborSignal(target);
            case "redstone_out" -> portBlock() instanceof NeuralPortBlockEntity be ? (double) be.redstoneOut() : null;
            default -> null;
        };
    }

    private Double stress(BlockPos target, boolean used) {
        var stress = CreateCompat.kinetic().networkStress(player.level(), target);
        return stress.isPresent() ? (used ? stress.get().used() : stress.get().capacity()) : null;
    }

    private net.minecraft.world.level.block.entity.BlockEntity portBlock() {
        GlobalPos bound = player.getData(ModAttachments.DECK_DATA).ports().get(name);
        return bound == null ? null : player.serverLevel().getBlockEntity(bound.pos());
    }

    /** Machine Interface Tier II: the port's redstone output ({@code SEND redstone_signal TO port STRENGTH n}). */
    @Override
    public boolean set(String member, Object value) {
        if (!member.equals("redstone_out") || !(value instanceof Double level)
                || DeckHardware.of(dev.cyberimplants.body.BodyState.get(player)).machineInterface() < 2
                || target() == null || !(portBlock() instanceof NeuralPortBlockEntity be)) {
            return false;
        }
        be.setRedstoneOut((int) Math.max(0, Math.min(15, Math.round(level))));
        return true;
    }

    @Override
    public int writeCost() {
        return 8;
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
