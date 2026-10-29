package dev.cyberimplants.implant;

import dev.cyberimplants.body.BodyRegion;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.cyberimplants.body.BodyRegion.*;
import static dev.cyberimplants.implant.ActivationType.*;
import static dev.cyberimplants.implant.ImplantCategory.*;
import static dev.cyberimplants.implant.ImplantFlag.*;
import static dev.cyberimplants.implant.ImplantIds.*;

/**
 * Every implant type of Layer 1: 44 of the catalog's 47 types. The Coprocessor, Machine Interface and Remote Sensor
 * Uplink modules arrive with Layers 2 and 3. Registration, items, recipes and language keys all derive from this list.
 */
public final class ImplantCatalog {
    public record Entry(ResourceLocation id, ImplantType type) {
        public String path() {
            return id.getPath();
        }
    }

    public static final List<Entry> ALL = List.of(
            e(ImplantIds.CORE, ImplantType.of(ImplantCategory.CORE, BodyRegion.CORE, PASSIVE)),

            e(SANDEVISTAN, ImplantType.of(NEURAL, CORTEX, ACTIVE)),
            e(REFLEX_BOOSTER, ImplantType.of(NEURAL, CORTEX, PASSIVE)),
            e(COMBAT_PREDICTOR, ImplantType.of(NEURAL, CORTEX, PASSIVE)),
            e(AES, ImplantType.of(NEURAL, CORTEX, TRIGGERED)),
            e(KERENZIKOV, ImplantType.of(NEURAL, CORTEX, INPUT_BOUND)),

            e(DECK_CHASSIS, ImplantType.of(CYBERDECK, SPINE, PASSIVE)),
            e(DECK_PROCESSOR, ImplantType.of(CYBERDECK, DECK, PASSIVE, DECK_MODULE)),
            e(DECK_RAM, ImplantType.of(CYBERDECK, DECK, PASSIVE, DECK_MODULE)),
            e(DECK_STORAGE, ImplantType.of(CYBERDECK, DECK, PASSIVE, DECK_MODULE)),

            e(REINFORCED_SKELETON, ImplantType.of(SKELETAL, SKELETON, PASSIVE)),
            e(POWER_ACTUATORS, ImplantType.of(SKELETAL, SKELETON, PASSIVE)),
            e(KINETIC_TENDONS, ImplantType.of(SKELETAL, SKELETON, PASSIVE)),
            e(MICRO_ACTUATORS, ImplantType.of(SKELETAL, SKELETON, PASSIVE)),
            e(IMPACT_DAMPENERS, ImplantType.of(SKELETAL, SKELETON, PASSIVE)),
            e(INERTIA_COMPENSATION, ImplantType.of(SKELETAL, SKELETON, PASSIVE)),

            e(CYBERNETIC_ARM, ImplantType.of(LIMBS, LEFT_ARM, PASSIVE, ARM_FRAME)),
            e(REMOTE_MANIPULATOR, ImplantType.of(LIMBS, LEFT_SOCKET, ACTIVE, ARM_MODULE, PNEUMATIC)),
            e(RETRACTABLE_BLADE, ImplantType.of(LIMBS, LEFT_SOCKET, TOGGLE, ARM_MODULE)),
            e(MICROGRIP, ImplantType.of(LIMBS, LEFT_SOCKET, INPUT_BOUND, ARM_MODULE)),
            e(BALLISTIC_MODULE, ImplantType.of(LIMBS, LEFT_SOCKET, INPUT_BOUND, ARM_MODULE, PNEUMATIC)),

            e(ENTITY_SCANNER, ImplantType.of(OPTICAL, OCULAR, ACTIVE)),
            e(THERMAL_VISION, ImplantType.of(OPTICAL, OCULAR, TOGGLE)),
            e(MACHINE_ANALYZER, ImplantType.of(OPTICAL, OCULAR, PASSIVE)),
            e(BALLISTIC_VISOR, ImplantType.of(OPTICAL, OCULAR, PASSIVE)),
            e(OPTICAL_CAMOUFLAGE, ImplantType.of(OPTICAL, OCULAR, ACTIVE)),
            e(TARGET_TRACKING, ImplantType.of(OPTICAL, OCULAR, ACTIVE)),

            e(BIOMONITOR, ImplantType.of(ImplantCategory.CIRCULATORY, BodyRegion.CIRCULATORY, PASSIVE)),
            e(REGENERATOR, ImplantType.of(ImplantCategory.CIRCULATORY, BodyRegion.CIRCULATORY, PASSIVE)),
            e(ADRENALINE_PUMP, ImplantType.of(ImplantCategory.CIRCULATORY, BodyRegion.CIRCULATORY, TRIGGERED)),
            e(DEFIBRILLATOR, ImplantType.of(ImplantCategory.CIRCULATORY, BodyRegion.CIRCULATORY, TRIGGERED, EMERGENCY)),
            e(ENERGY_BUFFER, ImplantType.of(ImplantCategory.CIRCULATORY, BodyRegion.CIRCULATORY, PASSIVE)),
            e(ERS, ImplantType.of(ImplantCategory.CIRCULATORY, BodyRegion.CIRCULATORY, TRIGGERED, EMERGENCY)),

            e(ARMOR_PLATING, ImplantType.of(SUBDERMAL, INTEGUMENT, PASSIVE)),
            e(NANOCOATING, ImplantType.of(SUBDERMAL, INTEGUMENT, PASSIVE)),
            e(SHOCK_CIRCUIT, ImplantType.of(SUBDERMAL, INTEGUMENT, TRIGGERED)),
            e(KINETIC_SHIELD, ImplantType.of(SUBDERMAL, INTEGUMENT, PASSIVE, EMERGENCY)),
            e(EMERGENCY_SHELL, ImplantType.of(SUBDERMAL, INTEGUMENT, TRIGGERED, EMERGENCY)),

            e(ENHANCED_TENDONS, ImplantType.of(MOBILITY, LEGS, INPUT_BOUND)),
            e(HYDRAULIC_LEGS, ImplantType.of(MOBILITY, LEGS, INPUT_BOUND, PNEUMATIC)),
            e(SILENT_ACTUATORS, ImplantType.of(MOBILITY, LEGS, PASSIVE)),
            e(MAGNETIC_SOLES, ImplantType.of(MOBILITY, LEGS, TOGGLE)),
            e(INERTIAL_STABILIZER, ImplantType.of(MOBILITY, LEGS, PASSIVE)),
            e(MOMENTUM_BOOSTER, ImplantType.of(MOBILITY, LEGS, ACTIVE)));

    private static final Map<ResourceLocation, ImplantType> BY_ID =
            ALL.stream().collect(Collectors.toUnmodifiableMap(Entry::id, Entry::type));

    private static Entry e(ResourceLocation id, ImplantType type) {
        return new Entry(id, type);
    }

    /** The type for an id, or null. Works before registries freeze, on both sides. */
    public static ImplantType get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    /** Regions an implant of this type may be placed in. */
    public static List<BodyRegion> regionsFor(ImplantType type) {
        if (type.has(ARM_FRAME)) {
            return List.of(LEFT_ARM, RIGHT_ARM);
        }
        if (type.has(ARM_MODULE)) {
            return List.of(LEFT_SOCKET, RIGHT_SOCKET);
        }
        return List.of(type.region());
    }

    private ImplantCatalog() {
    }
}
