package dev.cyberimplants.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config skeleton. Keys mirror the design docs (layers, charge, death rules, deck caps, PvP options);
 * most are consumed by later layers.
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue LAYERS_MAX_ENABLED;

    public static final ModConfigSpec.BooleanValue CHARGE_ENABLED;
    public static final ModConfigSpec.DoubleValue CHARGE_DISABLED_COOLDOWN_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue RIG_TIME_MULTIPLIER;

    public static final ModConfigSpec.BooleanValue KEEP_ON_DEATH;

    public static final ModConfigSpec.IntValue DECK_MAX_CYCLES_PER_PLAYER_PER_TICK;
    public static final ModConfigSpec.IntValue DECK_MAX_CYCLES_PER_SERVER_TICK;
    public static final ModConfigSpec.IntValue DECK_MAX_SOURCE_LENGTH;
    public static final ModConfigSpec.IntValue DECK_MAX_EVENTS_PER_SECOND;

    public static final ModConfigSpec.DoubleValue PVP_SANDEVISTAN_TIME_SCALE_FLOOR;
    public static final ModConfigSpec.BooleanValue PVP_COMBAT_PREDICTOR_AIM_ASSIST;
    public static final ModConfigSpec.BooleanValue PVP_SCAN_WARNINGS;
    public static final ModConfigSpec.DoubleValue PVP_CONTROL_EFFECT_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue PVP_DECK_SCRIPTS_AFFECT_PLAYERS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("layers");
        LAYERS_MAX_ENABLED = b
                .comment("Highest implementation layer (1-5) that is active. Content above it is disabled.")
                .defineInRange("maxEnabled", 5, 1, 5);
        b.pop();

        b.push("charge");
        CHARGE_ENABLED = b
                .comment("Disable to remove the energy system. All Charge costs become 0.")
                .define("enabled", true);
        CHARGE_DISABLED_COOLDOWN_MULTIPLIER = b
                .comment("Cooldown multiplier applied to every active implant while Charge is disabled.")
                .defineInRange("disabledCooldownMultiplier", 1.5, 1.0, 10.0);
        b.pop();

        b.push("rig");
        RIG_TIME_MULTIPLIER = b
                .comment("Scales the 5-20 s Implantation Rig operation time. 0.01 makes operations nearly instant.")
                .defineInRange("timeMultiplier", 1.0, 0.01, 10.0);
        b.pop();

        b.push("implants");
        KEEP_ON_DEATH = b
                .comment("Installed implants persist through death.")
                .define("keepOnDeath", true);
        b.pop();

        b.push("deck");
        DECK_MAX_CYCLES_PER_PLAYER_PER_TICK = b
                .defineInRange("maxCyclesPerPlayerPerTick", 1_000, 1, 1_000_000);
        DECK_MAX_CYCLES_PER_SERVER_TICK = b
                .comment("When exceeded, decks are throttled round-robin.")
                .defineInRange("maxCyclesPerServerTick", 50_000, 1, 100_000_000);
        DECK_MAX_SOURCE_LENGTH = b
                .defineInRange("maxSourceLength", 32 * 1024, 256, 1_048_576);
        DECK_MAX_EVENTS_PER_SECOND = b
                .defineInRange("maxEventsPerSecond", 200, 1, 100_000);
        b.pop();

        b.push("pvp");
        PVP_SANDEVISTAN_TIME_SCALE_FLOOR = b
                .comment("Minimum time scale for players inside a Sandevistan field.")
                .defineInRange("sandevistanTimeScaleFloor", 0.30, 0.0, 1.0);
        PVP_COMBAT_PREDICTOR_AIM_ASSIST = b
                .comment("Allow Combat Predictor aim assist against players.")
                .define("combatPredictorAimAssist", false);
        PVP_SCAN_WARNINGS = b
                .comment("Warn players who are scanned or tracked.")
                .define("scanWarnings", true);
        PVP_CONTROL_EFFECT_MULTIPLIER = b
                .comment("Scales the duration of stuns, slows and freezes applied to players.")
                .defineInRange("controlEffectMultiplier", 1.0, 0.0, 2.0);
        PVP_DECK_SCRIPTS_AFFECT_PLAYERS = b
                .comment("Whether script-issued activations may target players.")
                .define("deckScriptsAffectPlayers", true);
        b.pop();

        SPEC = b.build();
    }

    private ServerConfig() {
    }
}
