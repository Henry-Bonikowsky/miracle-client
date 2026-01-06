package gg.miracle.replay.camera;

/**
 * A single keyframe in a camera path.
 * Represents camera position and rotation at a specific point in time.
 */
public class CameraKeyframe {
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final float roll;
    private final long timestamp;
    
    public CameraKeyframe(double x, double y, double z, float yaw, float pitch, float roll, long timestamp) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.timestamp = timestamp;
    }
    
    public CameraKeyframe(double x, double y, double z, float yaw, float pitch, long timestamp) {
        this(x, y, z, yaw, pitch, 0.0f, timestamp);
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getRoll() { return roll; }
    public long getTimestamp() { return timestamp; }
    
    /**
     * Linearly interpolate between this keyframe and another.
     */
    public CameraKeyframe lerp(CameraKeyframe other, float t) {
        return new CameraKeyframe(
            lerp(this.x, other.x, t),
            lerp(this.y, other.y, t),
            lerp(this.z, other.z, t),
            lerpAngle(this.yaw, other.yaw, t),
            lerpAngle(this.pitch, other.pitch, t),
            lerpAngle(this.roll, other.roll, t),
            (long) lerp(this.timestamp, other.timestamp, t)
        );
    }
    
    private static double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }
    
    /**
     * Interpolate angles correctly (handling wraparound at 360°).
     */
    private static float lerpAngle(float a, float b, float t) {
        float delta = ((b - a + 180) % 360 + 360) % 360 - 180;
        return a + delta * t;
    }
}
