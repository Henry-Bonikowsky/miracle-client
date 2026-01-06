package gg.miracle.replay.camera;

/**
 * Camera styles for cinematic replay rendering.
 */
public enum CameraStyle {
    /**
     * Orbit camera - circles around the player at a fixed radius.
     * Uses Catmull-Rom spline interpolation for smooth motion.
     */
    ORBIT,
    
    /**
     * Chase camera - follows behind the player with smooth lag.
     * Uses exponential smoothing for natural tracking.
     */
    CHASE,
    
    /**
     * Multi-angle - cuts between preset dramatic angles.
     * Switches angles based on action intensity.
     */
    MULTI_ANGLE
}
