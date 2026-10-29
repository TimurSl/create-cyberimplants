package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Bio-Interface Gel and Neural Gel (surgery and the upgrade chain). Conductive Ink, Cryo-Coolant and Nanite
 * Suspension arrive with Layer 3's real production chain.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CyberImplants.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, CyberImplants.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> BIO_GEL_TYPE = FLUID_TYPES.register("bio_interface_gel",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.cyberimplants.bio_interface_gel").density(1200).viscosity(3000)));
    public static final DeferredHolder<FluidType, FluidType> NEURAL_GEL_TYPE = FLUID_TYPES.register("neural_gel",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.cyberimplants.neural_gel").density(1400).viscosity(4000).lightLevel(6)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BIO_GEL = FLUIDS.register("bio_interface_gel",
            () -> new BaseFlowingFluid.Source(ModFluids.bioGelProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> BIO_GEL_FLOWING = FLUIDS.register("flowing_bio_interface_gel",
            () -> new BaseFlowingFluid.Flowing(ModFluids.bioGelProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> NEURAL_GEL = FLUIDS.register("neural_gel",
            () -> new BaseFlowingFluid.Source(ModFluids.neuralGelProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> NEURAL_GEL_FLOWING = FLUIDS.register("flowing_neural_gel",
            () -> new BaseFlowingFluid.Flowing(ModFluids.neuralGelProperties()));

    private static BaseFlowingFluid.Properties bioGelProperties() {
        return new BaseFlowingFluid.Properties(BIO_GEL_TYPE, BIO_GEL, BIO_GEL_FLOWING)
                .bucket(() -> ModItems.BIO_GEL_BUCKET.get()).block(() -> ModBlocks.BIO_GEL_BLOCK.get())
                .slopeFindDistance(2).levelDecreasePerBlock(2);
    }

    private static BaseFlowingFluid.Properties neuralGelProperties() {
        return new BaseFlowingFluid.Properties(NEURAL_GEL_TYPE, NEURAL_GEL, NEURAL_GEL_FLOWING)
                .bucket(() -> ModItems.NEURAL_GEL_BUCKET.get()).block(() -> ModBlocks.NEURAL_GEL_BLOCK.get())
                .slopeFindDistance(2).levelDecreasePerBlock(2);
    }

    private ModFluids() {
    }
}
