package dev.cyberimplants.implant;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.body.BodyRegion;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.body.SlotId;
import dev.cyberimplants.implant.neural.Sandevistan;
import dev.cyberimplants.network.AppearancePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What other people can see of a body: arm hardware per side, glowing optics, neural ports, a deck along the spine and
 * whether a Sandevistan is running. Packed into an int, sent to the owner and to every player tracking them whenever
 * it changes; the client draws implant models and Sandevistan afterimages from it.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Appearance {
    public static final int FRAME_LEFT = 1;
    public static final int FRAME_RIGHT = 1 << 1;
    public static final int BLADE_LEFT = 1 << 2;
    public static final int BLADE_RIGHT = 1 << 3;
    public static final int BALLISTIC_LEFT = 1 << 4;
    public static final int BALLISTIC_RIGHT = 1 << 5;
    public static final int MANIPULATOR_LEFT = 1 << 6;
    public static final int MANIPULATOR_RIGHT = 1 << 7;
    /** The blade toggle is on; each arm still shows it only while its own hand is empty. */
    public static final int BLADE_DEPLOYED = 1 << 8;
    public static final int CORE = 1 << 9;
    public static final int OPTICS = 1 << 10;
    public static final int NEURAL = 1 << 11;
    public static final int DECK = 1 << 12;
    public static final int MOBILITY = 1 << 13;
    public static final int SUBDERMAL = 1 << 14;
    public static final int SANDEVISTAN = 1 << 15;

    private static final Map<UUID, Integer> LAST_SENT = new ConcurrentHashMap<>();

    public static int compute(Player player) {
        BodyState s = BodyState.get(player);
        if (s.installed().isEmpty()) {
            return 0;
        }
        int bits = 0;
        for (Map.Entry<SlotId, InstalledImplant> e : s.installed().entrySet()) {
            ImplantType type = ImplantCatalog.get(e.getValue().type());
            if (type == null) {
                continue;
            }
            bits |= switch (type.category()) {
                case CORE -> CORE;
                case OPTICAL -> OPTICS;
                case NEURAL -> NEURAL;
                case CYBERDECK -> DECK;
                case MOBILITY -> MOBILITY;
                case SUBDERMAL -> SUBDERMAL;
                default -> 0;
            };
            BodyRegion region = e.getKey().region();
            boolean left = region == BodyRegion.LEFT_ARM || region == BodyRegion.LEFT_SOCKET;
            if (region == BodyRegion.LEFT_ARM || region == BodyRegion.RIGHT_ARM) {
                bits |= left ? FRAME_LEFT : FRAME_RIGHT;
            } else if (region == BodyRegion.LEFT_SOCKET || region == BodyRegion.RIGHT_SOCKET) {
                var id = e.getValue().type();
                if (id.equals(ImplantIds.RETRACTABLE_BLADE)) {
                    bits |= left ? BLADE_LEFT : BLADE_RIGHT;
                } else if (id.equals(ImplantIds.BALLISTIC_MODULE)) {
                    bits |= left ? BALLISTIC_LEFT : BALLISTIC_RIGHT;
                } else if (id.equals(ImplantIds.REMOTE_MANIPULATOR)) {
                    bits |= left ? MANIPULATOR_LEFT : MANIPULATOR_RIGHT;
                }
            }
        }
        if (Toggles.isOn(player, ImplantIds.RETRACTABLE_BLADE)) {
            bits |= BLADE_DEPLOYED;
        }
        if (Sandevistan.isActive(player)) {
            bits |= SANDEVISTAN;
        }
        return bits;
    }

    /** Once per player tick: resend when anything visible changed. */
    public static void tick(ServerPlayer player) {
        int bits = compute(player);
        Integer last = LAST_SENT.put(player.getUUID(), bits);
        if (last == null || last != bits) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new AppearancePayload(player.getId(), bits));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, new AppearancePayload(target.getId(), compute(target)));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    private Appearance() {
    }
}
