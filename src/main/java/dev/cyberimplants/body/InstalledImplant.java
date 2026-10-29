package dev.cyberimplants.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.cyberimplants.implant.TechTier;
import net.minecraft.resources.ResourceLocation;

/**
 * An implant sitting in a slot. The type is stored by id and resolved lazily, so removing an implant from the
 * registry never corrupts a player's saved body.
 */
public record InstalledImplant(ResourceLocation type, int tier) {
    public static final Codec<InstalledImplant> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(InstalledImplant::type),
            TechTier.LEVEL_CODEC.fieldOf("tier").forGetter(InstalledImplant::tier)
    ).apply(i, InstalledImplant::new));
}
