package dev.cyberimplants.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config skeleton: overlay presentation and accessibility. */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue OVERLAY_OPACITY;
    public static final ModConfigSpec.BooleanValue REDUCE_SCREEN_EFFECTS;
    public static final ModConfigSpec.BooleanValue COLORBLIND_PALETTE;

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

        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
