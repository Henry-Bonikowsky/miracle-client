package gg.miracle.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Registry for Miracle Client custom sounds.
 */
public class MiracleSounds {

    // UI Sounds
    public static final SoundEvent UI_CLICK = register("ui.click");
    public static final SoundEvent UI_HOVER = register("ui.hover");
    public static final SoundEvent UI_BACK = register("ui.back");
    public static final SoundEvent UI_TOGGLE_ON = register("ui.toggle_on");
    public static final SoundEvent UI_TOGGLE_OFF = register("ui.toggle_off");
    public static final SoundEvent UI_ERROR = register("ui.error");
    public static final SoundEvent UI_SUCCESS = register("ui.success");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of("miracle", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {
        // Called to trigger static initialization
    }
}
