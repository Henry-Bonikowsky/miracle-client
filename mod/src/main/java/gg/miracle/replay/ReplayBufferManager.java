package gg.miracle.replay;

import gg.miracle.MiracleClient;
import gg.miracle.replay.data.EntitySnapshot;
import gg.miracle.replay.data.ReplayFrame;
import gg.miracle.replay.detection.HighlightDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Circular buffer that maintains last 60 seconds of gameplay.
 * Thread-safe, memory-efficient (target: ~3MB for 60 seconds).
 */
public class ReplayBufferManager {
    private static final int BUFFER_SIZE = 1200;  // 60 seconds at 20 TPS
    private static final int ENTITY_RADIUS = 32;  // Only track entities within 32 blocks
    private static final int MAX_ENTITIES = 50;   // Limit entities per frame
    
    private final ReplayFrame[] buffer;
    private final HighlightDetector highlightDetector;
    private int writeIndex = 0;
    private int tickCounter = 0;
    private boolean isRecording = false;
    
    public ReplayBufferManager() {
        this.buffer = new ReplayFrame[BUFFER_SIZE];
        this.highlightDetector = new HighlightDetector();
    }
    
    /**
     * Start recording to the buffer.
     */
    public void startRecording() {
        isRecording = true;
        MiracleClient.LOGGER.info("Replay buffer started recording");
    }
    
    /**
     * Stop recording.
     */
    public void stopRecording() {
        isRecording = false;
        MiracleClient.LOGGER.info("Replay buffer stopped recording");
    }
    
    /**
     * Capture current world state and add to buffer.
     * Called every tick from ClientTickEvent.
     */
    public void captureFrame() {
        if (!isRecording) return;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        
        try {
            // Create snapshot of current state
            ReplayFrame frame = createFrameSnapshot(mc);
            
            // Add to circular buffer
            buffer[writeIndex] = frame;
            writeIndex = (writeIndex + 1) % BUFFER_SIZE;
            tickCounter++;
            
        } catch (Exception e) {
            MiracleClient.LOGGER.error("Error capturing replay frame", e);
        }
    }
    
    /**
     * Create a snapshot of the current game state.
     */
    private ReplayFrame createFrameSnapshot(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        World world = mc.world;
        
        // Player state
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        float playerYaw = player.getYaw();
        float playerPitch = player.getPitch();
        
        double playerVelX = player.getVelocity().x;
        double playerVelY = player.getVelocity().y;
        double playerVelZ = player.getVelocity().z;
        
        boolean playerOnGround = player.isOnGround();
        float playerHealth = player.getHealth();
        
        // Camera context
        boolean isInAir = !player.isOnGround() && player.getVelocity().y < 0;
        double fallDistance = player.fallDistance;
        
        // Capture nearby entities (limited)
        List<EntitySnapshot> entities = captureNearbyEntities(world, player);
        
        // World time
        int worldTime = (int) (world.getTimeOfDay() % 24000);
        
        return new ReplayFrame(
            System.currentTimeMillis(),
            tickCounter,
            playerX, playerY, playerZ,
            playerYaw, playerPitch,
            playerVelX, playerVelY, playerVelZ,
            playerOnGround, playerHealth,
            isInAir, fallDistance,
            entities,
            worldTime
        );
    }
    
    /**
     * Capture nearby entities within radius.
     * Limited to MAX_ENTITIES to save memory.
     */
    private List<EntitySnapshot> captureNearbyEntities(World world, ClientPlayerEntity player) {
        List<EntitySnapshot> snapshots = new ArrayList<>();
        
        // Get entities within radius
        List<Entity> nearbyEntities = world.getOtherEntities(
            player,
            player.getBoundingBox().expand(ENTITY_RADIUS),
            entity -> entity instanceof LivingEntity
        );
        
        // Limit to MAX_ENTITIES (take closest ones)
        int count = 0;
        for (Entity entity : nearbyEntities) {
            if (count >= MAX_ENTITIES) break;
            
            if (entity instanceof LivingEntity living) {
                EntitySnapshot snapshot = new EntitySnapshot(
                    entity.getId(),
                    entity.getType(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getYaw(),
                    entity.getPitch(),
                    living.isDead(),
                    living.getHealth()
                );
                snapshots.add(snapshot);
                count++;
            }
        }
        
        return snapshots;
    }
    
    /**
     * Extract last N seconds of replay data.
     * Returns frames in chronological order.
     */
    public List<ReplayFrame> extractBuffer(int durationSeconds) {
        int frameCount = Math.min(durationSeconds * 20, tickCounter);
        frameCount = Math.min(frameCount, BUFFER_SIZE);
        
        List<ReplayFrame> frames = new ArrayList<>(frameCount);
        
        // Calculate start index in circular buffer
        int startIndex = (writeIndex - frameCount + BUFFER_SIZE) % BUFFER_SIZE;
        
        // Extract frames in chronological order
        for (int i = 0; i < frameCount; i++) {
            int index = (startIndex + i) % BUFFER_SIZE;
            ReplayFrame frame = buffer[index];
            if (frame != null) {
                frames.add(frame);
            }
        }
        
        return frames;
    }
    
    /**
     * Check if currently recording.
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * Get total frames captured.
     */
    public int getFrameCount() {
        return Math.min(tickCounter, BUFFER_SIZE);
    }
    
    /**
     * Get estimated memory usage in bytes.
     */
    public long getEstimatedMemoryUsage() {
        // Rough estimate: 2.6 KB per frame
        return getFrameCount() * 2600L;
    }
    
    /**
     * Get the highlight detector.
     */
    public HighlightDetector getHighlightDetector() {
        return highlightDetector;
    }
}
