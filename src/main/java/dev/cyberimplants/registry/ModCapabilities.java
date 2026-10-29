package dev.cyberimplants.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Block capabilities: the Rig's gel tank accepts fluid from pipes and buckets on every face. */
public final class ModCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.RIG.get(), (rig, side) -> rig.tank());
    }

    private ModCapabilities() {
    }
}
