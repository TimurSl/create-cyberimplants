package dev.cyberimplants.config;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;

/**
 * Client config: HUD layout and contents, implant visuals, accessibility and interface animations. Everything here is
 * editable in game from the mod list (Mods, Create: Cybernetics, Config) or with the Cybernetics settings key.
 */
public final class ClientConfig {
    /** Where the HUD status panel sits. */
    public enum Corner implements TranslatableEnum {
        BOTTOM_LEFT, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT;

        public boolean right() {
            return this == TOP_RIGHT || this == BOTTOM_RIGHT;
        }

        public boolean bottom() {
            return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
        }

        @Override
        public Component getTranslatedName() {
            return Component.translatable("cyberimplants.configuration.corner." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.EnumValue<Corner> HUD_CORNER;
    public static final ModConfigSpec.IntValue HUD_OFFSET_X;
    public static final ModConfigSpec.IntValue HUD_OFFSET_Y;
    public static final ModConfigSpec.BooleanValue HUD_LABELS;
    public static final ModConfigSpec.BooleanValue HUD_STATUS;
    public static final ModConfigSpec.BooleanValue HUD_COOLDOWNS;
    public static final ModConfigSpec.BooleanValue HUD_KEY_HINTS;
    public static final ModConfigSpec.BooleanValue HUD_VITALS;
    public static final ModConfigSpec.BooleanValue HUD_SCAN;
    public static final ModConfigSpec.DoubleValue OVERLAY_OPACITY;
    public static final ModConfigSpec.BooleanValue REDUCE_SCREEN_EFFECTS;
    public static final ModConfigSpec.BooleanValue COLORBLIND_PALETTE;

    public static final ModConfigSpec.BooleanValue IMPLANT_MODELS;
    public static final ModConfigSpec.BooleanValue OTHER_PLAYERS_IMPLANTS;
    public static final ModConfigSpec.BooleanValue FIRST_PERSON_IMPLANTS;
    public static final ModConfigSpec.BooleanValue AFTERIMAGES;
    public static final ModConfigSpec.IntValue AFTERIMAGE_LIFETIME;
    public static final ModConfigSpec.IntValue AFTERIMAGE_SPACING;
    public static final ModConfigSpec.DoubleValue AFTERIMAGE_OPACITY;
    public static final ModConfigSpec.BooleanValue AFTERIMAGE_RAINBOW;

    public static final ModConfigSpec.BooleanValue UI_ANIMATIONS;
    public static final ModConfigSpec.BooleanValue UI_SOUNDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("hud");
        HUD_ENABLED = b
                .comment("Show the implant HUD (Charge, shields, ammo, implant status, cooldowns).")
                .define("showHud", true);
        HUD_SCALE = b
                .comment("Size of the HUD, relative to the GUI scale.")
                .defineInRange("scale", 1.0, 0.5, 2.0);
        HUD_CORNER = b
                .comment("Screen corner the status panel is anchored to.")
                .defineEnum("corner", Corner.BOTTOM_LEFT);
        HUD_OFFSET_X = b
                .comment("Horizontal distance of the status panel from its corner, in GUI pixels.")
                .defineInRange("offsetX", 6, 0, 1000);
        HUD_OFFSET_Y = b
                .comment("Vertical distance of the status panel from its corner, in GUI pixels.")
                .defineInRange("offsetY", 6, 0, 1000);
        HUD_LABELS = b
                .comment("Name every bar (CHARGE, SHIELD, ...) and show its numbers.")
                .define("labels", true);
        HUD_STATUS = b
                .comment("List what is running: deployed blade, Sandevistan, camouflage, toggles, ammo.")
                .define("implantStatus", true);
        HUD_COOLDOWNS = b
                .comment("Show cooldown timers above the hotbar.")
                .define("cooldowns", true);
        HUD_KEY_HINTS = b
                .comment("Show which keys open the implant wheel, fire the Ballistic Module and open the manual.")
                .define("keyHints", true);
        HUD_VITALS = b
                .comment("Show the Biomonitor's vitals readout (top right).")
                .define("vitals", true);
        HUD_SCAN = b
                .comment("Show the Entity Scanner and Target Tracking readouts (top left).")
                .define("scan", true);
        OVERLAY_OPACITY = b
                .comment("Opacity of Optical implant overlays.")
                .defineInRange("overlayOpacity", 0.8, 0.0, 1.0);
        REDUCE_SCREEN_EFFECTS = b
                .comment("Disable static, screen shake and strong colour washes.")
                .define("reduceScreenEffects", false);
        COLORBLIND_PALETTE = b
                .comment("Use a colour-blind friendly palette for overlays.")
                .define("colorblindPalette", false);
        b.pop();

        b.push("visuals");
        IMPLANT_MODELS = b
                .comment("Draw installed implants on player models: arm plating, blades, launchers, glowing optics, spine decks.")
                .define("implantModels", true);
        OTHER_PLAYERS_IMPLANTS = b
                .comment("Also draw other players' implants, so you can tell who is augmented.")
                .define("otherPlayers", true);
        FIRST_PERSON_IMPLANTS = b
                .comment("Draw arm implants on your own arm in first person.")
                .define("firstPerson", true);
        AFTERIMAGES = b
                .comment("A running Sandevistan leaves a trail of coloured afterimages.")
                .define("sandevistanAfterimages", true);
        AFTERIMAGE_LIFETIME = b
                .comment("How long each afterimage lingers, in ticks (20 = one second).")
                .defineInRange("afterimageLifetime", 14, 4, 60);
        AFTERIMAGE_SPACING = b
                .comment("Ticks between two afterimages. Lower is a denser trail.")
                .defineInRange("afterimageSpacing", 1, 1, 10);
        AFTERIMAGE_OPACITY = b
                .comment("Opacity of a fresh afterimage.")
                .defineInRange("afterimageOpacity", 0.6, 0.1, 1.0);
        AFTERIMAGE_RAINBOW = b
                .comment("Cycle the afterimages through many colours; off keeps them Sandevistan green.")
                .define("afterimageRainbow", true);
        b.pop();

        b.push("ui");
        UI_ANIMATIONS = b
                .comment("Animate screens: boot sequences, transitions, the rotating body hologram and effect previews.")
                .define("animations", true);
        UI_SOUNDS = b
                .comment("Play interface blips when hovering, selecting and when the Rig finishes an operation.")
                .define("sounds", true);
        b.pop();

        SPEC = b.build();
    }

    /** Reads a value, falling back to its default before the config file has loaded. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return value.getDefault();
        }
    }

    private ClientConfig() {
    }
}
