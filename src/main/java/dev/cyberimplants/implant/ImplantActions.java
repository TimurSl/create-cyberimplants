package dev.cyberimplants.implant;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.Strain;
import dev.cyberimplants.deck.DeckService;
import dev.cyberimplants.implant.limbs.RemoteManipulator;
import dev.cyberimplants.implant.limbs.RetractableBlade;
import dev.cyberimplants.implant.mobility.MagneticSoles;
import dev.cyberimplants.implant.mobility.MomentumBooster;
import dev.cyberimplants.implant.neural.Sandevistan;
import dev.cyberimplants.implant.optical.EntityScanner;
import dev.cyberimplants.implant.optical.OpticalCamouflage;
import dev.cyberimplants.implant.optical.TargetTracking;
import dev.cyberimplants.implant.optical.ThermalVision;
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
            Map.entry(ImplantIds.MOMENTUM_BOOSTER, MomentumBooster.ACTION));

    public static ImplantAction get(ResourceLocation id) {
        return ACTIONS.get(id);
    }

    /** Implants the Implant Wheel lists: Active and Toggle types that are installed and have an action. */
    public static boolean wheelEligible(ResourceLocation id) {
        ImplantType type = ImplantCatalog.get(id);
        return type != null && ACTIONS.containsKey(id)
                && (type.activation() == ActivationType.ACTIVE || type.activation() == ActivationType.TOGGLE);
    }

    /** Uses an implant for a player, enforcing install and scripting rules. */
    public static UseResult use(ServerPlayer player, ResourceLocation id, UseContext context) {
        ImplantType type = ImplantCatalog.get(id);
        ImplantAction action = ACTIONS.get(id);
        if (type == null || action == null || !BodyState.get(player).has(id)) {
            return UseResult.MISSING;
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
