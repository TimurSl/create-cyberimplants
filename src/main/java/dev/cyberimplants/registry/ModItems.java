package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
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

    /** The transitional item of every upgrade sequenced assembly. */
    public static final DeferredItem<Item> INCOMPLETE_IMPLANT = ITEMS.register("incomplete_implant",
            () -> new dev.cyberimplants.compat.create.IncompleteImplantItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> NEURO_STABILIZER = ITEMS.register("neuro_stabilizer",
            () -> new dev.cyberimplants.implant.NeuroStabilizerItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BIO_GEL_BUCKET = ITEMS.register("bio_interface_gel_bucket",
            () -> new BucketItem(ModFluids.BIO_GEL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredItem<Item> NEURAL_GEL_BUCKET = ITEMS.register("neural_gel_bucket",
            () -> new BucketItem(ModFluids.NEURAL_GEL.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<BlockItem> IMPLANTATION_RIG =
            ITEMS.registerSimpleBlockItem("implantation_rig", ModBlocks.IMPLANTATION_RIG);
    public static final DeferredItem<BlockItem> KINETIC_CHARGER =
            ITEMS.registerSimpleBlockItem("kinetic_charger", ModBlocks.KINETIC_CHARGER);

    public static final DeferredItem<BlockItem> PROXIMITY_SENSOR =
            ITEMS.registerSimpleBlockItem("proximity_sensor", ModBlocks.PROXIMITY_SENSOR);

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
