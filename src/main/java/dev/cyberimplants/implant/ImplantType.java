package dev.cyberimplants.implant;

import dev.cyberimplants.body.BodyRegion;

import java.util.EnumSet;
import java.util.Set;

/**
 * Static description of an implant type. One registry entry per type; the tier lives on the item's
 * {@code cyberimplants:tier} component and per-tier numbers live in data (see ImplantStats).
 * Runtime behaviour is attached in Layer 1.
 */
public record ImplantType(
        ImplantCategory category,
        BodyRegion region,
        ActivationType activation,
        Set<ImplantFlag> flags
) {
    public ImplantType {
        flags = flags.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(flags));
    }

    public static ImplantType of(ImplantCategory category, BodyRegion region, ActivationType activation, ImplantFlag... flags) {
        Set<ImplantFlag> set = EnumSet.noneOf(ImplantFlag.class);
        set.addAll(Set.of(flags));
        return new ImplantType(category, region, activation, set);
    }

    public boolean has(ImplantFlag flag) {
        return flags.contains(flag);
    }
}
