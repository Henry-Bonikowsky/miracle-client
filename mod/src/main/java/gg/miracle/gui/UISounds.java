package gg.miracle.gui;

import gg.miracle.sound.MiracleSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility for playing UI sounds.
 */
public class UISounds {

    private static final float DEFAULT_VOLUME = 0.5f;
    private static final float DEFAULT_PITCH = 1.0f;

    // Track currently hovered widgets for automatic hover sound
    private static final Set<Object> hoveredWidgets = new HashSet<>();

    /** Standard click sound for buttons, tabs, etc. */
    public static void click() {
        play(MiracleSounds.UI_CLICK);
    }

    /** Hover sound for when mouse enters an interactive element */
    public static void hover() {
        play(MiracleSounds.UI_HOVER, 0.3f, 1.2f);
    }

    /**
     * Call in render() to auto-play hover sound on hover enter.
     * @param key unique identifier (use 'this' or a string key)
     * @param isHovered current hover state
     */
    public static void checkHover(Object key, boolean isHovered) {
        boolean wasHovered = hoveredWidgets.contains(key);
        if (isHovered && !wasHovered) {
            hoveredWidgets.add(key);
            hover();
        } else if (!isHovered && wasHovered) {
            hoveredWidgets.remove(key);
        }
    }

    /**
     * Call in mouseClicked() to play click sound.
     * Returns true so you can chain: if (UISounds.clickAndReturn()) { ... }
     */
    public static boolean clickAndReturn() {
        click();
        return true;
    }

    /** Back/cancel sound */
    public static void back() {
        play(MiracleSounds.UI_BACK);
    }

    /** Toggle sound based on new state */
    public static void toggle(boolean enabled) {
        if (enabled) {
            toggleOn();
        } else {
            toggleOff();
        }
    }

    /** Toggle enabled sound */
    public static void toggleOn() {
        play(MiracleSounds.UI_TOGGLE_ON);
    }

    /** Toggle disabled sound */
    public static void toggleOff() {
        play(MiracleSounds.UI_TOGGLE_OFF);
    }

    /** Error/failure sound */
    public static void error() {
        play(MiracleSounds.UI_ERROR);
    }

    /** Success/confirmation sound */
    public static void success() {
        play(MiracleSounds.UI_SUCCESS);
    }

    /** Play a sound with default volume/pitch */
    public static void play(SoundEvent sound) {
        play(sound, DEFAULT_VOLUME, DEFAULT_PITCH);
    }

    /** Play a sound with custom volume/pitch */
    public static void play(SoundEvent sound, float volume, float pitch) {
        if (sound != null) {
            MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(sound, pitch, volume));
        }
    }
}
