package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CyberImplants.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cyberimplants"))
                    .icon(() -> new ItemStack(ModItems.itemFor(ImplantIds.CORE)))
                    .displayItems((parameters, output) -> {
                        ModItems.implants().values().forEach(item -> output.accept(item.get()));
                        output.accept(ModItems.FIELD_MANUAL.get());
                        output.accept(ModItems.FLECHETTE.get());
                        output.accept(ModItems.IMPLANTATION_RIG.get());
                        output.accept(ModItems.KINETIC_CHARGER.get());
                        output.accept(ModItems.PRECISION_ETCHER.get());
                        output.accept(ModItems.NEURAL_PORT.get());
                        output.accept(ModItems.PROXIMITY_SENSOR.get());
                        output.accept(ModItems.NEURO_STABILIZER.get());
                        ModItems.components().forEach(item -> output.accept(item.get()));
                        ModFluids.ALL.forEach(fluid -> output.accept(fluid.bucket().get()));
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
