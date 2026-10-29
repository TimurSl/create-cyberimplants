package dev.cyberimplants.body;

import dev.cyberimplants.config.Layers;
import dev.cyberimplants.config.ServerConfig;
import dev.cyberimplants.data.Stats;
import dev.cyberimplants.data.TierStats;
import dev.cyberimplants.implant.ImplantIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.UnaryOperator;

/** Server-side rules for Load, Charge and cooldowns (docs/design/01-core-systems.md, sections 1.2-1.4). */
public final class Body {
    // ---------------------------------------------------------------- core and load

    public static boolean hasCore(BodyState s) {
        return s.has(ImplantIds.CORE);
    }

    /** Neural Capacity from the installed Core, 0 without one. */
    public static double capacity(BodyState s) {
        int tier = s.tierOf(ImplantIds.CORE);
        return tier == 0 ? 0 : Stats.of(ImplantIds.CORE, tier).value("capacity", 30);
    }

    public static double overloadMargin(BodyState s) {
        int tier = s.tierOf(ImplantIds.CORE);
        return tier == 0 ? 0 : Stats.of(ImplantIds.CORE, tier).value("overload_margin", 0.10);
    }

    public static double maxCharge(BodyState s) {
        int tier = s.tierOf(ImplantIds.CORE);
        return tier == 0 ? 0 : Stats.of(ImplantIds.CORE, tier).value("battery", 100);
    }

    /** Energy Buffer capacity in C, 0 without one. */
    public static double bufferCapacity(BodyState s) {
        int tier = s.tierOf(ImplantIds.ENERGY_BUFFER);
        return tier == 0 ? 0 : Stats.of(ImplantIds.ENERGY_BUFFER, tier).value("capacity", 50);
    }

    public static double metabolicRate(BodyState s) {
        int tier = s.tierOf(ImplantIds.CORE);
        return tier == 0 ? 0 : Stats.of(ImplantIds.CORE, tier).value("recharge_per_second", 0.25);
    }

    /** Total Load above Capacity: the Strained state of docs/design/01-core-systems.md, section 1.3. Layer 2 and up. */
    public static boolean strained(BodyState s) {
        double capacity = capacity(s);
        return Layers.atLeast(2) && capacity > 0 && totalLoad(s) > capacity;
    }

    /** Strained bodies pay 20% more Charge for everything. */
    public static double chargeMultiplier(BodyState s) {
        return strained(s) ? 1.2 : 1.0;
    }

    public static int loadOf(ResourceLocation type, int tier) {
        return Stats.of(type, tier).load();
    }

    public static int totalLoad(BodyState s) {
        int total = 0;
        for (InstalledImplant i : s.installed().values()) {
            total += loadOf(i.type(), i.tier());
        }
        return total;
    }

    // ---------------------------------------------------------------- state updates

    /** Applies a change to the player's body and schedules a sync to their client. */
    public static void update(ServerPlayer player, UnaryOperator<BodyState> change) {
        BodyState next = change.apply(BodyState.get(player));
        BodyState.set(player, next);
        BodySync.markDirty(player);
    }

    /** Like {@link #update} but waits for the periodic sync (for per-tick values such as Charge). */
    public static void updateQuiet(ServerPlayer player, UnaryOperator<BodyState> change) {
        BodyState.set(player, change.apply(BodyState.get(player)));
    }

    // ---------------------------------------------------------------- charge

    public static boolean chargeEnabled() {
        try {
            return ServerConfig.CHARGE_ENABLED.get();
        } catch (IllegalStateException e) {
            return true; // the server config is not loaded (yet) on this side
        }
    }

    public static double charge(Player player) {
        return BodyState.get(player).charge();
    }

    /** Spends Charge from the main battery. With the energy system disabled this is free and always succeeds. */
    public static boolean spend(ServerPlayer player, double amount) {
        if (amount <= 0 || !chargeEnabled()) {
            return true;
        }
        BodyState s = BodyState.get(player);
        double cost = amount * chargeMultiplier(s);
        if (s.charge() + 1e-9 < cost) {
            return false;
        }
        updateQuiet(player, st -> st.withCharge(st.charge() - cost, st.buffer()));
        return true;
    }

    /** Spends for an Emergency activation: the main battery first, then the Energy Buffer. */
    public static boolean spendEmergency(ServerPlayer player, double amount) {
        if (amount <= 0 || !chargeEnabled()) {
            return true;
        }
        BodyState s = BodyState.get(player);
        double cost = amount * chargeMultiplier(s);
        if (s.charge() + s.buffer() + 1e-9 < cost) {
            return false;
        }
        double fromBattery = Math.min(s.charge(), cost);
        double fromBuffer = cost - fromBattery;
        updateQuiet(player, st -> st.withCharge(st.charge() - fromBattery, st.buffer() - fromBuffer));
        BodySync.markDirty(player);
        return true;
    }

    public static boolean canAfford(Player player, double amount) {
        BodyState s = BodyState.get(player);
        return amount <= 0 || !chargeEnabled() || s.charge() + 1e-9 >= amount * chargeMultiplier(s);
    }

    public static void addCharge(ServerPlayer player, double amount) {
        BodyState s = BodyState.get(player);
        double max = maxCharge(s);
        if (max <= 0) {
            return;
        }
        updateQuiet(player, st -> st.withCharge(Math.min(max, st.charge() + amount), st.buffer()));
    }

    // ---------------------------------------------------------------- cooldowns

    public static boolean ready(Player player, ResourceLocation type) {
        return BodyState.get(player).cooldownRemaining(type) == 0;
    }

    /** Starts a cooldown. With Charge disabled every cooldown is stretched by the configured multiplier. */
    public static void startCooldown(ServerPlayer player, ResourceLocation type, int ticks) {
        if (ticks <= 0) {
            return;
        }
        double scaled = chargeEnabled() ? ticks : ticks * ServerConfig.CHARGE_DISABLED_COOLDOWN_MULTIPLIER.get();
        update(player, st -> st.withCooldown(type, st.onlineTicks() + Math.round(scaled)));
    }

    /** The full cooldown of an installed implant in a body state, for cooldown bars. */
    public static int cooldownTicksFor(BodyState s, ResourceLocation type) {
        int tier = s.tierOf(type);
        return tier == 0 ? 0 : Stats.of(type, tier).cooldownTicks();
    }

    /** The tier-1..5 cooldown of an installed implant from its stats. */
    public static int cooldownTicks(Player player, ResourceLocation type) {
        TierStats t = Stats.of(player, type);
        return t.cooldownTicks();
    }

    private Body() {
    }
}
