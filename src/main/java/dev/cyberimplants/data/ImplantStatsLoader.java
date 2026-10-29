package dev.cyberimplants.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.registry.ModImplantTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/** Reads {@code data/<ns>/cyberimplants/implant_stats/<type>.json}; the file name is the implant type id. */
public final class ImplantStatsLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final String DIRECTORY = "cyberimplants/implant_stats";

    public ImplantStatsLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, ImplantStats> loaded = new HashMap<>();
        files.forEach((id, json) -> ImplantStats.CODEC.parse(JsonOps.INSTANCE, json)
                .ifSuccess(stats -> loaded.put(id, stats))
                .ifError(err -> CyberImplants.LOGGER.error("Invalid implant stats {}: {}", id, err.message())));

        ImplantStatsRegistry.replace(loaded);

        // Every registered implant type must have stats for all five tiers.
        ModImplantTypes.REGISTRY.keySet().stream()
                .filter(id -> !loaded.containsKey(id))
                .forEach(id -> CyberImplants.LOGGER.warn("Implant type {} has no stats file", id));

        CyberImplants.LOGGER.info("Loaded stats for {} implant type(s)", loaded.size());
    }
}
