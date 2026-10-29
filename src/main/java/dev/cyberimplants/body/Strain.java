package dev.cyberimplants.body;

import dev.cyberimplants.CyberImplants;
import dev.cyberimplants.deck.DeckEvents;
import dev.cyberimplants.implant.ImplantAction;
import dev.cyberimplants.implant.ImplantActions;
import dev.cyberimplants.implant.ImplantCatalog;
import dev.cyberimplants.implant.Toggles;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Neural Strain (docs/design/01-core-systems.md, section 1.3). While total Load exceeds Capacity, every 60-120 s a
 * glitch fires: one random Active or Toggle implant goes offline for 3-5 s, or the HUD just flickers. A
 * Neuro-Stabilizer suppresses the glitches for ten minutes. The +20% Charge cost is in {@link Body#chargeMultiplier}.
 */
@EventBusSubscriber(modid = CyberImplants.MOD_ID)
public final class Strain {
    private static final String OFFLINE = "offline/";
    private static final String STABILIZED_UNTIL = "stabilized_until";
    public static final int STABILIZER_TICKS = 12_000;

    private static final Map<UUID, Long> NEXT_GLITCH = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> LAST_STATE = new ConcurrentHashMap<>();

    /** Works on both sides (the client reads its synced body). */
    public static boolean offline(Player player, ResourceLocation implant) {
        BodyState s = BodyState.get(player);
        return s.number(OFFLINE + implant.getPath(), 0) > s.onlineTicks();
    }

    public static boolean stabilized(BodyState s) {
        return s.number(STABILIZED_UNTIL, 0) > s.onlineTicks();
    }

    /** Called by the Neuro-Stabilizer item. */
    public static void stabilize(ServerPlayer player) {
        Body.update(player, s -> s.withData(STABILIZED_UNTIL, s.onlineTicks() + STABILIZER_TICKS));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        BodyState s = BodyState.get(player);
        boolean strained = Body.strained(s);
        Boolean before = LAST_STATE.put(player.getUUID(), strained);
        if (before != null && before != strained) {
            DeckEvents.fire(player, "strain_changed", Map.of("state", strained ? "strained" : "stable"));
        }
        if (!strained) {
            NEXT_GLITCH.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        Long due = NEXT_GLITCH.get(player.getUUID());
        if (due == null) {
            NEXT_GLITCH.put(player.getUUID(), now + interval(player.getRandom()));
        } else if (now >= due) {
            if (!stabilized(s)) {
                glitch(player);
            }
            NEXT_GLITCH.put(player.getUUID(), now + interval(player.getRandom()));
        }
    }

    private static long interval(RandomSource random) {
        return 20L * (60 + random.nextInt(61));
    }

    private static void glitch(ServerPlayer player) {
        BodyState s = BodyState.get(player);
        List<ResourceLocation> candidates = new ArrayList<>();
        for (ImplantCatalog.Entry e : ImplantCatalog.ALL) {
            if (s.has(e.id()) && ImplantActions.get(e.id()) != null) {
                candidates.add(e.id());
            }
        }
        RandomSource random = player.getRandom();
        if (candidates.isEmpty() || random.nextBoolean()) {
            player.displayClientMessage(Component.translatable("cyberimplants.strain.static"), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.8f);
            return;
        }
        ResourceLocation id = candidates.get(random.nextInt(candidates.size()));
        long end = s.onlineTicks() + 20L * (3 + random.nextInt(3));
        ImplantAction action = ImplantActions.get(id);
        if (action.isActive(player)) {
            action.stop(player);
        }
        Toggles.set(player, id, false);
        Body.update(player, st -> st.withData(OFFLINE + id.getPath(), end));
        player.displayClientMessage(Component.translatable("cyberimplants.strain.glitch",
                Component.translatable("item." + id.getNamespace() + "." + id.getPath())), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT_GLITCH.remove(event.getEntity().getUUID());
        LAST_STATE.remove(event.getEntity().getUUID());
    }

    private Strain() {
    }
}
