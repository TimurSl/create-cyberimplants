package dev.cyberimplants.deck;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;

import java.util.HashMap;
import java.util.Map;

/**
 * A player's stored programs (deck Storage) and bound devices. Persists with the player; the {@code loaded} program is
 * rebooted on login and respawn.
 *
 * @param programs program name -> source
 * @param loaded   name of the program to run, or empty
 * @param ports    bound Neural Ports: name -> position
 */
public record DeckData(Map<String, String> programs, String loaded, Map<String, GlobalPos> ports) {
    public static final DeckData EMPTY = new DeckData(Map.of(), "", Map.of());

    public static final MapCodec<DeckData> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("programs", Map.of()).forGetter(DeckData::programs),
            Codec.STRING.optionalFieldOf("loaded", "").forGetter(DeckData::loaded),
            Codec.unboundedMap(Codec.STRING, GlobalPos.CODEC).optionalFieldOf("ports", Map.of()).forGetter(DeckData::ports)
    ).apply(i, DeckData::new));

    public DeckData {
        programs = Map.copyOf(programs);
        ports = Map.copyOf(ports);
    }

    public DeckData withProgram(String name, String source) {
        Map<String, String> next = new HashMap<>(programs);
        next.put(name, source);
        return new DeckData(next, loaded, ports);
    }

    public DeckData withoutProgram(String name) {
        Map<String, String> next = new HashMap<>(programs);
        next.remove(name);
        return new DeckData(next, loaded.equals(name) ? "" : loaded, ports);
    }

    public DeckData withLoaded(String name) {
        return new DeckData(programs, name, ports);
    }

    public DeckData withPort(String name, GlobalPos pos) {
        Map<String, GlobalPos> next = new HashMap<>(ports);
        next.put(name, pos);
        return new DeckData(programs, loaded, next);
    }

    public DeckData withoutPort(String name) {
        Map<String, GlobalPos> next = new HashMap<>(ports);
        next.remove(name);
        return new DeckData(programs, loaded, next);
    }

    /** Total stored size in bytes (UTF-8), checked against Storage capacity. */
    public int storedBytes() {
        int total = 0;
        for (Map.Entry<String, String> e : programs.entrySet()) {
            total += e.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        }
        return total;
    }
}
