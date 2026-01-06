package gg.miracle.gui.menu;

import net.minecraft.client.gui.DrawContext;

/**
 * Interface for tab content renderers in the Miracle Client menu.
 * Each tab implements this to render its specific content in the ribbon.
 */
public interface TabContent {

    /**
     * Initialize the tab content (called when tab becomes active).
     */
    default void init() {}

    /**
     * Render the tab content within the ribbon area.
     * @param context Draw context
     * @param x Left edge of content area
     * @param y Top edge of content area (below title)
     * @param width Available width
     * @param height Available height
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param delta Frame delta time
     */
    void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta);

    /**
     * Handle mouse click within the content area (legacy - triggers on release).
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button (0=left, 1=right, 2=middle)
     * @return true if click was handled
     */
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Handle mouse click with press/release state.
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button (0=left, 1=right, 2=middle)
     * @param down true if mouse pressed, false if released
     * @return true if click was handled
     */
    default boolean mouseClicked(double mouseX, double mouseY, int button, boolean down) {
        // Default: only handle release, delegate to legacy method
        if (!down) {
            return mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    /**
     * Handle mouse scroll within the content area.
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param amount Scroll amount (positive=up, negative=down)
     * @return true if scroll was handled
     */
    default boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return false;
    }

    /**
     * Handle mouse drag.
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @param deltaX X delta
     * @param deltaY Y delta
     * @return true if drag was handled
     */
    default boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Handle mouse release.
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @return true if release was handled
     */
    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Handle key press.
     * @param keyCode GLFW key code
     * @param scanCode Scan code
     * @param modifiers Modifier keys
     * @return true if key was handled
     */
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Handle character typed.
     * @param chr Character typed
     * @param modifiers Modifier keys
     * @return true if character was handled
     */
    default boolean charTyped(char chr, int modifiers) {
        return false;
    }

    /**
     * Called when this tab becomes inactive (another tab selected).
     */
    default void onDeactivate() {}

    /**
     * Called every tick while this tab is active.
     */
    default void tick() {}
}
