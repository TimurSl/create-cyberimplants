package dev.cyberimplants.implant.optical;

import dev.cyberimplants.fx.ImplantFx;
import dev.cyberimplants.body.Body;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.deck.EntityHandle;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantIds;
import dev.cyberimplants.implant.UseContext;
import dev.cyberimplants.implant.UseResult;
import dev.cyberimplants.network.ScanResultPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Entity Scanner: a pulse that reveals nearby living entities as markers (through walls, via the glow outline) with
 * their type and threat class. Markers show the position at pulse time.
 */
public final class EntityScanner {
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
        int maxMarkers = (int) stats.value("max_markers", 8);
        int revealTicks = (int) stats.value("reveal_ticks", 60);

        List<LivingEntity> found = player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && e.distanceToSqr(player) <= radius * radius);
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        if (found.size() > maxMarkers) {
            found = found.subList(0, maxMarkers);
        }

        List<ScanResultPayload.Marker> markers = new ArrayList<>();
        for (LivingEntity e : found) {
            String kind = EntityKinds.classify(e);
            ImplantFx.scanMark(player, e, kind);
            markers.add(new ScanResultPayload.Marker(e.getId(), kind,
                    BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(), (float) e.distanceTo(player)));
            DeckEvents.fire(player, "enemy_detected", Map.of(
                    "entity", EntityHandle.of(player, e),
                    "distance", (double) e.distanceTo(player),
                    "kind", kind));
        }
        PacketDistributor.sendToPlayer(player, new ScanResultPayload(markers, revealTicks));
        ImplantFx.scanPulse(player, radius, markers.size());
    }

    private EntityScanner() {
    }
}
