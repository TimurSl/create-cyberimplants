package dev.cyberimplants;

import com.mojang.logging.LogUtils;
import dev.cyberimplants.compat.create.CreateCompat;
import dev.cyberimplants.config.ClientConfig;
import dev.cyberimplants.config.ServerConfig;
import dev.cyberimplants.network.NetworkHandler;
import dev.cyberimplants.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(CyberImplants.MOD_ID)
public final class CyberImplants {
    public static final String MOD_ID = "cyberimplants";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CyberImplants(IEventBus modBus, ModContainer container) {
        ModRegistries.register(modBus);
        modBus.addListener(NetworkHandler::registerPayloads);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(CreateCompat::init));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            dev.cyberimplants.client.ClientSetup.init(modBus);
        }

        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
