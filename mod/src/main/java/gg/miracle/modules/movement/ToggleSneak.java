package gg.miracle.modules.movement;

import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import net.minecraft.client.option.KeyBinding;

public class ToggleSneak extends Module {
    private final Setting<Boolean> showIndicator = register(
        Setting.ofBool("Show Indicator", "Show sneak state on screen", true)
    );

    private boolean sneakToggled = false;

    public ToggleSneak() {
        super("ToggleSneak", "Toggle sneak instead of holding", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Check if player pressed sneak key
        if (mc.options.sneakKey.wasPressed()) {
            sneakToggled = !sneakToggled;
        }

        // Force sneak state if toggled
        if (sneakToggled) {
            // Use key binding to simulate sneak being held
            KeyBinding.setKeyPressed(mc.options.sneakKey.getDefaultKey(), true);
        }
    }

    public boolean isSneakToggled() {
        return sneakToggled && isEnabled();
    }

    public boolean shouldShowIndicator() {
        return showIndicator.get() && isEnabled();
    }

    @Override
    protected void onDisable() {
        sneakToggled = false;
        // Release the sneak key
        if (mc.player != null) {
            KeyBinding.setKeyPressed(mc.options.sneakKey.getDefaultKey(), false);
        }
    }
}
