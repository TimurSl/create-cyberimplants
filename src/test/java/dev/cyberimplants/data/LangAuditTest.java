package dev.cyberimplants.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cyberimplants.synapse.IsaLevel;
import dev.cyberimplants.synapse.compiler.Compiler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every language file has the same keys and format specifiers, and every Synapse example in the Field Manual compiles. */
class LangAuditTest {
    private static final Path LANG = Path.of("src/main/resources/assets/cyberimplants/lang");
    private static final Pattern FORMAT = Pattern.compile("%(?:\\d+\\$)?[A-Za-z%]");
    private static final Pattern MANUAL_BODY = Pattern.compile("cyberimplants\\.manual\\.(\\w+)\\.body");

    private static JsonObject read(String file) throws IOException {
        try (Reader r = Files.newBufferedReader(LANG.resolve(file))) {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
    }

    @Test
    void russianHasExactlyTheEnglishKeys() throws IOException {
        Set<String> en = new TreeSet<>(read("en_us.json").keySet());
        Set<String> ru = new TreeSet<>(read("ru_ru.json").keySet());
        Set<String> missing = new TreeSet<>(en);
        missing.removeAll(ru);
        Set<String> extra = new TreeSet<>(ru);
        extra.removeAll(en);
        assertTrue(missing.isEmpty(), "ru_ru.json lacks " + missing);
        assertTrue(extra.isEmpty(), "ru_ru.json has keys en_us.json does not: " + extra);
    }

    @Test
    void formatSpecifiersMatchBetweenLanguages() throws IOException {
        JsonObject en = read("en_us.json");
        JsonObject ru = read("ru_ru.json");
        for (String key : en.keySet()) {
            assertEquals(specifiers(en.get(key).getAsString()), specifiers(ru.get(key).getAsString()), key);
        }
    }

    private static List<String> specifiers(String text) {
        List<String> found = new ArrayList<>();
        Matcher m = FORMAT.matcher(text);
        while (m.find()) {
            found.add(m.group());
        }
        found.sort(null);
        return found;
    }

    @Test
    void everyManualChapterHasATitleAndABodyInBothLanguages() throws IOException {
        for (String file : List.of("en_us.json", "ru_ru.json")) {
            JsonObject lang = read(file);
            int chapters = 0;
            for (String key : lang.keySet()) {
                Matcher m = MANUAL_BODY.matcher(key);
                if (m.matches()) {
                    chapters++;
                    assertTrue(lang.has("cyberimplants.manual." + m.group(1) + ".title"), file + ": no title for " + m.group(1));
                    assertFalse(lang.get(key).getAsString().isBlank(), file + ": empty body of " + m.group(1));
                }
            }
            assertTrue(chapters >= 10, file + " should hold the whole manual, found " + chapters + " chapters");
        }
    }

    @Test
    void synapseExamplesInTheManualCompile() throws IOException {
        int programs = 0;
        for (String file : List.of("en_us.json", "ru_ru.json")) {
            JsonObject lang = read(file);
            for (String key : lang.keySet()) {
                if (!MANUAL_BODY.matcher(key).matches()) {
                    continue;
                }
                StringBuilder code = new StringBuilder();
                for (String line : (lang.get(key).getAsString() + "\n").split("\n")) {
                    if (line.startsWith("|")) {
                        code.append(line.substring(1)).append('\n');
                    } else if (code.length() > 0) {
                        // Monospace blocks that do not start with PROGRAM are plain tables.
                        if (code.toString().startsWith("PROGRAM")) {
                            Compiler.compile(code.toString(), IsaLevel.ISA_2);
                            programs++;
                        }
                        code.setLength(0);
                    }
                }
            }
        }
        assertTrue(programs >= 8, "expected the manual's example programs, compiled " + programs);
    }
}
