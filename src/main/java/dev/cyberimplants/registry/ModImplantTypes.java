package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.implant.ImplantType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * The {@code cyberimplants:implant_type} registry. Layer 0 creates the registry and the deferred register; the
 * concrete implant types are added from Layer 1 onward.
 */
public final class ModImplantTypes {
    public static final ResourceKey<Registry<ImplantType>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(CyberImplants.id("implant_type"));

    public static final Registry<ImplantType> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY)
            .sync(true)
            .create();

    public static final DeferredRegister<ImplantType> IMPLANT_TYPES =
            DeferredRegister.create(REGISTRY, CyberImplants.MOD_ID);

    public static void onNewRegistry(NewRegistryEvent event) {
        event.register(REGISTRY);
    }

    private ModImplantTypes() {
    }
}
