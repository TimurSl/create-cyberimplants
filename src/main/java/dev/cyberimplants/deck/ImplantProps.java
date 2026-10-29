package dev.cyberimplants.deck;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.Toggles;
import dev.cyberimplants.implant.circulatory.EnergyBuffer;
import dev.cyberimplants.implant.circulatory.Ers;
import dev.cyberimplants.implant.circulatory.Regenerator;
import dev.cyberimplants.implant.limbs.BallisticModule;
import dev.cyberimplants.implant.mobility.MagneticSoles;
import dev.cyberimplants.implant.mobility.MomentumBooster;
import dev.cyberimplants.implant.neural.Aes;
import dev.cyberimplants.implant.neural.Kerenzikov;
import dev.cyberimplants.implant.neural.Sandevistan;
import dev.cyberimplants.implant.optical.OpticalCamouflage;
import dev.cyberimplants.implant.optical.TargetTracking;
import dev.cyberimplants.implant.subdermal.EmergencyShell;
import dev.cyberimplants.implant.subdermal.KineticShield;
import dev.cyberimplants.synapse.compiler.Ticks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Implant-specific script members (the "Deck" line of each catalog entry). Unknown members read as NULL. */
final class ImplantProps {
    static Object get(ServerPlayer player, ResourceLocation type, String member) {
        BodyState s = BodyState.get(player);
        if (type.equals(ImplantIds.SANDEVISTAN)) {
            return switch (member) {
                case "radius_max" -> Sandevistan.radiusMax(player);
                case "duration_max" -> new Ticks(Sandevistan.durationMax(player));
                default -> null;
            };
        }
        if (type.equals(ImplantIds.AES)) {
            return switch (member) {
                case "charges" -> (double) Aes.charges(player);
                case "enabled" -> Aes.enabled(player);
                default -> null;
            };
        }
        if (type.equals(ImplantIds.KERENZIKOV)) {
            return member.equals("window_active") ? Kerenzikov.windowActive(player) : null;
        }
        if (type.equals(ImplantIds.THERMAL_VISION)) {
            return member.equals("active") ? Toggles.isOn(player, type) : null;
        }
        if (type.equals(ImplantIds.OPTICAL_CAMOUFLAGE)) {
            return member.equals("active") ? OpticalCamouflage.active(player) : null;
        }
        if (type.equals(ImplantIds.TARGET_TRACKING)) {
            if (member.equals("target")) {
                Entity target = TargetTracking.target(player);
                return target == null ? null : EntityHandle.of(player, target);
            }
            return null;
        }
        if (type.equals(ImplantIds.RETRACTABLE_BLADE)) {
            return member.equals("deployed") ? Toggles.isOn(player, type) : null;
        }
        if (type.equals(ImplantIds.BALLISTIC_MODULE)) {
            return switch (member) {
                case "magazine" -> (double) BallisticModule.magazine(player);
                case "ammo" -> "flechette";
                default -> null;
            };
        }
        if (type.equals(ImplantIds.REINFORCED_SKELETON)) {
            return member.equals("tier") ? (double) s.tierOf(type) : null;
        }
        if (type.equals(ImplantIds.BIOMONITOR)) {
            return switch (member) {
                case "health" -> (double) player.getHealth();
                case "max_health" -> (double) player.getMaxHealth();
                case "health_pct" -> player.getHealth() / player.getMaxHealth() * 100.0;
                case "hunger" -> (double) player.getFoodData().getFoodLevel();
                case "saturation" -> (double) player.getFoodData().getSaturationLevel();
                default -> null;
            };
        }
        if (type.equals(ImplantIds.REGENERATOR)) {
            return member.equals("healing") ? Regenerator.healing(player) : null;
        }
        if (type.equals(ImplantIds.ENERGY_BUFFER)) {
            return switch (member) {
                case "charge" -> s.buffer();
                case "capacity" -> EnergyBuffer.capacity(player);
                default -> null;
            };
        }
        if (type.equals(ImplantIds.ERS)) {
            return switch (member) {
                case "enabled" -> Ers.enabled(player);
                case "danger" -> Ers.danger(player);
                default -> null;
            };
        }
        if (type.equals(ImplantIds.KINETIC_SHIELD)) {
            return switch (member) {
                case "value" -> KineticShield.value(player);
                case "max" -> KineticShield.max(player);
                default -> null;
            };
        }
        if (type.equals(ImplantIds.EMERGENCY_SHELL)) {
            return switch (member) {
                case "active" -> EmergencyShell.active(player);
                default -> null;
            };
        }
        if (type.equals(ImplantIds.MAGNETIC_SOLES)) {
            return switch (member) {
                case "active" -> Toggles.isOn(player, type);
                case "attached" -> MagneticSoles.attached(player);
                default -> null;
            };
        }
        if (type.equals(ImplantIds.MOMENTUM_BOOSTER)) {
            return member.equals("value") ? MomentumBooster.value(player) : null;
        }
        if (type.equals(ImplantIds.DEFIBRILLATOR) || type.equals(ImplantIds.HYDRAULIC_LEGS)) {
            return null; // ready / cooldown are the common members
        }
        return Stats.of(type, s.tierOf(type)).values().containsKey(member) ? Stats.of(type, s.tierOf(type)).value(member, 0) : null;
    }

    /** Writable properties. Returns false for anything else. */
    static boolean set(ServerPlayer player, ResourceLocation type, String member, Object value) {
        if (!(value instanceof Boolean on)) {
            return false;
        }
        if (type.equals(ImplantIds.AES) && member.equals("enabled")) {
            Body.update(player, s -> s.withFlag(Aes.DISABLED_KEY, !on));
            return true;
        }
        if (type.equals(ImplantIds.ERS) && member.equals("enabled")) {
            Body.update(player, s -> s.withFlag(Ers.DISABLED_KEY, !on));
            return true;
        }
        if ((type.equals(ImplantIds.THERMAL_VISION) && member.equals("active"))
                || (type.equals(ImplantIds.RETRACTABLE_BLADE) && member.equals("deployed"))
                || (type.equals(ImplantIds.MAGNETIC_SOLES) && member.equals("active"))) {
            dev.cyberimplants.implant.ImplantAction action = dev.cyberimplants.implant.ImplantActions.get(type);
            if (action != null && action.isActive(player) != on) {
                dev.cyberimplants.implant.ImplantActions.use(player, type, dev.cyberimplants.implant.UseContext.scripted(null, java.util.Map.of()));
            }
            return true;
        }
        return false;
    }

    private ImplantProps() {
    }
}
