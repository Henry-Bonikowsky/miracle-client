package gg.miracle.replay.data;

import net.minecraft.entity.EntityType;

/**
 * Lightweight entity state snapshot.
 * Memory: ~50 bytes per entity
 */
public class EntitySnapshot {
    private final int entityId;
    private final EntityType<?> type;
    private final double x, y, z;
    private final float yaw, pitch;
    private final boolean isDead;     // For death animations
    private final float health;       // For damage tracking
    
    public EntitySnapshot(int entityId, EntityType<?> type,
                         double x, double y, double z,
                         float yaw, float pitch,
                         boolean isDead, float health) {
        this.entityId = entityId;
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.isDead = isDead;
        this.health = health;
    }
    
    // Getters
    public int getEntityId() { return entityId; }
    public EntityType<?> getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public boolean isDead() { return isDead; }
    public float getHealth() { return health; }
}
