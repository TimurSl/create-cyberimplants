package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Bio-Interface Gel, Conductive Ink, Cryo-Coolant, Nanite Suspension, Neural Gel (Layer 3). */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CyberImplants.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, CyberImplants.MOD_ID);

    private ModFluids() {
    }
}
