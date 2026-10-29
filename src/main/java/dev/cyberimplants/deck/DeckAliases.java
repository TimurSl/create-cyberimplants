package dev.cyberimplants.deck;

import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

/** The identifiers scripts use for implants (docs/design/04-synapse-language.md, section 10.2). */
public final class DeckAliases {
    private static final Map<String, ResourceLocation> ALIASES = Map.ofEntries(
            entry("sandevistan", ImplantIds.SANDEVISTAN),
            entry("reflex", ImplantIds.REFLEX_BOOSTER),
            entry("predict", ImplantIds.COMBAT_PREDICTOR),
            entry("aes", ImplantIds.AES),
            entry("kerenzikov", ImplantIds.KERENZIKOV),
            entry("skeleton", ImplantIds.REINFORCED_SKELETON),
            entry("actuators", ImplantIds.POWER_ACTUATORS),
            entry("tendons", ImplantIds.KINETIC_TENDONS),
            entry("micro", ImplantIds.MICRO_ACTUATORS),
            entry("dampeners", ImplantIds.IMPACT_DAMPENERS),
            entry("inertia", ImplantIds.INERTIA_COMPENSATION),
            entry("left_arm", ImplantIds.CYBERNETIC_ARM),
            entry("right_arm", ImplantIds.CYBERNETIC_ARM),
            entry("manipulator", ImplantIds.REMOTE_MANIPULATOR),
            entry("blade", ImplantIds.RETRACTABLE_BLADE),
            entry("microgrip", ImplantIds.MICROGRIP),
            entry("ballistic", ImplantIds.BALLISTIC_MODULE),
            entry("air_jump", ImplantIds.ENHANCED_TENDONS),
            entry("hydraulics", ImplantIds.HYDRAULIC_LEGS),
            entry("silent", ImplantIds.SILENT_ACTUATORS),
            entry("momentum", ImplantIds.MOMENTUM_BOOSTER),
            entry("scanner", ImplantIds.ENTITY_SCANNER),
            entry("scan", ImplantIds.ENTITY_SCANNER),
            entry("thermal", ImplantIds.THERMAL_VISION),
            entry("analyzer", ImplantIds.MACHINE_ANALYZER),
            entry("ballistics", ImplantIds.BALLISTIC_VISOR),
            entry("camo", ImplantIds.OPTICAL_CAMOUFLAGE),
            entry("tracked", ImplantIds.TARGET_TRACKING),
            entry("tracker", ImplantIds.TARGET_TRACKING),
            entry("bio", ImplantIds.BIOMONITOR),
            entry("regen", ImplantIds.REGENERATOR),
            entry("adrenaline", ImplantIds.ADRENALINE_PUMP),
            entry("defib", ImplantIds.DEFIBRILLATOR),
            entry("buffer", ImplantIds.ENERGY_BUFFER),
            entry("ers", ImplantIds.ERS),
            entry("plating", ImplantIds.ARMOR_PLATING),
            entry("nano", ImplantIds.NANOCOATING),
            entry("shock", ImplantIds.SHOCK_CIRCUIT),
            entry("shield", ImplantIds.KINETIC_SHIELD),
            entry("shell", ImplantIds.EMERGENCY_SHELL),
            entry("soles", ImplantIds.MAGNETIC_SOLES),
            entry("stabilizer", ImplantIds.INERTIAL_STABILIZER));

    public static ResourceLocation resolve(String alias) {
        return ALIASES.get(alias);
    }

    public static Set<String> names() {
        return ALIASES.keySet();
    }

    private DeckAliases() {
    }
}
