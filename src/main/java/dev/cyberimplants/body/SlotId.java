package dev.cyberimplants.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** A single slot: a region plus an index below {@link BodyRegion#slots()}. Serialises as {@code "region/index"}. */
public record SlotId(BodyRegion region, int index) {
    public static final Codec<SlotId> CODEC = Codec.STRING.comapFlatMap(SlotId::parse, SlotId::toString);

    public SlotId {
        if (index < 0 || index >= region.slots()) {
            throw new IllegalArgumentException("Slot index " + index + " out of range for " + region);
        }
    }

    private static DataResult<SlotId> parse(String s) {
        int sep = s.lastIndexOf('/');
        if (sep <= 0) {
            return DataResult.error(() -> "Malformed slot id: " + s);
        }
        BodyRegion region = null;
        for (BodyRegion r : BodyRegion.values()) {
            if (r.getSerializedName().equals(s.substring(0, sep))) {
                region = r;
            }
        }
        if (region == null) {
            return DataResult.error(() -> "Unknown body region in slot id: " + s);
        }
        try {
            return DataResult.success(new SlotId(region, Integer.parseInt(s.substring(sep + 1))));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Bad slot id: " + s);
        }
    }

    @Override
    public String toString() {
        return region.getSerializedName() + "/" + index;
    }
}
