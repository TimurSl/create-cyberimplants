package dev.cyberimplants.body;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Anatomical regions and their slot counts (docs/design/01-core-systems.md, section 1.1). */
public enum BodyRegion implements StringRepresentable {
    CORE("core", 1),
    CORTEX("cortex", 2),
    SPINE("spine", 1),
    OCULAR("ocular", 2),
    SKELETON("skeleton", 3),
    LEFT_ARM("left_arm", 1),
    RIGHT_ARM("right_arm", 1),
    CIRCULATORY("circulatory", 3),
    INTEGUMENT("integument", 2),
    LEGS("legs", 2);

    public static final Codec<BodyRegion> CODEC = StringRepresentable.fromEnum(BodyRegion::values);

    private final String serializedName;
    private final int slots;

    BodyRegion(String serializedName, int slots) {
        this.serializedName = serializedName;
        this.slots = slots;
    }

    public int slots() {
        return slots;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
