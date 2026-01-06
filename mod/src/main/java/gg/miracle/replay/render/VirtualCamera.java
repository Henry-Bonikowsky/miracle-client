package gg.miracle.replay.render;

import gg.miracle.replay.camera.CameraKeyframe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Virtual camera for replay rendering.
 * Overrides the player camera to render from custom positions/rotations.
 */
public class VirtualCamera {
    private static VirtualCamera instance;
    
    private boolean active = false;
    private CameraKeyframe currentFrame;
    
    private double x, y, z;
    private float yaw, pitch, roll;
    
    public VirtualCamera() {
        instance = this;
    }
    
    public static VirtualCamera getInstance() {
        if (instance == null) {
            instance = new VirtualCamera();
        }
        return instance;
    }
    
    /**
     * Activate virtual camera with a specific keyframe.
     */
    public void activate(CameraKeyframe keyframe) {
        this.active = true;
        this.currentFrame = keyframe;
        this.x = keyframe.getX();
        this.y = keyframe.getY();
        this.z = keyframe.getZ();
        this.yaw = keyframe.getYaw();
        this.pitch = keyframe.getPitch();
        this.roll = keyframe.getRoll();
    }
    
    /**
     * Update virtual camera to a new keyframe.
     */
    public void update(CameraKeyframe keyframe) {
        if (!active) return;
        
        this.currentFrame = keyframe;
        this.x = keyframe.getX();
        this.y = keyframe.getY();
        this.z = keyframe.getZ();
        this.yaw = keyframe.getYaw();
        this.pitch = keyframe.getPitch();
        this.roll = keyframe.getRoll();
    }
    
    /**
     * Deactivate virtual camera (return to normal player camera).
     */
    public void deactivate() {
        this.active = false;
        this.currentFrame = null;
    }
    
    /**
     * Check if virtual camera is currently active.
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Note: Camera transform is applied from CameraMixin using setPos/setRotation.
     * Those methods are protected, so they must be called from the mixin with @Shadow access.
     */
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getRoll() { return roll; }
    public Vec3d getPos() { return new Vec3d(x, y, z); }
    public CameraKeyframe getCurrentFrame() { return currentFrame; }
}
