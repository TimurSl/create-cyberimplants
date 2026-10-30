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

    /**
     * Unlock rows are cumulative over the tiers and never on Tier I. Rows first unlocked at Tiers II-IV belong to layer 3
     * (Capability), Tier V rows to layer 5 (Mastery).
     */
    @Test
    void unlockRowsAreCumulativeAndRegistered() throws IOException {
        int withUnlocks = 0;
        int withTierFive = 0;
        for (Path file : statFiles()) {
            JsonObject json = read(file);
            JsonArray tiers = json.getAsJsonArray("tiers");
            JsonObject layers = json.getAsJsonObject("unlock_layers");
            assertEquals(0, tiers.get(0).getAsJsonObject().getAsJsonArray("unlocks").size(), file + ": Tier I has no unlocks");
            Set<String> previous = Set.of();
            boolean tierFive = false;
            for (int tier = 1; tier <= 5; tier++) {
                Set<String> unlocks = new TreeSet<>();
                tiers.get(tier - 1).getAsJsonObject().getAsJsonArray("unlocks").forEach(u -> unlocks.add(u.getAsString()));
                assertTrue(unlocks.containsAll(previous), file + ": a tier lost an unlock of the tier below");
                for (String u : unlocks) {
                    if (!previous.contains(u)) {
                        assertEquals(tier == 5 ? 5 : 3, layers.get(u).getAsInt(), file + ": unlock " + u + " first appears at Tier " + tier);
                        tierFive |= tier == 5;
                    }
                }
                previous = unlocks;
            }
            withUnlocks += previous.isEmpty() ? 0 : 1;
            withTierFive += tierFive ? 1 : 0;
        }
        assertTrue(withUnlocks >= 5, "Layer 3 starts with at least five implants that have unlock rows");
        assertTrue(withTierFive >= 15, "Layer 5 gives at least fifteen implants a Tier V row, found " + withTierFive);
    }

    /** The basic Tier I implants come off a crafting table, so the first implant needs only andesite-age parts. */
    @Test
    void basicImplantsAreCraftedFromCivilianParts() throws IOException {
        for (Path file : statFiles()) {
            String id = file.getFileName().toString().replace(".json", "");
            JsonObject recipe = read(RECIPES.resolve(id + ".json"));
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), id);
            assertEquals("cyberimplants:civilian_casing", recipe.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString(), id);
            assertEquals(1, recipe.getAsJsonObject("result").getAsJsonObject("components").get("cyberimplants:tier").getAsInt(), id);
        }
    }

    /** Every item a recipe of this mod names must be made by this mod's own chain (or come from Minecraft or Create). */
    @Test
    void everyComponentHasARecipe() throws IOException {
        Set<String> made = new TreeSet<>();
        Set<String> used = new TreeSet<>();
        try (Stream<Path> s = Files.walk(RECIPES)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".json")).toList()) {
                String text = Files.readString(p);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"(?:item|id)\": \"(cyberimplants:[a-z_]+)\"").matcher(text);
                while (m.find()) {
                    used.add(m.group(1));
                }
                JsonObject json = read(p);
                if (json.has("results")) {
                    json.getAsJsonArray("results").forEach(r -> made.add(r.getAsJsonObject().get("id").getAsString()));
                }
                if (json.has("result")) {
                    made.add(json.getAsJsonObject("result").get("id").getAsString());
                }
                if (json.has("transitional_item")) {
                    made.add(json.getAsJsonObject("transitional_item").get("id").getAsString());
                }
            }
        }
        used.removeAll(made);
        assertTrue(used.isEmpty(), "used in recipes but never made: " + used);
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

    /** Layer 5: Tier III-V grades etch on the Precision Etcher at 64, 128 and 256 RPM; nothing etches on a saw any more. */
    @Test
    void etchStepsUseTheEtcherAtTheTierSpeed() throws IOException {
        int[] rpm = {0, 0, 0, 64, 128, 256};
        for (Path file : statFiles()) {
            String id = file.getFileName().toString().replace(".json", "");
            for (int tier = 2; tier <= 5; tier++) {
                JsonArray sequence = read(RECIPES.resolve("upgrade/" + id + "_t" + tier + ".json")).getAsJsonArray("sequence");
                boolean etched = false;
                for (JsonElement step : sequence) {
                    JsonObject o = step.getAsJsonObject();
                    assertFalse(o.get("type").getAsString().equals("create:cutting"), id + " T" + tier + " still cuts");
                    if (o.get("type").getAsString().equals("cyberimplants:etching")) {
                        etched = true;
                        assertEquals(rpm[tier], o.get("min_rpm").getAsInt(), id + " T" + tier + " etches at the tier's speed");
                    }
                }
                assertEquals(tier >= 3, etched, id + " T" + tier + " etch step");
            }
        }
    }
}
