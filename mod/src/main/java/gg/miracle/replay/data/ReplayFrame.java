package gg.miracle.replay.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of world state at a single tick.
 * Optimized for memory efficiency - only stores essential data.
 * 
 * Memory estimate: ~2.6 KB per frame
 * - Player state: ~100 bytes
 * - 50 entities × 50 bytes = 2.5 KB
 */
public class ReplayFrame {
    private final long timestamp;        // System time in ms
    private final int tickNumber;        // Tick counter for synchronization
    
    // Player state
    private final double playerX, playerY, playerZ;
    private final float playerYaw, playerPitch;
    private final double playerVelX, playerVelY, playerVelZ;
    private final boolean playerOnGround;
    private final float playerHealth;
    
    // Camera context (for determining highlight type)
    private final boolean isInAir;
    private final double fallDistance;
    
    // Entity tracking (limited to nearby entities)
    private final List<EntitySnapshot> entities;
    
    // World context
    private final int worldTime;
    
    // Metadata
    private transient int highlightScore;  // Calculated after capture
    
    public ReplayFrame(long timestamp, int tickNumber,
                      double playerX, double playerY, double playerZ,
                      float playerYaw, float playerPitch,
                      double playerVelX, double playerVelY, double playerVelZ,
                      boolean playerOnGround, float playerHealth,
                      boolean isInAir, double fallDistance,
                      List<EntitySnapshot> entities, int worldTime) {
        this.timestamp = timestamp;
        this.tickNumber = tickNumber;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.playerYaw = playerYaw;
        this.playerPitch = playerPitch;
        this.playerVelX = playerVelX;
        this.playerVelY = playerVelY;
        this.playerVelZ = playerVelZ;
        this.playerOnGround = playerOnGround;
        this.playerHealth = playerHealth;
        this.isInAir = isInAir;
        this.fallDistance = fallDistance;
        this.entities = entities != null ? entities : new ArrayList<>();
        this.worldTime = worldTime;
        this.highlightScore = 0;
    }
    
    // Getters
    public long getTimestamp() { return timestamp; }
    public int getTickNumber() { return tickNumber; }
    
    public double getPlayerX() { return playerX; }
    public double getPlayerY() { return playerY; }
    public double getPlayerZ() { return playerZ; }
    public float getPlayerYaw() { return playerYaw; }
    public float getPlayerPitch() { return playerPitch; }
    
    public double getPlayerVelX() { return playerVelX; }
    public double getPlayerVelY() { return playerVelY; }
    public double getPlayerVelZ() { return playerVelZ; }
    
    public boolean isPlayerOnGround() { return playerOnGround; }
    public float getPlayerHealth() { return playerHealth; }
    
    public boolean isInAir() { return isInAir; }
    public double getFallDistance() { return fallDistance; }
    
    public List<EntitySnapshot> getEntities() { return entities; }
    public int getWorldTime() { return worldTime; }
    
    public int getHighlightScore() { return highlightScore; }
    public void setHighlightScore(int score) { this.highlightScore = score; }
}
