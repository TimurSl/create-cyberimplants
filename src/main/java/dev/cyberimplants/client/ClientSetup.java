package dev.cyberimplants.client;

import dev.cyberimplants.CyberImplants;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/** Wires up everything client-only. Loaded only on the physical client (see {@link CyberImplants}). */
public final class ClientSetup {
    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerKeys);
        modBus.addListener(ClientSetup::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onMovementInput);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : ModKeys.all()) {
            event.register(key);
        }
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, CyberImplants.id("hud"), Hud::render);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientBodyState.tick();
        ClientInput.tick();
        ClientMovement.tick();
        ClientOptics.tick();
    }

    private static void onMovementInput(MovementInputUpdateEvent event) {
        ClientMovement.onMovementInput(event.getInput());
    }

    private ClientSetup() {
    }
}
