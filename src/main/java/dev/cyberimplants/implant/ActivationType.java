package dev.cyberimplants.implant;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** How an implant is used (docs/design/01-core-systems.md, section 1.6). */
public enum ActivationType implements StringRepresentable {
    PASSIVE("passive"),
    TOGGLE("toggle"),
    ACTIVE("active"),
    INPUT_BOUND("input_bound"),
    TRIGGERED("triggered");

    public static final Codec<ActivationType> CODEC = StringRepresentable.fromEnum(ActivationType::values);

    private final String serializedName;

    ActivationType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /** Input-bound abilities can never be driven by a Synapse script (safety rule 1). */
    public boolean scriptable() {
        return this != INPUT_BOUND;
    }
}
