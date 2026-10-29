package dev.cyberimplants.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Layer 2 exit criterion: every implant has data for all five tiers, and the tiers are real (not Tier I copies). */
class StatAuditTest {
    private static final Path STATS = Path.of("src/main/resources/data/cyberimplants/cyberimplants/implant_stats");
    private static final Path RECIPES = Path.of("src/main/resources/data/cyberimplants/recipe");

    private static List<Path> statFiles() throws IOException {
        try (Stream<Path> s = Files.list(STATS)) {
            return s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static JsonObject read(Path p) throws IOException {
        try (Reader r = Files.newBufferedReader(p)) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void everyImplantHasFiveTiersWithTheSameKeys() throws IOException {
        List<Path> files = statFiles();
        assertTrue(files.size() >= 46, "expected a stats file per implant type, found " + files.size());
        for (Path file : files) {
            JsonArray tiers = read(file).getAsJsonArray("tiers");
            assertEquals(5, tiers.size(), file + " must hold five tiers");
            Set<String> keys = new TreeSet<>(tiers.get(0).getAsJsonObject().getAsJsonObject("values").keySet());
            for (JsonElement t : tiers) {
                assertEquals(keys, new TreeSet<>(t.getAsJsonObject().getAsJsonObject("values").keySet()), file + ": tier keys differ");
            }
        }
    }

    /** Layer 3: unlock rows are cumulative over the tiers, never on Tier I, and each one is registered for layer 3. */
    @Test
    void unlockRowsAreCumulativeAndRegistered() throws IOException {
        int withUnlocks = 0;
        for (Path file : statFiles()) {
            JsonObject json = read(file);
            JsonArray tiers = json.getAsJsonArray("tiers");
            JsonObject layers = json.getAsJsonObject("unlock_layers");
            assertEquals(0, tiers.get(0).getAsJsonObject().getAsJsonArray("unlocks").size(), file + ": Tier I has no unlocks");
            Set<String> previous = Set.of();
            for (JsonElement t : tiers) {
                Set<String> unlocks = new TreeSet<>();
                t.getAsJsonObject().getAsJsonArray("unlocks").forEach(u -> unlocks.add(u.getAsString()));
                assertTrue(unlocks.containsAll(previous), file + ": a tier lost an unlock of the tier below");
                for (String u : unlocks) {
                    assertEquals(3, layers.get(u).getAsInt(), file + ": unlock " + u + " belongs to layer 3");
                }
                previous = unlocks;
            }
            if (!previous.isEmpty()) {
                withUnlocks++;
            }
        }
        assertTrue(withUnlocks >= 5, "Layer 3 starts with at least five implants that have unlock rows");
    }

    @Test
    void tiersAreNotCopiesOfTierOne() throws IOException {
        for (Path file : statFiles()) {
            JsonArray tiers = read(file).getAsJsonArray("tiers");
            boolean differs = false;
            for (int i = 1; i < 5 && !differs; i++) {
                differs = !tiers.get(i).equals(tiers.get(0));
            }
            assertTrue(differs, file + " repeats Tier I in every tier");
        }
    }

    @Test
    void everyImplantHasAnUpgradeToEachHigherTier() throws IOException {
        for (Path file : statFiles()) {
            String id = file.getFileName().toString().replace(".json", "");
            for (int tier = 2; tier <= 5; tier++) {
                Path recipe = RECIPES.resolve("upgrade/" + id + "_t" + tier + ".json");
                assertTrue(Files.exists(recipe), "missing upgrade recipe " + recipe);
                JsonObject json = read(recipe);
                assertEquals("create:sequenced_assembly", json.get("type").getAsString());
                assertEquals(tier == 2 ? 1 : tier - 1, json.get("loops").getAsInt(), "loops scale with the tier");
                assertFalse(json.getAsJsonArray("sequence").isEmpty());
            }
        }
    }
}
