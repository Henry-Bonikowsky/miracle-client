package gg.miracle.modules.render;

import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import net.minecraft.client.option.Perspective;

/**
 * Freelook module - hold key to look around in third person without moving character.
 *
 * This module works via mixins:
 * - EntityMixin: Intercepts mouse movement to update camera rotation instead of player rotation
 * - CameraMixin: Applies the freelook rotation to the camera
 */
public class Freelook extends Module {
    private boolean freelookActive = false;

    // Camera rotation (independent of player)
    private float cameraYaw = 0;
    private float cameraPitch = 0;

    // Store original perspective to restore
    private Perspective savedPerspective = Perspective.FIRST_PERSON;

    public Freelook() {
        super("Freelook", "Hold to look around in third person", Category.RENDER);
    }

    /**
     * Called from MiracleClient tick handler with current key state
     */
    public void updateHoldState(boolean keyHeld) {
        if (keyHeld && !freelookActive) {
            // Key just pressed - activate freelook
            activateFreelook();
        } else if (!keyHeld && freelookActive) {
            // Key just released - deactivate freelook
            deactivateFreelook();
        }
    }

    private void activateFreelook() {
        if (mc.player == null) return;

        freelookActive = true;

        // Save current perspective and switch to third person back
        savedPerspective = mc.options.getPerspective();
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);

        // Initialize camera to current player rotation
        cameraYaw = mc.player.getYaw();
        cameraPitch = mc.player.getPitch();
    }

    private void deactivateFreelook() {
        freelookActive = false;

        // Restore original perspective
        mc.options.setPerspective(savedPerspective);
    }

    @Override
    protected void onDisable() {
        if (freelookActive) {
            deactivateFreelook();
        }
    }

    /**
     * Check if freelook camera is currently active.
     * Used by mixins to determine if they should intercept rotation.
     */
    public boolean isFreelookActive() {
        return freelookActive;
    }

    public float getCameraYaw() {
        return cameraYaw;
    }

    public void setCameraYaw(float yaw) {
        this.cameraYaw = yaw;
    }

    public float getCameraPitch() {
        return cameraPitch;
    }

    public void setCameraPitch(float pitch) {
        this.cameraPitch = pitch;
    }
}
