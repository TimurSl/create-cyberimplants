package dev.cyberimplants.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.cyberimplants.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The persistent cybernetic state of one player (data attachment, immutable value).
 *
 * @param installed   slot -> implant
 * @param charge      main battery, in C
 * @param buffer      Energy Buffer reserve, in C
 * @param cooldownEnd implant id -> value of {@code onlineTicks} when its cooldown ends
 * @param onlineTicks ticks the player has been online; cooldowns count in these so they pause while logged out
 * @param data        small persisted numbers keyed by name: toggle states ({@code toggle/thermal_vision}), the
 *                    Ballistic Module magazine, anything that must survive logout but is not a stat
 */
public record BodyState(
        Map<SlotId, InstalledImplant> installed,
        double charge,
        double buffer,
        Map<ResourceLocation, Long> cooldownEnd,
        long onlineTicks,
        Map<String, Double> data
) {
    public static final BodyState EMPTY = new BodyState(Map.of(), 0, 0, Map.of(), 0, Map.of());

    public static final MapCodec<BodyState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(SlotId.CODEC, InstalledImplant.CODEC)
                    .optionalFieldOf("installed", Map.of()).forGetter(BodyState::installed),
            Codec.DOUBLE.optionalFieldOf("charge", 0.0).forGetter(BodyState::charge),
            Codec.DOUBLE.optionalFieldOf("buffer", 0.0).forGetter(BodyState::buffer),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
                    .optionalFieldOf("cooldown_end", Map.of()).forGetter(BodyState::cooldownEnd),
            Codec.LONG.optionalFieldOf("online_ticks", 0L).forGetter(BodyState::onlineTicks),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
                    .optionalFieldOf("data", Map.of()).forGetter(BodyState::data)
    ).apply(i, BodyState::new));

    /** Plain codec form, used for network payloads. */
    public static final Codec<BodyState> CODEC = MAP_CODEC.codec();

    public BodyState {
        installed = Map.copyOf(installed);
        cooldownEnd = Map.copyOf(cooldownEnd);
        data = Map.copyOf(data);
    }

    /** The server's state on the server, the last synced state on the client. */
    public static BodyState get(Player player) {
        if (player.level().isClientSide) {
            return dev.cyberimplants.client.ClientBodyState.get();
        }
        return player.getData(ModAttachments.BODY_STATE);
    }

    public static void set(Player player, BodyState state) {
        player.setData(ModAttachments.BODY_STATE, state);
    }

    // ---------------------------------------------------------------- queries

    /** The first slot holding an implant of this type. */
    public Optional<Map.Entry<SlotId, InstalledImplant>> find(ResourceLocation type) {
        for (Map.Entry<SlotId, InstalledImplant> e : installed.entrySet()) {
            if (e.getValue().type().equals(type)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    public boolean has(ResourceLocation type) {
        return find(type).isPresent();
    }

    /** Tier of the installed implant of this type, or 0. */
    public int tierOf(ResourceLocation type) {
        return find(type).map(e -> e.getValue().tier()).orElse(0);
    }

    public boolean flag(String key) {
        return data.getOrDefault(key, 0.0) != 0.0;
    }

    public double number(String key, double fallback) {
        return data.getOrDefault(key, fallback);
    }

    /** Remaining cooldown in ticks (0 when ready). */
    public long cooldownRemaining(ResourceLocation type) {
        return Math.max(0, cooldownEnd.getOrDefault(type, 0L) - onlineTicks);
    }

    // ---------------------------------------------------------------- updates

    public BodyState withImplant(SlotId slot, InstalledImplant implant) {
        Map<SlotId, InstalledImplant> next = new HashMap<>(installed);
        next.put(slot, implant);
        return new BodyState(next, charge, buffer, cooldownEnd, onlineTicks, data);
    }

    public BodyState withoutImplant(SlotId slot) {
        Map<SlotId, InstalledImplant> next = new HashMap<>(installed);
        next.remove(slot);
        return new BodyState(next, charge, buffer, cooldownEnd, onlineTicks, data);
    }

    public BodyState withCharge(double newCharge, double newBuffer) {
        return new BodyState(installed, newCharge, newBuffer, cooldownEnd, onlineTicks, data);
    }

    public BodyState withCooldown(ResourceLocation implantId, long endsAtOnlineTick) {
        Map<ResourceLocation, Long> next = new HashMap<>(cooldownEnd);
        next.put(implantId, endsAtOnlineTick);
        return new BodyState(installed, charge, buffer, next, onlineTicks, data);
    }

    public BodyState withOnlineTicks(long ticks) {
        return new BodyState(installed, charge, buffer, cooldownEnd, ticks, data);
    }

    public BodyState withData(String key, double value) {
        Map<String, Double> next = new HashMap<>(data);
        next.put(key, value);
        return new BodyState(installed, charge, buffer, cooldownEnd, onlineTicks, next);
    }

    public BodyState withFlag(String key, boolean value) {
        return withData(key, value ? 1.0 : 0.0);
    }
}
