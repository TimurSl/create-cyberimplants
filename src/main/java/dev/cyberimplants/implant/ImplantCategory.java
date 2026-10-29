package dev.cyberimplants.implant;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Implant categories A-H from the design spec, plus the Cybernetic Core. */
public enum ImplantCategory implements StringRepresentable {
    CORE("core"),
    NEURAL("neural"),
    CYBERDECK("cyberdeck"),
    SKELETAL("skeletal"),
    LIMBS("limbs"),
    OPTICAL("optical"),
    CIRCULATORY("circulatory"),
    SUBDERMAL("subdermal"),
    MOBILITY("mobility");

    public static final Codec<ImplantCategory> CODEC = StringRepresentable.fromEnum(ImplantCategory::values);

    private final String serializedName;

    ImplantCategory(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
