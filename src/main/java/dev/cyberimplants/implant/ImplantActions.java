package dev.cyberimplants.implant;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.Strain;
import dev.cyberimplants.deck.DeckService;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.implant.circulatory.Regenerator;
import dev.cyberimplants.implant.limbs.RemoteManipulator;
import dev.cyberimplants.implant.limbs.RetractableBlade;
import dev.cyberimplants.implant.mobility.MagneticSoles;
import dev.cyberimplants.implant.mobility.MomentumBooster;
import dev.cyberimplants.implant.neural.Sandevistan;
import dev.cyberimplants.implant.optical.EntityScanner;
import dev.cyberimplants.implant.optical.OpticalCamouflage;
import dev.cyberimplants.implant.optical.TargetTracking;
import dev.cyberimplants.implant.optical.ThermalVision;
import dev.cyberimplants.implant.subdermal.EmergencyShell;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

/** Dispatch for Active and Toggle implants: the Implant Wheel, Synapse and the ERS all go through here. */
public final class ImplantActions {
    private static final Map<ResourceLocation, ImplantAction> ACTIONS = Map.ofEntries(
            Map.entry(ImplantIds.SANDEVISTAN, Sandevistan.ACTION),
            Map.entry(ImplantIds.ENTITY_SCANNER, EntityScanner.ACTION),
            Map.entry(ImplantIds.THERMAL_VISION, ThermalVision.ACTION),
            Map.entry(ImplantIds.OPTICAL_CAMOUFLAGE, OpticalCamouflage.ACTION),
            Map.entry(ImplantIds.TARGET_TRACKING, TargetTracking.ACTION),
            Map.entry(ImplantIds.REMOTE_MANIPULATOR, RemoteManipulator.ACTION),
            Map.entry(ImplantIds.RETRACTABLE_BLADE, RetractableBlade.ACTION),
            Map.entry(ImplantIds.MAGNETIC_SOLES, MagneticSoles.ACTION),
            Map.entry(ImplantIds.MOMENTUM_BOOSTER, MomentumBooster.ACTION),
            Map.entry(ImplantIds.REGENERATOR, Regenerator.BURST),
            Map.entry(ImplantIds.EMERGENCY_SHELL, EmergencyShell.MANUAL));

    /**
     * Actions of Passive or Triggered implants that only exist once the installed tier has the unlock row
     * (scripts/unlocks.py, ACTION_UNLOCKS): Regenerator Burst and the manual Emergency Shell (Layer 5).
     */
    private static final Map<ResourceLocation, String> ACTION_UNLOCKS = Map.of(
            ImplantIds.REGENERATOR, "burst",
            ImplantIds.EMERGENCY_SHELL, "manual_activation");

    public static ImplantAction get(ResourceLocation id) {
        return ACTIONS.get(id);
    }

    /**
     * Implants the Implant Wheel lists at this installed tier: Active and Toggle types with an action, and implants whose
     * action comes with an unlock the tier has. Tier 0 means not installed.
     */
    public static boolean wheelEligible(ResourceLocation id, int tier) {
        ImplantType type = ImplantCatalog.get(id);
        if (type == null || tier <= 0 || !ACTIONS.containsKey(id)) {
            return false;
        }
        String unlock = ACTION_UNLOCKS.get(id);
        if (unlock != null) {
            return Stats.unlocked(id, tier, unlock);
        }
        return type.activation() == ActivationType.ACTIVE || type.activation() == ActivationType.TOGGLE;
    }

    /** Whether the action of an installed implant is available at its tier (unlock-gated actions need their row). */
    public static boolean actionAvailable(ResourceLocation id, int tier) {
        String unlock = ACTION_UNLOCKS.get(id);
        return ACTIONS.containsKey(id) && (unlock == null || Stats.unlocked(id, tier, unlock));
    }

    /** Uses an implant for a player, enforcing install and scripting rules. */
    public static UseResult use(ServerPlayer player, ResourceLocation id, UseContext context) {
        ImplantType type = ImplantCatalog.get(id);
        ImplantAction action = ACTIONS.get(id);
        int tier = BodyState.get(player).tierOf(id);
        if (type == null || action == null || tier == 0) {
            return UseResult.MISSING;
        }
        if (!actionAvailable(id, tier)) {
            return UseResult.DENIED;
        }
        if (context.scripted() && !type.activation().scriptable()) {
            return UseResult.DENIED;
        }
        if (Strain.offline(player, id)) {
            return UseResult.OFFLINE;
        }
        UseResult result = action.use(player, context);
        DeckService.recordResult(player, result);
        if (!result.ok() && !context.scripted() && !context.emergency()) {
            fizzle(player, id, result);
        }
        return result;
    }

    public static void fizzle(ServerPlayer player, ResourceLocation id, UseResult result) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.4f);
        ImplantFx.fizzle(player);
        player.displayClientMessage(Component.translatable("item." + id.getNamespace() + "." + id.getPath())
                .append(": ").append(Component.translatable(result.translationKey())), true);
    }

    private ImplantActions() {
    }
}
