package dev.cyberimplants.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Key bindings (docs/design/01-core-systems.md, section 1.6). Quick slots and deck macro keys are unbound by default. */
public final class ModKeys {
    public static final String CATEGORY = "key.categories.cyberimplants";

    public static final KeyMapping WHEEL = key("wheel", GLFW.GLFW_KEY_R);
    public static final KeyMapping DODGE = key("dodge", GLFW.GLFW_KEY_V);
    public static final KeyMapping FIRE_BALLISTIC = key("fire_ballistic", GLFW.GLFW_KEY_G);
    public static final KeyMapping DECK = key("deck", GLFW.GLFW_KEY_K);
    public static final KeyMapping MANUAL = key("manual", GLFW.GLFW_KEY_H);
    public static final KeyMapping SETTINGS = key("settings", InputConstants.UNKNOWN.getValue());
    public static final KeyMapping[] MACROS = {
            key("macro_1", InputConstants.UNKNOWN.getValue()),
            key("macro_2", InputConstants.UNKNOWN.getValue()),
            key("macro_3", InputConstants.UNKNOWN.getValue()),
            key("macro_4", InputConstants.UNKNOWN.getValue())};

    private static KeyMapping key(String name, int keyCode) {
        return new KeyMapping("key.cyberimplants." + name, InputConstants.Type.KEYSYM, keyCode, CATEGORY);
    }

    public static KeyMapping[] all() {
        KeyMapping[] all = new KeyMapping[6 + MACROS.length];
        all[0] = WHEEL;
        all[1] = DODGE;
        all[2] = FIRE_BALLISTIC;
        all[3] = DECK;
        all[4] = MANUAL;
        all[5] = SETTINGS;
        System.arraycopy(MACROS, 0, all, 6, MACROS.length);
        return all;
    }

    private ModKeys() {
    }
}
