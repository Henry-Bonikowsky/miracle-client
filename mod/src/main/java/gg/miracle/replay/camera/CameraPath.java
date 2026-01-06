package gg.miracle.replay.camera;

import java.util.ArrayList;
import java.util.List;

/**
 * A camera path consisting of multiple keyframes.
 * Provides interpolation between keyframes for smooth camera motion.
 */
public class CameraPath {
    private final List<CameraKeyframe> keyframes;
    private final CameraStyle style;
    private final long startTime;
    private final long endTime;
    
    public CameraPath(CameraStyle style, List<CameraKeyframe> keyframes) {
        this.style = style;
        this.keyframes = new ArrayList<>(keyframes);
        
        if (keyframes.isEmpty()) {
            this.startTime = 0;
            this.endTime = 0;
        } else {
            this.startTime = keyframes.get(0).getTimestamp();
            this.endTime = keyframes.get(keyframes.size() - 1).getTimestamp();
        }
    }
    
    public CameraStyle getStyle() {
        return style;
    }
    
    public List<CameraKeyframe> getKeyframes() {
        return new ArrayList<>(keyframes);
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public long getDuration() {
        return endTime - startTime;
    }
    
    /**
     * Get the camera position/rotation at a specific time via interpolation.
     * 
     * @param timestamp The time to sample the camera path
     * @return Interpolated camera keyframe
     */
    public CameraKeyframe getFrameAtTime(long timestamp) {
        if (keyframes.isEmpty()) {
            return new CameraKeyframe(0, 0, 0, 0, 0, timestamp);
        }
        
        if (keyframes.size() == 1) {
            return keyframes.get(0);
        }
        
        // Clamp to path duration
        timestamp = Math.max(startTime, Math.min(endTime, timestamp));
        
        // Find surrounding keyframes
        CameraKeyframe before = keyframes.get(0);
        CameraKeyframe after = keyframes.get(keyframes.size() - 1);
        
        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (keyframes.get(i).getTimestamp() <= timestamp && 
                keyframes.get(i + 1).getTimestamp() >= timestamp) {
                before = keyframes.get(i);
                after = keyframes.get(i + 1);
                break;
            }
        }
        
        // Calculate interpolation factor
        long duration = after.getTimestamp() - before.getTimestamp();
        if (duration == 0) {
            return before;
        }
        
        float t = (float) (timestamp - before.getTimestamp()) / duration;
        
        // Interpolate between keyframes
        return before.lerp(after, t);
    }
    
    /**
     * Sample the camera path at regular intervals.
     * 
     * @param fps Frames per second to sample at
     * @return List of keyframes at regular intervals
     */
    public List<CameraKeyframe> sample(int fps) {
        List<CameraKeyframe> samples = new ArrayList<>();
        long duration = getDuration();
        
        if (duration == 0) {
            return keyframes;
        }
        
        long frameTimeMs = 1000L / fps;
        
        for (long time = startTime; time <= endTime; time += frameTimeMs) {
            samples.add(getFrameAtTime(time));
        }
        
        // Ensure we include the final frame
        if (samples.isEmpty() || samples.get(samples.size() - 1).getTimestamp() < endTime) {
            samples.add(getFrameAtTime(endTime));
        }
        
        return samples;
    }
}
