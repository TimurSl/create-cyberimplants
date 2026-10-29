package dev.cyberimplants.registry;

import net.neoforged.bus.api.IEventBus;

/** Single entry point that attaches every deferred register to the mod event bus. */
public final class ModRegistries {
    public static void register(IEventBus modBus) {
        modBus.addListener(ModImplantTypes::onNewRegistry);

        ModImplantTypes.IMPLANT_TYPES.register(modBus);
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        ModAttachments.ATTACHMENT_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModMenus.MENU_TYPES.register(modBus);
        ModRecipes.RECIPE_TYPES.register(modBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);
    }

    private ModRegistries() {
    }
}
