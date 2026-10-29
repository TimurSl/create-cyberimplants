package dev.cyberimplants.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config: overlay presentation, accessibility and interface animations. */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue OVERLAY_OPACITY;
    public static final ModConfigSpec.BooleanValue REDUCE_SCREEN_EFFECTS;
    public static final ModConfigSpec.BooleanValue COLORBLIND_PALETTE;
    public static final ModConfigSpec.BooleanValue UI_ANIMATIONS;
    public static final ModConfigSpec.BooleanValue UI_SOUNDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("hud");
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

    private ClientConfig() {
    }
}
