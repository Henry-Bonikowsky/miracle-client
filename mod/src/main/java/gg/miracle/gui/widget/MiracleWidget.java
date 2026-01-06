package gg.miracle.gui.widget;

import gg.miracle.gui.UISounds;
import gg.miracle.gui.animation.Animation;
import gg.miracle.gui.animation.Easing;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.gui.DrawContext;

/**
 * Base class for all Miracle UI components.
 * Provides automatic hover sounds, animations, and theme access.
 */
public abstract class MiracleWidget {

    protected int x, y, width, height;
    protected boolean enabled = true;
    protected boolean visible = true;
    protected boolean hovered = false;
    protected boolean pressed = false;  // Mouse is held down on this widget

    // Hover animation (0 = not hovered, 1 = fully hovered)
    protected final Animation hoverAnim;
    // Press animation (0 = not pressed, 1 = fully pressed)
    protected final Animation pressAnim;

    public MiracleWidget() {
        this.hoverAnim = new Animation(0, 1, MiracleTheme.ANIM_QUICK, Easing.EASE_OUT_QUAD);
        this.pressAnim = new Animation(0, 1, MiracleTheme.ANIM_QUICK, Easing.EASE_OUT_QUAD);
    }

    /**
     * Set the widget bounds.
     */
    public MiracleWidget setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Set position only.
     */
    public MiracleWidget setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Set size only.
     */
    public MiracleWidget setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public MiracleWidget setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public MiracleWidget setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Check if mouse is over this widget.
     */
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width &&
               mouseY >= y && mouseY < y + height;
    }

    /**
     * Main render method - handles hover detection and sounds automatically.
     * Subclasses implement renderWidget() for actual drawing.
     */
    public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        // Update hover state
        boolean wasHovered = hovered;
        hovered = enabled && isMouseOver(mouseX, mouseY);

        // Play hover sound on enter
        UISounds.checkHover(this, hovered);

        // Update hover animation
        hoverAnim.setTarget(hovered ? 1.0f : 0.0f);
        hoverAnim.tick();

        // Update press animation
        pressAnim.setTarget(pressed ? 1.0f : 0.0f);
        pressAnim.tick();

        // Render the actual widget
        renderWidget(context, mouseX, mouseY, delta);
    }

    /**
     * Subclasses implement this to draw the widget.
     */
    protected abstract void renderWidget(DrawContext context, int mouseX, int mouseY, float delta);

    /**
     * Handle mouse click. Returns true if click was consumed.
     * For older MC versions - triggers on click.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        if (button != 0) return false;  // Left click only by default
        return isMouseOver(mouseX, mouseY) && onClicked();
    }

    /**
     * Handle mouse press (down=true) or release (down=false).
     * For MC 1.21.11+ with Click API.
     * Note: MC 1.21.11 only sends down=false events, so we trigger on release.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean down) {
        if (!visible || !enabled) return false;
        if (button != 0) return false;  // Left click only

        if (down) {
            // Mouse pressed - start press state if over widget
            if (isMouseOver(mouseX, mouseY)) {
                pressed = true;
                return true;
            }
        } else {
            // Mouse released
            if (pressed) {
                // Normal case: was pressed, now releasing
                pressed = false;
                if (isMouseOver(mouseX, mouseY)) {
                    return onClicked();
                }
            } else if (isMouseOver(mouseX, mouseY)) {
                // Fallback: MC 1.21.11 only sends release events, trigger click directly
                return onClicked();
            }
        }
        return false;
    }

    /**
     * Called when widget is clicked. Override to handle clicks.
     * Return true to consume the click.
     */
    protected boolean onClicked() {
        return false;
    }

    /**
     * Handle mouse release.
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Handle mouse drag.
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Handle mouse scroll.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isEnabled() { return enabled; }
    public boolean isVisible() { return visible; }
    public boolean isHovered() { return hovered; }
    public boolean isPressed() { return pressed; }
    public float getHoverProgress() { return hoverAnim.getValue(); }
    public float getPressProgress() { return pressAnim.getValue(); }

    /**
     * Get the current theme.
     */
    protected MiracleTheme.Theme theme() {
        return MiracleTheme.current();
    }
}
