package dev.cyberimplants.client;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.registry.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Gel textures: the animated neon sprites of this mod (textures/block/*_gel_still and *_flow), lightly translucent. */
public final class ClientFluids {
    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(sprites("bio_interface_gel", 0xE6FFFFFF), ModFluids.BIO_GEL_TYPE.get());
        event.registerFluidType(sprites("neural_gel", 0xE6FFFFFF), ModFluids.NEURAL_GEL_TYPE.get());
    }

    private static IClientFluidTypeExtensions sprites(String gel, int argb) {
        ResourceLocation still = CyberImplants.id("block/" + gel + "_still");
        ResourceLocation flowing = CyberImplants.id("block/" + gel + "_flow");
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }

            @Override
            public int getTintColor() {
                return argb;
            }
        };
    }

    private ClientFluids() {
    }
}
