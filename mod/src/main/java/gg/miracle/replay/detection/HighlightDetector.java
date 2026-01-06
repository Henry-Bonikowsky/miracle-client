package gg.miracle.replay.detection;

import gg.miracle.MiracleClient;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Analyzes gameplay events and assigns highlight scores.
 * Triggers clip creation when score threshold is met.
 */
public class HighlightDetector {
    private static final int SCORE_THRESHOLD = 30;
    private static final int MULTI_KILL_WINDOW_MS = 3000;  // 3 seconds
    private static final int COMBO_MULTIPLIER = 15;
    
    // Track recent events (last 5 seconds)
    private final Queue<EventRecord> recentEvents;
    private long lastKillTime = 0;
    private int killStreak = 0;
    
    public HighlightDetector() {
        this.recentEvents = new LinkedList<>();
    }
    
    /**
     * Register a highlight event.
     * @param event The type of event that occurred
     */
    public void registerEvent(HighlightEvent event) {
        long currentTime = System.currentTimeMillis();
        
        // Add to recent events
        recentEvents.add(new EventRecord(event, currentTime));
        
        // Clean up old events (older than 5 seconds)
        cleanupOldEvents(currentTime);
        
        // Check for multi-kill streak
        if (event == HighlightEvent.KILL || event == HighlightEvent.PVP_KILL) {
            if (currentTime - lastKillTime < MULTI_KILL_WINDOW_MS) {
                killStreak++;
                MiracleClient.LOGGER.info("Kill streak: {}", killStreak);
            } else {
                killStreak = 1;
            }
            lastKillTime = currentTime;
        }
        
        // Calculate total score
        int score = calculateHighlightScore();
        
        // Log the event
        MiracleClient.LOGGER.info("Highlight event: {} (score: {})", event.getDisplayName(), score);
        
        // Check if this should trigger a clip
        if (shouldSaveHighlight(score)) {
            MiracleClient.LOGGER.info("HIGHLIGHT DETECTED! Score: {} (threshold: {})", score, SCORE_THRESHOLD);
            // TODO: Trigger clip creation in Phase 6
        }
    }
    
    /**
     * Calculate the total highlight score based on recent events.
     */
    public int calculateHighlightScore() {
        int totalScore = 0;
        long currentTime = System.currentTimeMillis();
        
        // Add base scores for all recent events
        for (EventRecord record : recentEvents) {
            totalScore += record.event.getBaseScore();
        }
        
        // Add multi-kill bonus
        if (killStreak > 1) {
            int multiKillBonus = HighlightEvent.MULTI_KILL.getBaseScore();
            multiKillBonus += (killStreak - 2) * COMBO_MULTIPLIER;
            totalScore += multiKillBonus;
        }
        
        return totalScore;
    }
    
    /**
     * Check if a highlight should be saved based on score.
     */
    public boolean shouldSaveHighlight(int score) {
        return score >= SCORE_THRESHOLD;
    }
    
    /**
     * Get recent events for analysis.
     */
    public Queue<EventRecord> getRecentEvents() {
        return recentEvents;
    }
    
    /**
     * Clean up events older than 5 seconds.
     */
    private void cleanupOldEvents(long currentTime) {
        while (!recentEvents.isEmpty()) {
            EventRecord oldest = recentEvents.peek();
            if (currentTime - oldest.timestamp > 5000) {
                recentEvents.poll();
            } else {
                break;
            }
        }
    }
    
    /**
     * Record of a highlight event with timestamp.
     */
    public static class EventRecord {
        public final HighlightEvent event;
        public final long timestamp;
        
        public EventRecord(HighlightEvent event, long timestamp) {
            this.event = event;
            this.timestamp = timestamp;
        }
    }
}
