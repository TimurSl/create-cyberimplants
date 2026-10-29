package dev.cyberimplants.implant;

import dev.cyberimplants.CyberImplants;
import net.minecraft.resources.ResourceLocation;

/** Registry ids of every implant type that exists in Layer 1. */
public final class ImplantIds {
    public static final ResourceLocation CORE = id("cybernetic_core");

    // A. Neural
    public static final ResourceLocation SANDEVISTAN = id("sandevistan");
    public static final ResourceLocation REFLEX_BOOSTER = id("reflex_booster");
    public static final ResourceLocation COMBAT_PREDICTOR = id("combat_predictor");
    public static final ResourceLocation AES = id("automatic_evasion_system");
    public static final ResourceLocation KERENZIKOV = id("kerenzikov");

    // B. Cyberdeck
    public static final ResourceLocation DECK_CHASSIS = id("deck_chassis");
    public static final ResourceLocation DECK_PROCESSOR = id("deck_processor");
    public static final ResourceLocation DECK_RAM = id("deck_ram");
    public static final ResourceLocation DECK_STORAGE = id("deck_storage");
    public static final ResourceLocation DECK_COPROCESSOR = id("deck_coprocessor");
    public static final ResourceLocation DECK_MACHINE_INTERFACE = id("deck_machine_interface");

    // C. Skeletal & Muscular
    public static final ResourceLocation REINFORCED_SKELETON = id("reinforced_skeleton");
    public static final ResourceLocation POWER_ACTUATORS = id("power_actuators");
    public static final ResourceLocation KINETIC_TENDONS = id("kinetic_tendons");
    public static final ResourceLocation MICRO_ACTUATORS = id("micro_actuators");
    public static final ResourceLocation IMPACT_DAMPENERS = id("impact_dampeners");
    public static final ResourceLocation INERTIA_COMPENSATION = id("inertia_compensation");

    // D. Limbs
    public static final ResourceLocation CYBERNETIC_ARM = id("cybernetic_arm");
    public static final ResourceLocation REMOTE_MANIPULATOR = id("remote_manipulator");
    public static final ResourceLocation RETRACTABLE_BLADE = id("retractable_blade");
    public static final ResourceLocation MICROGRIP = id("microgrip");
    public static final ResourceLocation BALLISTIC_MODULE = id("ballistic_module");

    // E. Optical
    public static final ResourceLocation ENTITY_SCANNER = id("entity_scanner");
    public static final ResourceLocation THERMAL_VISION = id("thermal_vision");
    public static final ResourceLocation MACHINE_ANALYZER = id("machine_analyzer");
    public static final ResourceLocation BALLISTIC_VISOR = id("ballistic_visor");
    public static final ResourceLocation OPTICAL_CAMOUFLAGE = id("optical_camouflage");
    public static final ResourceLocation TARGET_TRACKING = id("target_tracking");

    // F. Circulatory
    public static final ResourceLocation BIOMONITOR = id("biomonitor");
    public static final ResourceLocation REGENERATOR = id("regenerator");
    public static final ResourceLocation ADRENALINE_PUMP = id("adrenaline_pump");
    public static final ResourceLocation DEFIBRILLATOR = id("emergency_defibrillator");
    public static final ResourceLocation ENERGY_BUFFER = id("energy_buffer");
    public static final ResourceLocation ERS = id("emergency_response_system");

    // G. Subdermal
    public static final ResourceLocation ARMOR_PLATING = id("armor_plating");
    public static final ResourceLocation NANOCOATING = id("nanocoating");
    public static final ResourceLocation SHOCK_CIRCUIT = id("shock_circuit");
    public static final ResourceLocation KINETIC_SHIELD = id("kinetic_shield");
    public static final ResourceLocation EMERGENCY_SHELL = id("emergency_shell");

    // H. Mobility
    public static final ResourceLocation ENHANCED_TENDONS = id("enhanced_tendons");
    public static final ResourceLocation HYDRAULIC_LEGS = id("hydraulic_legs");
    public static final ResourceLocation SILENT_ACTUATORS = id("silent_actuators");
    public static final ResourceLocation MAGNETIC_SOLES = id("magnetic_soles");
    public static final ResourceLocation INERTIAL_STABILIZER = id("inertial_stabilizer");
    public static final ResourceLocation MOMENTUM_BOOSTER = id("momentum_booster");

    private static ResourceLocation id(String path) {
        return CyberImplants.id(path);
    }

    private ImplantIds() {
    }
}
