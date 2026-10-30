package dev.cyberimplants.client;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.compat.create.block.RigRenderer;
import dev.cyberimplants.registry.ModBlockEntities;
import dev.cyberimplants.registry.ModEntities;
import dev.cyberimplants.client.render.Afterimages;
import dev.cyberimplants.client.render.ImplantModels;
import dev.cyberimplants.client.render.ImplantRender;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/** Wires up everything client-only. Loaded only on the physical client (see {@link CyberImplants}). */
public final class ClientSetup {
    private static ModContainer container;

    public static void init(IEventBus modBus, ModContainer modContainer) {
        container = modContainer;
        // Mods > Create: Cybernetics > Config edits the client config (HUD, visuals, UI) in game.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(ClientSetup::registerKeys);
        modBus.addListener(ImplantModels::registerLayer);
        modBus.addListener(ClientSetup::addLayers);
        modBus.addListener(ClientSetup::registerGuiLayers);
        modBus.addListener(ClientFluids::register);
        modBus.addListener(ClientSetup::registerRenderers);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onMovementInput);
        NeoForge.EVENT_BUS.addListener(ClientMovement::onJump);
        NeoForge.EVENT_BUS.addListener(ClientFx::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(ClientFx::onFov);
        NeoForge.EVENT_BUS.addListener(ImplantRender::onRenderArm);
        NeoForge.EVENT_BUS.addListener(Afterimages::render);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            ClientAppearance.clear();
            Afterimages.clear();
        });
    }

    /** Opens the in-game config screen (HUD layout, implant visuals, UI). */
    public static void openSettings(Screen parent) {
        Minecraft.getInstance().setScreen(new ConfigurationScreen(container, parent));
    }

    /** Implant models on both player renderers; the models themselves are baked here on every resource reload. */
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        ImplantModels.bake(event.getEntityModels());
        Afterimages.bake(event.getEntityModels());
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new ImplantRender.Layer(renderer));
            }
        }
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
        Afterimages.tick();
    }

    private static void onMovementInput(MovementInputUpdateEvent event) {
        ClientRig.onMovementInput(event.getInput());
        ClientMovement.onMovementInput(event.getInput());
    }

    private ClientSetup() {
    }
}
