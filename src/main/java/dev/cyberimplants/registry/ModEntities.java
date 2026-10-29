package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.content.RigSeatEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, CyberImplants.MOD_ID);

    /** The Implantation Rig's invisible seat (see {@link RigSeatEntity}). */
    public static final DeferredHolder<EntityType<?>, EntityType<RigSeatEntity>> RIG_SEAT = ENTITY_TYPES.register("rig_seat",
            () -> EntityType.Builder.<RigSeatEntity>of(RigSeatEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).noSummon().fireImmune().clientTrackingRange(10).updateInterval(20)
                    .build("rig_seat"));

    private ModEntities() {
    }
}
