package dev.cyberimplants.deck;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * A player's stored programs (deck Storage). Persists with the player; the {@code loaded} program is rebooted on
 * login and respawn.
 *
 * @param programs program name -> source
 * @param loaded   name of the program to run, or empty
 */
public record DeckData(Map<String, String> programs, String loaded) {
    public static final DeckData EMPTY = new DeckData(Map.of(), "");

    public static final MapCodec<DeckData> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("programs", Map.of()).forGetter(DeckData::programs),
            Codec.STRING.optionalFieldOf("loaded", "").forGetter(DeckData::loaded)
    ).apply(i, DeckData::new));

    public DeckData {
        programs = Map.copyOf(programs);
    }

    public DeckData withProgram(String name, String source) {
        Map<String, String> next = new HashMap<>(programs);
        next.put(name, source);
        return new DeckData(next, loaded);
    }

    public DeckData withoutProgram(String name) {
        Map<String, String> next = new HashMap<>(programs);
        next.remove(name);
        return new DeckData(next, loaded.equals(name) ? "" : loaded);
    }

    public DeckData withLoaded(String name) {
        return new DeckData(programs, name);
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
