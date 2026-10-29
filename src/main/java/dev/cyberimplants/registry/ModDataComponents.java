package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.implant.TechTier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CyberImplants.MOD_ID);

    /** Technology tier (1-5) of an implant item. */
    public static final Supplier<DataComponentType<Integer>> TIER = DATA_COMPONENTS.register("tier",
            () -> DataComponentType.<Integer>builder()
                    .persistent(TechTier.LEVEL_CODEC)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private ModDataComponents() {
    }
}
