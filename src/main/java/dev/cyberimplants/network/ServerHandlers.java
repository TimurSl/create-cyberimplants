package dev.cyberimplants.network;

import dev.cyberimplants.body.BodyOps;
import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodySync;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.compat.create.block.RigBlockEntity;
import dev.cyberimplants.deck.DeckService;
import dev.cyberimplants.implant.ImplantActions;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.limbs.BallisticModule;
import dev.cyberimplants.implant.mobility.EnhancedTendons;
import dev.cyberimplants.implant.mobility.HydraulicLegs;
import dev.cyberimplants.implant.mobility.MagneticSoles;
import dev.cyberimplants.implant.neural.Kerenzikov;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side handlers for client payloads. Everything the client sends is untrusted and validated here. */
final class ServerHandlers {
    /** At most this many client payloads per player per second. */
    private static final int MAX_PER_SECOND = 120;
    private static final Map<UUID, long[]> WINDOWS = new ConcurrentHashMap<>();

    static boolean allow(ServerPlayer player) {
        long now = player.level().getGameTime();
        long[] window = WINDOWS.computeIfAbsent(player.getUUID(), id -> new long[]{now, 0});
        if (now - window[0] >= 20) {
            window[0] = now;
            window[1] = 0;
        }
        return ++window[1] <= MAX_PER_SECOND;
    }

    static void useImplant(ServerPlayer player, UseImplantPayload payload) {
        if (ImplantActions.wheelEligible(payload.implant())) {
            ImplantActions.use(player, payload.implant(), UseContext.MANUAL);
        }
    }

    static void movement(ServerPlayer player, MovementAbilityPayload payload) {
        boolean ok = switch (payload.kind()) {
            case DODGE -> Kerenzikov.dodge(player, payload.a(), payload.b());
            case AIR_JUMP -> EnhancedTendons.airJump(player);
            case LEAP -> HydraulicLegs.leap(player, payload.a());
            case SOLES_ATTACHED -> {
                MagneticSoles.reportAttached(player);
                yield true;
            }
            case SOLES_DETACHED -> {
                MagneticSoles.detach(player);
                yield true;
            }
        };
        if (!ok) {
            // The client already applied the ability; tell it where the server thinks it stands.
            BodySync.markDirty(player);
        }
    }

    static void rigAction(ServerPlayer player, RigActionPayload payload) {
        if (!RigBlockEntity.canOperate(player, payload.rig())) {
            player.displayClientMessage(Component.translatable("cyberimplants.rig.no_power"), true);
            return;
        }
        BodyRegion[] regions = BodyRegion.values();
        if (payload.region() < 0 || payload.region() >= regions.length
                || payload.index() < 0 || payload.index() >= regions[payload.region()].slots()) {
            return;
        }
        SlotId slot = new SlotId(regions[payload.region()], payload.index());
        BodyOps.Result result;
        if (payload.install()) {
            int inv = payload.inventorySlot();
            if (inv < 0 || inv >= player.getInventory().getContainerSize()) {
                return;
            }
            ItemStack stack = player.getInventory().getItem(inv);
            result = BodyOps.install(player, slot, stack);
        } else {
            result = BodyOps.remove(player, slot);
        }
        if (result.ok()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.PLAYERS, 1.0f, 0.8f);
        } else {
            player.displayClientMessage(Component.translatable(result.translationKey()), true);
        }
        BodySync.markDirty(player);
    }

    static void uploadProgram(ServerPlayer player, UploadProgramPayload payload) {
        DeckService.upload(player, payload.source());
    }

    static void deckCommand(ServerPlayer player, DeckCommandPayload payload) {
        switch (payload.op()) {
            case STATUS -> DeckService.sendStatus(player, "");
            case LOAD -> DeckService.loadCommand(player, payload.program());
            case UNLOAD -> DeckService.unloadCommand(player);
            case DELETE -> DeckService.deleteCommand(player, payload.program());
        }
    }

    static void macroKey(ServerPlayer player, DeckMacroKeyPayload payload) {
        DeckService.macroKey(player, payload.key());
    }

    static void fireBallistic(ServerPlayer player) {
        BallisticModule.fire(player);
    }

    private ServerHandlers() {
    }
}
