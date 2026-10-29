package dev.cyberimplants.implant;

import dev.cyberimplants.body.Body;
import dev.cyberimplants.body.BodyState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Persisted on/off state of Toggle implants, stored in {@link BodyState#data()} so it syncs and survives logout. */
public final class Toggles {
    public static String key(ResourceLocation id) {
        return "toggle/" + id.getPath();
    }

    /** Works on both sides: the client reads its synced copy. */
    public static boolean isOn(Player player, ResourceLocation id) {
        BodyState s = BodyState.get(player);
        return s.has(id) && s.flag(key(id));
    }

    public static void set(ServerPlayer player, ResourceLocation id, boolean on) {
        if (BodyState.get(player).flag(key(id)) != on) {
            Body.update(player, s -> s.withFlag(key(id), on));
        }
    }

    /** Switches off every toggle whose implant is no longer installed. */
    public static void reconcile(ServerPlayer player) {
        BodyState s = BodyState.get(player);
        for (ImplantCatalog.Entry e : ImplantCatalog.ALL) {
            if (s.flag(key(e.id())) && !s.has(e.id())) {
                Body.update(player, st -> st.withFlag(key(e.id()), false));
                s = BodyState.get(player);
            }
        }
    }

    private Toggles() {
    }
}
