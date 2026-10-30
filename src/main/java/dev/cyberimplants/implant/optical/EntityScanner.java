package dev.cyberimplants.implant.optical;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import dev.cyberimplants.body.InstalledImplant;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.deck.EntityHandle;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.ImplantCategory;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.ImplantType;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import dev.cyberimplants.implant.mobility.SilentActuators;
import dev.cyberimplants.network.ScanResultPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity Scanner: a pulse that reveals nearby living entities as markers (through walls, via the glow outline) with
 * their type and threat class. Markers show the position at pulse time. Tier III auto-scan (Layer 3) pulses for free
 * at half radius; it reports each entity once while it stays in range and skips Ghost-step users who stand still.
 * Tier V implant reading (Layer 5) lists other players' implant categories and breaks Optical Camouflage up to Tier IV.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class EntityScanner {
    /** Entities the last auto-scan revealed, per scanning player, so {@code enemy_detected} fires once per reveal. */
    private static final Map<UUID, Set<UUID>> AUTO_SEEN = new ConcurrentHashMap<>();

    public static final ImplantAction ACTION = new ImplantAction() {
        @Override
        public UseResult use(ServerPlayer player, UseContext ctx) {
            if (!Body.ready(player, ImplantIds.ENTITY_SCANNER)) {
                return UseResult.COOLDOWN;
            }
            TierStats stats = Stats.of(player, ImplantIds.ENTITY_SCANNER);
            if (!(ctx.emergency() ? Body.spendEmergency(player, stats.chargeCost()) : Body.spend(player, stats.chargeCost()))) {
                return UseResult.NO_CHARGE;
            }
            Body.startCooldown(player, ImplantIds.ENTITY_SCANNER, stats.cooldownTicks());
            pulse(player, stats);
            return UseResult.OK;
        }
    };

    private static void pulse(ServerPlayer player, TierStats stats) {
        double radius = stats.value("radius", 12);
        int revealTicks = (int) stats.value("reveal_ticks", 60);
        List<LivingEntity> found = scan(player, radius, (int) stats.value("max_markers", 8), false);

        boolean reading = Stats.unlocked(player, ImplantIds.ENTITY_SCANNER, "implant_reading");
        List<ScanResultPayload.Marker> markers = new ArrayList<>();
        for (LivingEntity e : found) {
            String kind = EntityKinds.classify(e);
            ImplantFx.scanMark(player, e, kind);
            markers.add(marker(player, e, kind, reading));
            fireDetected(player, e, kind);
        }
        AUTO_SEEN.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new ScanResultPayload(markers, revealTicks, false));
        ImplantFx.scanPulse(player, radius, markers.size());
    }

    /** Tier III+: every {@code auto_scan_ticks} a free pulse at half radius, without the pulse effects. */
    private static void autoScan(ServerPlayer player, TierStats stats) {
        int interval = (int) stats.value("auto_scan_ticks", 80);
        List<LivingEntity> found = scan(player, stats.value("radius", 12) * 0.5, (int) stats.value("max_markers", 8), true);
        boolean reading = Stats.unlocked(player, ImplantIds.ENTITY_SCANNER, "implant_reading");
        Set<UUID> previous = AUTO_SEEN.getOrDefault(player.getUUID(), Set.of());
        Set<UUID> seen = new HashSet<>();
        List<ScanResultPayload.Marker> markers = new ArrayList<>();
        for (LivingEntity e : found) {
            String kind = EntityKinds.classify(e);
            markers.add(marker(player, e, kind, reading));
            seen.add(e.getUUID());
            if (!previous.contains(e.getUUID())) {
                fireDetected(player, e, kind);
            }
        }
        AUTO_SEEN.put(player.getUUID(), seen);
        PacketDistributor.sendToPlayer(player, new ScanResultPayload(markers, interval + 10, true));
    }

    private static List<LivingEntity> scan(ServerPlayer player, double radius, int maxMarkers, boolean passive) {
        List<LivingEntity> found = new ArrayList<>(player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && e.distanceToSqr(player) <= radius * radius
                        && !(passive && e instanceof ServerPlayer other && SilentActuators.ghostStill(other))));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return found.size() > maxMarkers ? found.subList(0, maxMarkers) : found;
    }

    private static ScanResultPayload.Marker marker(ServerPlayer player, LivingEntity e, String kind, boolean reading) {
        String implants = "";
        if (reading && e instanceof ServerPlayer other) {
            implants = implantLetters(other);
            OpticalCamouflage.revealByScan(other);
        }
        return new ScanResultPayload.Marker(e.getId(), kind,
                BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(), (float) e.distanceTo(player), implants);
    }

    private static void fireDetected(ServerPlayer player, LivingEntity e, String kind) {
        DeckEvents.fire(player, "enemy_detected", Map.of(
                "entity", EntityHandle.of(player, e),
                "distance", (double) e.distanceTo(player),
                "kind", kind));
    }

    /** The design spec's category letters (A Neural ... H Mobility) of everything installed, e.g. "ACEH". */
    public static String implantLetters(Player player) {
        Set<Character> letters = new TreeSet<>();
        for (InstalledImplant implant : BodyState.get(player).installed().values()) {
            ImplantType type = ImplantCatalog.get(implant.type());
            if (type != null && type.category() != ImplantCategory.CORE) {
                letters.add((char) ('A' + type.category().ordinal() - 1));
            }
        }
        StringBuilder out = new StringBuilder();
        letters.forEach(out::append);
        return out.toString();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        int tier = BodyState.get(player).tierOf(ImplantIds.ENTITY_SCANNER);
        if (!Stats.unlocked(ImplantIds.ENTITY_SCANNER, tier, "auto_scan")) {
            AUTO_SEEN.remove(player.getUUID());
            return;
        }
        TierStats stats = Stats.of(ImplantIds.ENTITY_SCANNER, tier);
        int interval = Math.max(20, (int) stats.value("auto_scan_ticks", 80));
        if (player.tickCount % interval == 0) {
            autoScan(player, stats);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AUTO_SEEN.remove(event.getEntity().getUUID());
    }

    private EntityScanner() {
    }
}
