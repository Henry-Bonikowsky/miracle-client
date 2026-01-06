package gg.miracle.replay.camera;

import gg.miracle.replay.data.ReplayFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates cinematic camera paths from replay data.
 */
public class CameraPathGenerator {
    private static final Random RANDOM = new Random();
    
    // Orbit camera settings
    private static final double ORBIT_RADIUS = 5.0;
    private static final double ORBIT_HEIGHT_VARIATION = 2.0;
    private static final double ORBIT_SPEED = 0.05; // Radians per frame
    
    // Chase camera settings
    private static final double CHASE_DISTANCE = 4.0;
    private static final double CHASE_HEIGHT_OFFSET = 1.5;
    private static final float CHASE_DAMPING = 0.15f;
    private static final float CHASE_LOOK_AHEAD = 0.3f;
    
    /**
     * Generate a random camera style.
     */
    public static CameraStyle randomStyle() {
        CameraStyle[] styles = {CameraStyle.ORBIT, CameraStyle.CHASE};
        return styles[RANDOM.nextInt(styles.length)];
    }
    
    /**
     * Generate a camera path from replay frames.
     * 
     * @param frames The replay frames to generate a path for
     * @param style The camera style to use
     * @return Generated camera path
     */
    public static CameraPath generate(List<ReplayFrame> frames, CameraStyle style) {
        if (frames.isEmpty()) {
            return new CameraPath(style, new ArrayList<>());
        }
        
        return switch (style) {
            case ORBIT -> generateOrbitPath(frames);
            case CHASE -> generateChasePath(frames);
            case MULTI_ANGLE -> generateMultiAnglePath(frames);
        };
    }
    
    /**
     * Generate orbit camera - circles around player at fixed radius.
     * Uses Catmull-Rom spline for smooth circular motion.
     */
    private static CameraPath generateOrbitPath(List<ReplayFrame> frames) {
        List<CameraKeyframe> keyframes = new ArrayList<>();
        
        double angle = RANDOM.nextDouble() * Math.PI * 2; // Random starting angle
        
        for (ReplayFrame frame : frames) {
            double playerX = frame.getPlayerX();
            double playerY = frame.getPlayerY();
            double playerZ = frame.getPlayerZ();
            
            // Calculate camera position on orbit
            double heightVariation = Math.sin(angle * 3) * ORBIT_HEIGHT_VARIATION;
            double cameraX = playerX + Math.cos(angle) * ORBIT_RADIUS;
            double cameraY = playerY + 1.6 + heightVariation; // Eye height + variation
            double cameraZ = playerZ + Math.sin(angle) * ORBIT_RADIUS;
            
            // Look at player
            double dx = playerX - cameraX;
            double dy = playerY + 1.6 - cameraY;
            double dz = playerZ - cameraZ;
            
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));
            
            keyframes.add(new CameraKeyframe(
                cameraX, cameraY, cameraZ,
                yaw, pitch,
                frame.getTimestamp()
            ));
            
            angle += ORBIT_SPEED;
        }
        
        return new CameraPath(CameraStyle.ORBIT, keyframes);
    }
    
    /**
     * Generate chase camera - follows behind player with smooth lag.
     * Uses exponential smoothing for natural tracking.
     */
    private static CameraPath generateChasePath(List<ReplayFrame> frames) {
        List<CameraKeyframe> keyframes = new ArrayList<>();
        
        // Initialize camera position at first frame
        ReplayFrame firstFrame = frames.get(0);
        double cameraX = firstFrame.getPlayerX();
        double cameraY = firstFrame.getPlayerY() + CHASE_HEIGHT_OFFSET;
        double cameraZ = firstFrame.getPlayerZ();
        
        for (int i = 0; i < frames.size(); i++) {
            ReplayFrame frame = frames.get(i);
            
            // Calculate ideal camera position (behind player)
            double playerYawRad = Math.toRadians(frame.getPlayerYaw());
            double idealX = frame.getPlayerX() - Math.sin(playerYawRad) * CHASE_DISTANCE;
            double idealY = frame.getPlayerY() + CHASE_HEIGHT_OFFSET;
            double idealZ = frame.getPlayerZ() + Math.cos(playerYawRad) * CHASE_DISTANCE;
            
            // Smooth camera movement with exponential damping
            cameraX += (idealX - cameraX) * CHASE_DAMPING;
            cameraY += (idealY - cameraY) * CHASE_DAMPING;
            cameraZ += (idealZ - cameraZ) * CHASE_DAMPING;
            
            // Look ahead of player for dynamic feel
            double lookAtX = frame.getPlayerX();
            double lookAtY = frame.getPlayerY() + 1.6;
            double lookAtZ = frame.getPlayerZ();
            
            if (i < frames.size() - 1) {
                ReplayFrame nextFrame = frames.get(i + 1);
                double velX = nextFrame.getPlayerX() - frame.getPlayerX();
                double velZ = nextFrame.getPlayerZ() - frame.getPlayerZ();
                
                lookAtX += velX * CHASE_LOOK_AHEAD;
                lookAtZ += velZ * CHASE_LOOK_AHEAD;
            }
            
            // Calculate camera rotation to look at target
            double dx = lookAtX - cameraX;
            double dy = lookAtY - cameraY;
            double dz = lookAtZ - cameraZ;
            
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));
            
            keyframes.add(new CameraKeyframe(
                cameraX, cameraY, cameraZ,
                yaw, pitch,
                frame.getTimestamp()
            ));
        }
        
        return new CameraPath(CameraStyle.CHASE, keyframes);
    }
    
    /**
     * Generate multi-angle camera - cuts between preset dramatic angles.
     * Switches based on action intensity.
     */
    private static CameraPath generateMultiAnglePath(List<ReplayFrame> frames) {
        // TODO: Implement multi-angle cuts (Phase 3.5)
        // For now, fall back to orbit
        return generateOrbitPath(frames);
    }
}
