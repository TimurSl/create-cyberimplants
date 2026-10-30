package dev.cyberimplants.client;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.registry.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Process fluid textures: the animated neon sprites of this mod (textures/block/*_still and *_flow), lightly translucent. */
public final class ClientFluids {
    public static void register(RegisterClientExtensionsEvent event) {
        for (ModFluids.ProcessFluid fluid : ModFluids.ALL) {
            event.registerFluidType(sprites(fluid.name(), 0xE6FFFFFF), fluid.type().get());
        }
    }

    private static IClientFluidTypeExtensions sprites(String name, int argb) {
        ResourceLocation still = CyberImplants.id("block/" + name + "_still");
        ResourceLocation flowing = CyberImplants.id("block/" + name + "_flow");
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
