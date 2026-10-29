package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/** The {@code cyberimplants:implant_type} registry; its entries are exactly {@link ImplantCatalog#ALL}. */
public final class ModImplantTypes {
    public static final ResourceKey<Registry<ImplantType>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(CyberImplants.id("implant_type"));

    public static final Registry<ImplantType> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY)
            .sync(true)
            .create();

    public static final DeferredRegister<ImplantType> IMPLANT_TYPES =
            DeferredRegister.create(REGISTRY, CyberImplants.MOD_ID);

    static {
        for (ImplantCatalog.Entry e : ImplantCatalog.ALL) {
            IMPLANT_TYPES.register(e.path(), e::type);
        }
    }

    public static void onNewRegistry(NewRegistryEvent event) {
        event.register(REGISTRY);
    }

    private ModImplantTypes() {
    }
}
