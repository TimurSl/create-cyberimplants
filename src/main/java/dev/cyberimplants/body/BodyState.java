package dev.cyberimplants.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.cyberimplants.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * The persistent cybernetic state of one player (data attachment, immutable value).
 * Layer 0 holds the data model only; Load, Charge and cooldown rules arrive in Layer 1/2.
 *
 * @param installed   slot -> implant
 * @param charge      main battery, in C
 * @param buffer      Energy Buffer reserve, in C
 * @param cooldownEnd implant id -> value of {@code onlineTicks} when its cooldown ends
 * @param onlineTicks ticks the player has been online; cooldowns count in these so they pause while logged out
 */
public record BodyState(
        Map<SlotId, InstalledImplant> installed,
        double charge,
        double buffer,
        Map<ResourceLocation, Long> cooldownEnd,
        long onlineTicks
) {
    public static final BodyState EMPTY = new BodyState(Map.of(), 0, 0, Map.of(), 0);

    public static final MapCodec<BodyState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(SlotId.CODEC, InstalledImplant.CODEC)
                    .optionalFieldOf("installed", Map.of()).forGetter(BodyState::installed),
            Codec.DOUBLE.optionalFieldOf("charge", 0.0).forGetter(BodyState::charge),
            Codec.DOUBLE.optionalFieldOf("buffer", 0.0).forGetter(BodyState::buffer),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
                    .optionalFieldOf("cooldown_end", Map.of()).forGetter(BodyState::cooldownEnd),
            Codec.LONG.optionalFieldOf("online_ticks", 0L).forGetter(BodyState::onlineTicks)
    ).apply(i, BodyState::new));

    /** Plain codec form, used for network payloads. */
    public static final Codec<BodyState> CODEC = MAP_CODEC.codec();

    public BodyState {
        installed = Map.copyOf(installed);
        cooldownEnd = Map.copyOf(cooldownEnd);
    }

    public static BodyState get(Player player) {
        return player.getData(ModAttachments.BODY_STATE);
    }

    public static void set(Player player, BodyState state) {
        player.setData(ModAttachments.BODY_STATE, state);
    }

    public BodyState withImplant(SlotId slot, InstalledImplant implant) {
        Map<SlotId, InstalledImplant> next = new HashMap<>(installed);
        next.put(slot, implant);
        return new BodyState(next, charge, buffer, cooldownEnd, onlineTicks);
    }

    public BodyState withoutImplant(SlotId slot) {
        Map<SlotId, InstalledImplant> next = new HashMap<>(installed);
        next.remove(slot);
        return new BodyState(next, charge, buffer, cooldownEnd, onlineTicks);
    }

    public BodyState withCharge(double newCharge, double newBuffer) {
        return new BodyState(installed, newCharge, newBuffer, cooldownEnd, onlineTicks);
    }

    public BodyState withCooldown(ResourceLocation implantId, long endsAtOnlineTick) {
        Map<ResourceLocation, Long> next = new HashMap<>(cooldownEnd);
        next.put(implantId, endsAtOnlineTick);
        return new BodyState(installed, charge, buffer, next, onlineTicks);
    }

    public BodyState withOnlineTicks(long ticks) {
        return new BodyState(installed, charge, buffer, cooldownEnd, ticks);
    }
}
