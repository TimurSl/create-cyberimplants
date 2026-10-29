package dev.cyberimplants.client;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.compat.create.block.RigRenderer;
import dev.cyberimplants.registry.ModBlockEntities;
import dev.cyberimplants.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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
        modBus.addListener(ClientFluids::register);
        modBus.addListener(ClientSetup::registerRenderers);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onMovementInput);
        NeoForge.EVENT_BUS.addListener(ClientFx::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(ClientFx::onFov);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : ModKeys.all()) {
            event.register(key);
        }
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // Screen effects sit under the crosshair and the HUD, so washes never hide the readouts.
        event.registerBelow(VanillaGuiLayers.CROSSHAIR, CyberImplants.id("screen_fx"), ClientFx::render);
        event.registerAbove(VanillaGuiLayers.HOTBAR, CyberImplants.id("hud"), Hud::render);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.RIG.get(), RigRenderer::new);
        event.registerEntityRenderer(ModEntities.RIG_SEAT.get(), NoopRenderer::new);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientBodyState.tick();
        ClientInput.tick();
        ClientMovement.tick();
        ClientOptics.tick();
        ClientFx.tick();
    }

    private static void onMovementInput(MovementInputUpdateEvent event) {
        ClientRig.onMovementInput(event.getInput());
        ClientMovement.onMovementInput(event.getInput());
    }

    private ClientSetup() {
    }
}
