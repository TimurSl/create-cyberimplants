package dev.cyberimplants.client;

import dev.cyberimplants.config.ClientConfig;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Accessibility helpers (Layer 5): the colour-blind palette (Okabe-Ito colours, which stay apart for every common
 * colour-vision deficiency) and the particle density setting.
 */
public final class A11y {
    public static boolean colorblind() {
        return ClientConfig.get(ClientConfig.COLORBLIND_PALETTE);
    }

    /** HUD colour of an Entity Scanner threat class. */
    public static int kindColor(String kind) {
        if (colorblind()) {
            return switch (kind) {
                case "hostile" -> 0xFFE69F00;  // orange
                case "boss" -> 0xFFCC79A7;     // reddish purple
                case "neutral" -> 0xFFF0E442;  // yellow
                case "player" -> 0xFF56B4E9;   // sky blue
                default -> 0xFF009E73;         // bluish green
            };
        }
        return switch (kind) {
            case "hostile" -> CyberUi.RED;
            case "boss" -> 0xFFFF40FF;
            case "neutral" -> CyberUi.AMBER;
            case "player" -> 0xFF60C0FF;
            default -> 0xFF80FF80;
        };
    }

    /** A Thermal Vision footprint's colour at {@code heat} (1 fresh, 0 cold): orange to dim red, or yellow to blue. */
    public static Vector3f heat(float heat) {
        if (colorblind()) {
            return new Vector3f(0.34f + 0.6f * heat, 0.46f + 0.44f * heat, 0.91f - 0.8f * heat);
        }
        return new Vector3f(1.0f, 0.25f + 0.45f * heat, 0.1f * heat);
    }

    /** The Combat Predictor's Precog ghost: violet, or sky blue in the colour-blind palette. */
    public static Vector3f precog() {
        return colorblind() ? new Vector3f(0.34f, 0.71f, 0.91f) : new Vector3f(0.7f, 0.36f, 1.0f);
    }

    /** Whether to draw one more decorative particle, by the particle density setting. */
    public static boolean particle() {
        double density = ClientConfig.get(ClientConfig.PARTICLE_DENSITY);
        return density >= 1.0 || ThreadLocalRandom.current().nextDouble() < density;
    }

    private A11y() {
    }
}
