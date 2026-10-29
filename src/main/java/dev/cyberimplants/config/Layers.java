package dev.cyberimplants.config;

/**
 * The {@code layers.maxEnabled} cap (docs/design/09-roadmap.md, section 9.1 rule 4). Content of a higher layer is off:
 * Tier II-V numbers fall back to the Tier I row, Strain and the Rig's timed operations do not exist, and the deck's
 * ISA is capped. The config is only loaded on the server, so clients and tests read the full five layers.
 */
public final class Layers {
    public static final int MAX = 5;

    public static int maxEnabled() {
        try {
            return ServerConfig.LAYERS_MAX_ENABLED.get();
        } catch (IllegalStateException | NullPointerException e) {
            return MAX; // the server config is not loaded (yet) on this side
        }
    }

    public static boolean atLeast(int layer) {
        return maxEnabled() >= layer;
    }

    private Layers() {
    }
}
