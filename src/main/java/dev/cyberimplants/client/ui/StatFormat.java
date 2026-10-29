package dev.cyberimplants.client.ui;

import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Turns the raw numbers of an implant's stat file into readable rows: a translated label
 * ({@code cyberimplants.stat.<key>}) and a value with its unit. Keys ending in {@code _ticks} read as seconds,
 * {@code _per_second} as a rate, fractions of a whole as percentages, scales as multipliers and distances in metres.
 */
public final class StatFormat {
    private static final Set<String> PERCENT = Set.of("absorb_share", "accel_bonus", "balanced", "bilateral_break_speed", "boost",
            "break_speed", "buff_speed", "counter_bonus", "detect_moving", "detect_still", "dual_attack_speed", "fall_reduction",
            "item_use_speed", "knockback_resistance", "machinery", "movement_penalty", "overload_margin", "reduction",
            "sneak_detection_reduction", "sneak_speed", "specialised", "sprint_speed", "trigger_health", "turn_speed_kept",
            "wall_impact_reduction", "wear_reduction", "walk_speed");
    private static final Set<String> MULTIPLIER = Set.of("time_scale", "freeze_time_scale", "unarmed_multiplier");
    private static final Set<String> METRES = Set.of("radius", "range", "distance", "dash_blocks", "max_horizontal", "max_vertical",
            "reach", "safe_fall", "wall_penetration", "cast_range", "direct_range", "tracking_radius", "jump_height", "step_height");
    /** Larger is worse for these, so the tier comparison colours a rise red. */
    private static final Set<String> LOWER_IS_BETTER = Set.of("time_scale", "freeze_time_scale", "movement_penalty", "weight",
            "latency_ticks", "lockout_ticks", "crash_ticks", "recharge_delay_ticks", "recharge_ticks", "reload_ticks",
            "internal_cooldown_ticks", "full_charge_ticks", "min_every_ticks", "detect_moving", "detect_still",
            "detection_ticks", "upkeep_per_second", "overlay_upkeep_per_second", "charge_per_hp", "sprint_exhaustion",
            "shield_raise_ticks", "emergency_cooldown_ticks", "decay_per_second");

    public static Component label(String key) {
        return Component.translatableWithFallback("cyberimplants.stat." + key, humanize(key));
    }

    public static String value(String key, double v) {
        if (key.equals("persistence_ticks") && v < 0) {
            return "∞";
        }
        if (key.endsWith("_ticks")) {
            return number(v / 20.0) + " s";
        }
        if (key.endsWith("_per_second")) {
            return number(v) + "/s";
        }
        if (PERCENT.contains(key)) {
            return number(v * 100) + "%";
        }
        if (MULTIPLIER.contains(key)) {
            return "×" + number(v);
        }
        if (METRES.contains(key)) {
            return number(v) + " m";
        }
        if (key.equals("kb")) {
            return number(v) + " KB";
        }
        if (key.equals("isa")) {
            return "ISA-" + (int) v;
        }
        return number(v);
    }

    /** Whether a higher value of this stat helps the player. */
    public static boolean higherIsBetter(String key) {
        return !LOWER_IS_BETTER.contains(key);
    }

    /** At most two decimals, no trailing zeros. */
    public static String number(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-6) {
            return Long.toString(Math.round(v));
        }
        String s = String.format(Locale.ROOT, "%.2f", v);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    /** {@code "duration_ticks"} reads as {@code "Duration"} when a language has no label for it. */
    private static String humanize(String key) {
        String k = key.replace("_ticks", "").replace("_per_second", "").replace('_', ' ');
        return k.isEmpty() ? key : Character.toUpperCase(k.charAt(0)) + k.substring(1);
    }

    private StatFormat() {
    }
}
