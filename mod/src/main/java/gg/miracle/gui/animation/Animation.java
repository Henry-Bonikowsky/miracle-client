package gg.miracle.gui.animation;

/**
 * A single animation that interpolates between two values over time.
 */
public class Animation {

    private float from;
    private float to;
    private float current;
    private final int durationMs;
    private final Easing easing;

    private long startTime = -1;
    private boolean playing = false;
    private boolean finished = false;
    private boolean reversed = false;

    private Runnable onComplete;

    /**
     * Create a new animation.
     * @param from Starting value
     * @param to Ending value
     * @param durationMs Duration in milliseconds
     * @param easing Easing function
     */
    public Animation(float from, float to, int durationMs, Easing easing) {
        this.from = from;
        this.to = to;
        this.current = from;
        this.durationMs = durationMs;
        this.easing = easing;
    }

    /**
     * Create a new animation with default easing (EASE_OUT_QUAD).
     */
    public Animation(float from, float to, int durationMs) {
        this(from, to, durationMs, Easing.EASE_OUT_QUAD);
    }

    /**
     * Start the animation from the beginning.
     */
    public Animation start() {
        this.startTime = System.currentTimeMillis();
        this.playing = true;
        this.finished = false;
        this.reversed = false;
        this.current = from;
        return this;
    }

    /**
     * Start the animation in reverse.
     */
    public Animation startReverse() {
        this.startTime = System.currentTimeMillis();
        this.playing = true;
        this.finished = false;
        this.reversed = true;
        this.current = to;
        return this;
    }

    /**
     * Animate to a new target value, starting from current position.
     */
    public Animation animateTo(float newTarget) {
        this.from = this.current;
        this.to = newTarget;
        return start();
    }

    /**
     * Set target and animate if different from current target.
     */
    public void setTarget(float target) {
        if (Math.abs(this.to - target) > 0.001f || !playing) {
            animateTo(target);
        }
    }

    /**
     * Update the animation. Should be called each frame.
     */
    public void tick() {
        if (!playing || startTime < 0) {
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        float progress = Math.min(1.0f, (float) elapsed / durationMs);

        float easedProgress = easing.apply(progress);

        if (reversed) {
            current = to + (from - to) * easedProgress;
        } else {
            current = from + (to - from) * easedProgress;
        }

        if (progress >= 1.0f) {
            playing = false;
            finished = true;
            current = reversed ? from : to;

            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    /**
     * Get the current interpolated value.
     */
    public float getValue() {
        return current;
    }

    /**
     * Get the current value as an integer.
     */
    public int getValueInt() {
        return Math.round(current);
    }

    /**
     * Check if the animation is currently playing.
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * Check if the animation has finished.
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Get progress from 0 to 1.
     */
    public float getProgress() {
        if (startTime < 0 || !playing) {
            return finished ? 1.0f : 0.0f;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0f, (float) elapsed / durationMs);
    }

    /**
     * Set a callback to run when animation completes.
     */
    public Animation onComplete(Runnable callback) {
        this.onComplete = callback;
        return this;
    }

    /**
     * Stop the animation at current value.
     */
    public void stop() {
        this.playing = false;
    }

    /**
     * Reset to the starting value.
     */
    public void reset() {
        this.current = from;
        this.playing = false;
        this.finished = false;
        this.startTime = -1;
    }

    /**
     * Immediately set to the end value.
     */
    public void finish() {
        this.current = reversed ? from : to;
        this.playing = false;
        this.finished = true;
    }

    /**
     * Get the starting value.
     */
    public float getFrom() {
        return from;
    }

    /**
     * Get the ending value.
     */
    public float getTo() {
        return to;
    }

    /**
     * Set new from/to values without starting.
     */
    public void setRange(float from, float to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Create a hover animation (0 to 1 over duration).
     */
    public static Animation hover(int durationMs) {
        return new Animation(0, 1, durationMs, Easing.EASE_OUT_QUAD);
    }

    /**
     * Create a toggle animation (0 to 1 or 1 to 0).
     */
    public static Animation toggle(int durationMs) {
        return new Animation(0, 1, durationMs, Easing.EASE_IN_OUT_QUAD);
    }

    /**
     * Create an expand/collapse animation.
     */
    public static Animation expand(int durationMs) {
        return new Animation(0, 1, durationMs, Easing.EASE_OUT_CUBIC);
    }

    /**
     * Create a pulse animation (for glow effects).
     */
    public static Animation pulse(int durationMs) {
        return new Animation(0, 1, durationMs, Easing.EASE_IN_OUT_QUAD);
    }
}
