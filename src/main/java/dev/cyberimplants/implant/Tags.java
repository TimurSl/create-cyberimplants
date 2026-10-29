package dev.cyberimplants.implant;

import dev.cyberimplants.CyberImplants;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/** Tags that carry this mod's data-driven rules (see data/cyberimplants/tags). */
public final class Tags {
    public static final TagKey<DamageType> BLUNT = damage("blunt");
    public static final TagKey<DamageType> MACHINERY = damage("machinery");
    public static final TagKey<DamageType> SHIELD_BYPASS = damage("shield_bypass");
    public static final TagKey<DamageType> NANO_THERMAL = damage("nano/thermal");
    public static final TagKey<DamageType> NANO_KINETIC = damage("nano/kinetic");
    public static final TagKey<DamageType> NANO_BLAST = damage("nano/blast");
    public static final TagKey<DamageType> NANO_CHEMICAL = damage("nano/chemical");

    public static final TagKey<EntityType<?>> AUGMENT_RESISTANT = TagKey.create(Registries.ENTITY_TYPE, CyberImplants.id("augment_resistant"));
    public static final TagKey<EntityType<?>> EVADABLE = TagKey.create(Registries.ENTITY_TYPE, CyberImplants.id("evadable"));

    public static final TagKey<Block> MAGNETIC = TagKey.create(Registries.BLOCK, CyberImplants.id("magnetic"));
    /** Glass and ice: ungrippable until Microgrip Tier III. */
    public static final TagKey<Block> UNGRIPPABLE_SMOOTH = TagKey.create(Registries.BLOCK, CyberImplants.id("ungrippable_smooth"));
    public static final TagKey<Block> UNGRIPPABLE = TagKey.create(Registries.BLOCK, CyberImplants.id("ungrippable"));

    private static TagKey<DamageType> damage(String path) {
        return TagKey.create(Registries.DAMAGE_TYPE, CyberImplants.id(path));
    }

    private Tags() {
    }
}
