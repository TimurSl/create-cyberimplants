package dev.cyberimplants.registry;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One item per implant type (the tier is a component), the shared production components, ammunition and the machine
 * block items. Fluid buckets are registered by {@link ModFluids}.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CyberImplants.MOD_ID);

    private static final Map<ResourceLocation, DeferredItem<ImplantItem>> IMPLANTS = new LinkedHashMap<>();

    static {
        for (ImplantCatalog.Entry e : ImplantCatalog.ALL) {
            IMPLANTS.put(e.id(), ITEMS.register(e.path(),
                    () -> new ImplantItem(e.id(), new Item.Properties().stacksTo(1))));
        }
    }

    /**
     * Shared production components of the five tiers (docs/design/06-progression-and-crafting.md, section 6.3), in chain
     * order. Recipes, textures and names come from scripts/crafting.py.
     */
    public static final List<String> COMPONENT_IDS = List.of(
            "copper_wire", "synthetic_fiber", "mechanical_servo", "civilian_casing",
            "crude_steel_blend", "tempered_steel_ingot", "tempered_steel_sheet", "precision_servo", "synthetic_muscle",
            "silica_dust", "silicon_boule", "silicon_wafer", "logic_chip", "myomer_bundle", "inked_steel_sheet",
            "ground_lens", "phase_lens",
            "composite_plate", "military_processor",
            "echo_lattice", "experimental_core", "quantum_substrate");

    /** Transitional items of the component sequenced assemblies. */
    public static final List<String> INCOMPLETE_COMPONENT_IDS = List.of(
            "incomplete_precision_servo", "incomplete_logic_chip", "incomplete_composite_plate",
            "incomplete_military_processor", "incomplete_experimental_core");

    private static final List<DeferredItem<Item>> COMPONENTS = new ArrayList<>();

    static {
        COMPONENT_IDS.forEach(id -> COMPONENTS.add(ITEMS.registerSimpleItem(id)));
        INCOMPLETE_COMPONENT_IDS.forEach(id -> ITEMS.register(id,
                () -> new dev.cyberimplants.compat.create.IncompleteImplantItem(new Item.Properties())));
    }

    /** Ammunition for the Ballistic Module. */
    public static final DeferredItem<Item> FLECHETTE = ITEMS.registerSimpleItem("flechette");

    /** The transitional item of every upgrade sequenced assembly. */
    public static final DeferredItem<Item> INCOMPLETE_IMPLANT = ITEMS.register("incomplete_implant",
            () -> new dev.cyberimplants.compat.create.IncompleteImplantItem(new Item.Properties().stacksTo(1)));

    /** The in-game guide (also on a key). Given once to new players unless {@code manual.giveOnFirstJoin} is off. */
    public static final DeferredItem<Item> FIELD_MANUAL = ITEMS.register("field_manual",
            () -> new dev.cyberimplants.content.FieldManualItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> NEURO_STABILIZER = ITEMS.register("neuro_stabilizer",
            () -> new dev.cyberimplants.implant.NeuroStabilizerItem(new Item.Properties().stacksTo(16)));

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

    public static List<DeferredItem<Item>> components() {
        return COMPONENTS;
    }

    private ModItems() {
    }
}
