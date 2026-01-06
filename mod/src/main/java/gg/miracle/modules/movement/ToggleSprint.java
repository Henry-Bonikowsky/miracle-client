package gg.miracle.modules.movement;

import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;

public class ToggleSprint extends Module {
    private final Setting<Boolean> showIndicator = register(
        Setting.ofBool("Show Indicator", "Show sprint state on screen", true)
    );

    private boolean sprintToggled = false;

    public ToggleSprint() {
        super("ToggleSprint", "Toggle sprint instead of holding", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Check if player pressed sprint key
        if (mc.options.sprintKey.wasPressed()) {
            sprintToggled = !sprintToggled;
        }

        // Apply sprint if toggled and player is moving forward
        if (sprintToggled && isMovingForward() && !mc.player.isSprinting() && canSprint()) {
            mc.player.setSprinting(true);
        }

        // Disable sprint toggle if player stops or can't sprint
        if (sprintToggled && (!isMovingForward() || !canSprint())) {
            // Keep toggle state but don't force sprint
        }
    }

    private boolean isMovingForward() {
        //? if MC_1_21_5 || MC_1_21_8 || MC_1_21_11 {
        return mc.player.input.hasForwardMovement();
        //?} else {
        /*return mc.player.input.movementForward > 0;*/
        //?}
    }

    private boolean canSprint() {
        return !mc.player.isSneaking() &&
               !mc.player.isUsingItem() &&
               mc.player.getHungerManager().getFoodLevel() > 6;
    }

    public boolean isSprintToggled() {
        return sprintToggled && isEnabled();
    }

    public boolean shouldShowIndicator() {
        return showIndicator.get() && isEnabled();
    }

    @Override
    protected void onDisable() {
        sprintToggled = false;
    }
}
