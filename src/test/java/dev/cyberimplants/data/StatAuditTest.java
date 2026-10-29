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
                assertEquals(0, t.getAsJsonObject().getAsJsonArray("unlocks").size(), file + ": unlock rows stay off in Layer 2");
            }
        }
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
