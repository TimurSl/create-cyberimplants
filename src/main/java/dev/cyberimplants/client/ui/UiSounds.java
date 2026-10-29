package dev.cyberimplants.client.ui;

import dev.cyberimplants.config.ClientConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Quiet interface blips, all vanilla sounds. Off with {@code ui.sounds = false}. */
public final class UiSounds {
    private static long lastHover;

    private static boolean enabled() {
        try {
            return ClientConfig.UI_SOUNDS.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    private static void play(Holder<SoundEvent> sound, float pitch, float volume) {
        if (enabled()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound.value(), pitch, volume));
        }
    }

    private static void play(SoundEvent sound, float pitch, float volume) {
        if (enabled()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    /** Pointer moved onto something new. Rate limited so sweeping across a list does not rattle. */
    public static void hover() {
        long now = Util.getMillis();
        if (now - lastHover > 60) {
            lastHover = now;
            play(SoundEvents.NOTE_BLOCK_HAT, 2.0f, 0.12f);
        }
    }

    public static void select() {
        play(SoundEvents.NOTE_BLOCK_BIT, 1.6f, 0.25f);
    }

    /** A screen comes up. */
    public static void boot() {
        play(SoundEvents.BEACON_POWER_SELECT, 1.8f, 0.25f);
    }

    /** One line of a boot log or one typed character group. */
    public static void tick() {
        play(SoundEvents.NOTE_BLOCK_BIT, 2.0f, 0.08f);
    }

    public static void success() {
        play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.8f);
        play(SoundEvents.NOTE_BLOCK_CHIME, 1.5f, 0.35f);
    }

    public static void error() {
        play(SoundEvents.NOTE_BLOCK_BASS, 0.6f, 0.4f);
    }

    private UiSounds() {
    }
}
