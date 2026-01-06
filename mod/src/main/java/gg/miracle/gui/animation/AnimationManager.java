package gg.miracle.gui.animation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Manages multiple animations with automatic creation and cleanup.
 * Animations are stored by key for easy retrieval.
 */
public class AnimationManager {

    private final Map<String, Animation> animations = new ConcurrentHashMap<>();
    private final Map<String, Float> hoverStates = new ConcurrentHashMap<>();

    // Global pulse animation for glow effects
    private float pulseValue = 0;
    private long pulseStartTime = System.currentTimeMillis();
    private static final int PULSE_DURATION_MS = 2000;

    /**
     * Update all animations. Call this each frame.
     */
    public void tick() {
        // Update pulse
        long elapsed = System.currentTimeMillis() - pulseStartTime;
        float pulseProgress = (elapsed % PULSE_DURATION_MS) / (float) PULSE_DURATION_MS;
        pulseValue = (float) Math.sin(pulseProgress * Math.PI * 2) * 0.5f + 0.5f;

        // Update all animations
        animations.values().forEach(Animation::tick);

        // Remove finished non-essential animations to prevent memory leaks
        // (Keep them for now - they hold final values)
    }

    /**
     * Get the global pulse value (0 to 1, oscillating).
     */
    public float getPulse() {
        return pulseValue;
    }

    /**
     * Get or create an animation.
     */
    public Animation getOrCreate(String key, Supplier<Animation> creator) {
        return animations.computeIfAbsent(key, k -> creator.get());
    }

    /**
     * Get an animation by key.
     */
    public Animation get(String key) {
        return animations.get(key);
    }

    /**
     * Get animation value, or default if not exists.
     */
    public float getValue(String key, float defaultValue) {
        Animation anim = animations.get(key);
        return anim != null ? anim.getValue() : defaultValue;
    }

    /**
     * Create or update a hover animation based on hover state.
     */
    public float getHoverAnimation(String key, boolean isHovered, int durationMs) {
        Animation anim = animations.computeIfAbsent(key, k -> Animation.hover(durationMs));

        float targetValue = isHovered ? 1.0f : 0.0f;
        float currentTarget = anim.getTo();

        if (Math.abs(currentTarget - targetValue) > 0.001f) {
            anim.setTarget(targetValue);
        }

        anim.tick();
        return anim.getValue();
    }

    /**
     * Create or update a toggle animation.
     */
    public float getToggleAnimation(String key, boolean isEnabled, int durationMs) {
        Animation anim = animations.computeIfAbsent(key, k -> Animation.toggle(durationMs));

        float targetValue = isEnabled ? 1.0f : 0.0f;
        float currentTarget = anim.getTo();

        if (Math.abs(currentTarget - targetValue) > 0.001f) {
            anim.setTarget(targetValue);
        }

        return anim.getValue();
    }

    /**
     * Create or update an expand animation.
     */
    public float getExpandAnimation(String key, boolean isExpanded, int durationMs) {
        Animation anim = animations.computeIfAbsent(key, k -> Animation.expand(durationMs));

        float targetValue = isExpanded ? 1.0f : 0.0f;
        float currentTarget = anim.getTo();

        if (Math.abs(currentTarget - targetValue) > 0.001f) {
            anim.setTarget(targetValue);
        }

        return anim.getValue();
    }

    /**
     * Track hover state and return smooth interpolated value.
     * This is simpler than full animations for simple hover effects.
     */
    public float trackHover(String key, boolean isHovered, float speed) {
        float current = hoverStates.getOrDefault(key, 0f);
        float target = isHovered ? 1f : 0f;

        if (Math.abs(current - target) > 0.001f) {
            current = lerp(current, target, speed);
            hoverStates.put(key, current);
        }

        return current;
    }

    /**
     * Remove an animation.
     */
    public void remove(String key) {
        animations.remove(key);
        hoverStates.remove(key);
    }

    /**
     * Clear all animations.
     */
    public void clear() {
        animations.clear();
        hoverStates.clear();
    }

    /**
     * Check if an animation exists and is playing.
     */
    public boolean isPlaying(String key) {
        Animation anim = animations.get(key);
        return anim != null && anim.isPlaying();
    }

    /**
     * Linear interpolation.
     */
    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /**
     * Get number of active animations.
     */
    public int getAnimationCount() {
        return animations.size();
    }

    /**
     * Create a key for a module animation.
     */
    public static String moduleKey(String moduleName, String type) {
        return "module_" + moduleName + "_" + type;
    }

    /**
     * Create a key for a setting animation.
     */
    public static String settingKey(String moduleName, String settingName, String type) {
        return "setting_" + moduleName + "_" + settingName + "_" + type;
    }

    /**
     * Create a key for a category animation.
     */
    public static String categoryKey(String categoryName, String type) {
        return "category_" + categoryName + "_" + type;
    }
}
