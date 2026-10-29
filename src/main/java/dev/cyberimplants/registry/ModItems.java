package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Implant items, deck modules, components and consumables are registered from Layer 1 onward. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CyberImplants.MOD_ID);

    private ModItems() {
    }
}
