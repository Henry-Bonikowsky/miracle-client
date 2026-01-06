package gg.miracle.replay;

import gg.miracle.MiracleClient;
import gg.miracle.replay.camera.CameraPath;
import gg.miracle.replay.camera.CameraPathGenerator;
import gg.miracle.replay.camera.CameraStyle;
import gg.miracle.replay.data.ReplayFrame;
import gg.miracle.replay.render.ReplayRenderer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Automatic clip generation system.
 * Generates cinematic background videos every 10 minutes.
 */
public class AutoClipSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoClipSystem");
    
    // Clip settings
    private static final int CLIP_INTERVAL_TICKS = 6000; // 5 minutes (300 seconds * 20 ticks)
    private static final int CLIP_DURATION_SECONDS = 30;
    
    private final ReplayBufferManager bufferManager;
    private final ClipManager clipManager;
    private final ReplayRenderer renderer;
    private final MinecraftClient mc;
    
    private boolean enabled = false;
    private int ticksSinceLastClip = 0;
    
    public AutoClipSystem(ReplayBufferManager bufferManager) {
        this.bufferManager = bufferManager;
        this.clipManager = new ClipManager();
        this.renderer = new ReplayRenderer();
        this.mc = MinecraftClient.getInstance();
    }
    
    /**
     * Enable automatic clip generation.
     */
    public void enable() {
        if (enabled) return;
        
        enabled = true;
        ticksSinceLastClip = 0;
        LOGGER.info("Auto-clip system enabled (interval: 5 minutes, duration: 30 seconds)");
    }
    
    /**
     * Disable automatic clip generation.
     */
    public void disable() {
        if (!enabled) return;
        
        enabled = false;
        LOGGER.info("Auto-clip system disabled");
    }
    
    /**
     * Toggle auto-clip system.
     */
    public void toggle() {
        if (enabled) {
            disable();
        } else {
            enable();
        }
    }
    
    /**
     * Tick the auto-clip system.
     * Called every game tick from MiracleClient.
     */
    public void tick() {
        if (!enabled) return;
        if (mc.player == null || mc.world == null) return;
        
        ticksSinceLastClip++;
        
        if (ticksSinceLastClip >= CLIP_INTERVAL_TICKS) {
            generateClip();
            ticksSinceLastClip = 0;
        }
    }
    
    /**
     * Generate a cinematic clip from the replay buffer.
     */
    private void generateClip() {
        LOGGER.info("Generating automatic cinematic clip...");
        
        // Extract last 30 seconds from buffer
        List<ReplayFrame> replayFrames = bufferManager.extractBuffer(CLIP_DURATION_SECONDS);
        
        if (replayFrames.isEmpty()) {
            LOGGER.warn("No replay data available for clip generation");
            return;
        }
        
        // Random camera style (orbit or chase)
        CameraStyle style = CameraPathGenerator.randomStyle();
        
        // Generate camera path
        CameraPath cameraPath = CameraPathGenerator.generate(replayFrames, style);
        
        // Create metadata
        ClipMetadata metadata = ClipMetadata.create(CLIP_DURATION_SECONDS, style);
        
        // Save metadata (video will be saved when renderer completes)
        clipManager.saveClip(metadata);
        
        // Start rendering (TODO: implement video encoding in Phase 5)
        Path outputPath = clipManager.getClipsDirectory().resolve(metadata.getVideoFileName());
        LOGGER.info("Clip queued for rendering: {} ({})", metadata.getClipId(), style);
        
        // TODO: renderer.startRendering(replayFrames, cameraPath, outputPath.toString());
        // For now, just log that we would render
        LOGGER.info("Would render {} frames with {} camera to: {}", 
            replayFrames.size(), style, outputPath);
    }
    
    /**
     * Manually trigger clip generation (for testing).
     */
    public void generateClipNow() {
        generateClip();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public ClipManager getClipManager() {
        return clipManager;
    }
    
    public ReplayRenderer getRenderer() {
        return renderer;
    }
    
    /**
     * Get progress to next clip (0.0 to 1.0).
     */
    public float getProgress() {
        return (float) ticksSinceLastClip / CLIP_INTERVAL_TICKS;
    }
    
    /**
     * Get seconds until next clip.
     */
    public int getSecondsUntilNextClip() {
        return (CLIP_INTERVAL_TICKS - ticksSinceLastClip) / 20;
    }
}
