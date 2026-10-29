package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/** One item per implant type (the tier is a component), plus ammunition and the machine block items. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CyberImplants.MOD_ID);

    private static final Map<ResourceLocation, DeferredItem<ImplantItem>> IMPLANTS = new LinkedHashMap<>();

    static {
        for (ImplantCatalog.Entry e : ImplantCatalog.ALL) {
            IMPLANTS.put(e.id(), ITEMS.register(e.path(),
                    () -> new ImplantItem(e.id(), new Item.Properties().stacksTo(1))));
        }
    }

    /** Ammunition for the Ballistic Module. */
    public static final DeferredItem<Item> FLECHETTE = ITEMS.registerSimpleItem("flechette");

    public static final DeferredItem<BlockItem> IMPLANTATION_RIG =
            ITEMS.registerSimpleBlockItem("implantation_rig", ModBlocks.IMPLANTATION_RIG);
    public static final DeferredItem<BlockItem> KINETIC_CHARGER =
            ITEMS.registerSimpleBlockItem("kinetic_charger", ModBlocks.KINETIC_CHARGER);

    public static final DeferredItem<BlockItem> NEURAL_PORT =
            ITEMS.registerSimpleBlockItem("neural_port", ModBlocks.NEURAL_PORT);

    public static Item itemFor(ResourceLocation typeId) {
        return IMPLANTS.get(typeId).get();
    }

    public static Map<ResourceLocation, DeferredItem<ImplantItem>> implants() {
        return IMPLANTS;
    }

    private ModItems() {
    }
}
