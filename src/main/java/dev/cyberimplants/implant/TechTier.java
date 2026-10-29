package dev.cyberimplants.implant;

import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;

/** The five technology tiers (see docs/design/06-progression-and-crafting.md). */
public enum TechTier {
    CIVILIAN(1),
    INDUSTRIAL(2),
    ADVANCED(3),
    MILITARY(4),
    EXPERIMENTAL(5);

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 5;

    /** Codec for the {@code cyberimplants:tier} data component and for stored tiers. */
    public static final Codec<Integer> LEVEL_CODEC = ExtraCodecs.intRange(MIN_LEVEL, MAX_LEVEL);

    private final int level;

    TechTier(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public String translationKey() {
        return "cyberimplants.tier." + level;
    }

    public static TechTier fromLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Tier level out of range: " + level);
        }
        return values()[level - 1];
    }
}
