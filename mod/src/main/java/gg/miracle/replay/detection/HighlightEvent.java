package gg.miracle.replay.detection;

/**
 * Types of highlight-worthy events that can occur during gameplay.
 * Each event has a base score that contributes to determining if a clip should be saved.
 */
public enum HighlightEvent {
    KILL(40, "Kill"),
    PVP_KILL(60, "PvP Kill"),
    DEATH(25, "Death"),
    CLUTCH_SURVIVAL(50, "Clutch Survival"),
    LONG_FALL_SURVIVAL(30, "Long Fall Survival"),
    MULTI_KILL(60, "Multi-Kill"),
    CRITICAL_HIT(20, "Critical Hit"),
    PARKOUR_JUMP(25, "Parkour Jump");
    
    private final int baseScore;
    private final String displayName;
    
    HighlightEvent(int baseScore, String displayName) {
        this.baseScore = baseScore;
        this.displayName = displayName;
    }
    
    public int getBaseScore() {
        return baseScore;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
