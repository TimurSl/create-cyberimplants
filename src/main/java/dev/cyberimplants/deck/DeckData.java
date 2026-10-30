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
 * @param loaded   name of the program to run on thread 0, or empty
 * @param ports    bound Neural Ports: name -> position
 * @param threads  programs for Coprocessor threads 1..n: thread number (as text) -> name
 * @param persisted ISA-5 {@code PERSIST VAR} values: program name -> variable -> encoded value (see {@link #encode})
 */
public record DeckData(Map<String, String> programs, String loaded, Map<String, GlobalPos> ports, Map<String, String> threads,
                       Map<String, Map<String, String>> persisted) {
    public static final DeckData EMPTY = new DeckData(Map.of(), "", Map.of(), Map.of(), Map.of());

    public static final MapCodec<DeckData> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("programs", Map.of()).forGetter(DeckData::programs),
            Codec.STRING.optionalFieldOf("loaded", "").forGetter(DeckData::loaded),
            Codec.unboundedMap(Codec.STRING, GlobalPos.CODEC).optionalFieldOf("ports", Map.of()).forGetter(DeckData::ports),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("threads", Map.of()).forGetter(DeckData::threads),
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.STRING))
                    .optionalFieldOf("persisted", Map.of()).forGetter(DeckData::persisted)
    ).apply(i, DeckData::new));

    public DeckData {
        programs = Map.copyOf(programs);
        ports = Map.copyOf(ports);
        threads = Map.copyOf(threads);
        persisted = Map.copyOf(persisted);
    }

    public DeckData withProgram(String name, String source) {
        Map<String, String> next = new HashMap<>(programs);
        next.put(name, source);
        return new DeckData(next, loaded, ports, threads, persisted);
    }

    public DeckData withoutProgram(String name) {
        Map<String, String> next = new HashMap<>(programs);
        next.remove(name);
        Map<String, String> nextThreads = new HashMap<>(threads);
        nextThreads.values().removeIf(name::equals);
        Map<String, Map<String, String>> nextPersisted = new HashMap<>(persisted);
        nextPersisted.remove(name);
        return new DeckData(next, loaded.equals(name) ? "" : loaded, ports, nextThreads, nextPersisted);
    }

    /** The program marked loaded on a thread, or empty. */
    public String loadedAt(int thread) {
        return thread == 0 ? loaded : threads.getOrDefault(Integer.toString(thread), "");
    }

    public DeckData withLoaded(String name) {
        return withLoaded(0, name);
    }

    public DeckData withLoaded(int thread, String name) {
        if (thread == 0) {
            return new DeckData(programs, name, ports, threads, persisted);
        }
        Map<String, String> next = new HashMap<>(threads);
        if (name.isEmpty()) {
            next.remove(Integer.toString(thread));
        } else {
            next.put(Integer.toString(thread), name);
        }
        return new DeckData(programs, loaded, ports, next, persisted);
    }

    public DeckData withPort(String name, GlobalPos pos) {
        Map<String, GlobalPos> next = new HashMap<>(ports);
        next.put(name, pos);
        return new DeckData(programs, loaded, next, threads, persisted);
    }

    public DeckData withoutPort(String name) {
        Map<String, GlobalPos> next = new HashMap<>(ports);
        next.remove(name);
        return new DeckData(programs, loaded, next, threads, persisted);
    }

    /** Replaces the persisted variables of one program. */
    public DeckData withPersisted(String program, Map<String, String> values) {
        Map<String, Map<String, String>> next = new HashMap<>(persisted);
        if (values.isEmpty()) {
            next.remove(program);
        } else {
            next.put(program, Map.copyOf(values));
        }
        return new DeckData(programs, loaded, ports, threads, next);
    }

    /** A persisted value as text: {@code n:} number, {@code b:} bool, {@code s:} string, {@code z} NULL. */
    public static String encode(Object value) {
        if (value instanceof Double d) {
            return "n:" + d;
        }
        if (value instanceof Boolean b) {
            return "b:" + b;
        }
        if (value instanceof String str) {
            return "s:" + str;
        }
        return "z";
    }

    /** The inverse of {@link #encode}; unknown text decodes to NULL. */
    public static Object decode(String text) {
        if (text.startsWith("n:")) {
            try {
                return Double.parseDouble(text.substring(2));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (text.startsWith("b:")) {
            return Boolean.parseBoolean(text.substring(2));
        }
        if (text.startsWith("s:")) {
            return text.substring(2);
        }
        return null;
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
