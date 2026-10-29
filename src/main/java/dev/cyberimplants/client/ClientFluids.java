package dev.cyberimplants.client;

import dev.cyberimplants.registry.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Gel textures: vanilla water sprites with a tint. */
public final class ClientFluids {
    private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOWING = ResourceLocation.withDefaultNamespace("block/water_flow");

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(tinted(0xCC6FE0A8), ModFluids.BIO_GEL_TYPE.get());
        event.registerFluidType(tinted(0xCC9B6BFF), ModFluids.NEURAL_GEL_TYPE.get());
    }

    private static IClientFluidTypeExtensions tinted(int argb) {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING;
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
