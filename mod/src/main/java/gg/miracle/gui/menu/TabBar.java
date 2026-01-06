package gg.miracle.gui.menu;

import gg.miracle.gui.UISounds;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.gui.DrawContext;
//? if >=1.21.6
import net.minecraft.client.gl.RenderPipelines;
//? if <1.21.6
/*import net.minecraft.client.render.RenderLayer;*/

/**
 * Left-side icon tab bar for the Miracle Client menu.
 * Displays icons for each tab with selection indicator.
 */
public class TabBar {

    public static final int WIDTH = 40;
    private static final int TAB_HEIGHT = 40;
    private static final int TAB_PADDING = 4;
    private static final int ICON_SIZE = 16;

    private Tab selectedTab = Tab.HOME;
    private Tab hoveredTab = null;

    private int x, y, height;

    /**
     * Set the position and size of the tab bar.
     */
    public void setBounds(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
    }

    /**
     * Render the tab bar.
     */
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Glass background for tab bar
        context.fill(x, y, x + WIDTH, y + height, theme.glassBg);
        // Right border
        context.fill(x + WIDTH - 1, y, x + WIDTH, y + height, theme.glassBorder);

        // Update hovered state
        hoveredTab = null;
        if (mouseX >= x && mouseX < x + WIDTH) {
            int tabY = y + TAB_PADDING;
            for (Tab tab : Tab.values()) {
                if (mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
                    hoveredTab = tab;
                    break;
                }
                tabY += TAB_HEIGHT;
            }
        }

        // Render each tab
        int tabY = y + TAB_PADDING;
        for (Tab tab : Tab.values()) {
            boolean isSelected = tab == selectedTab;
            boolean isHovered = tab == hoveredTab;

            // Play hover sound on hover enter
            UISounds.checkHover(tab, isHovered);

            // Tab background
            if (isSelected) {
                // Selected: accent background
                context.fill(x + 2, tabY, x + WIDTH - 2, tabY + TAB_HEIGHT - 2,
                    MiracleTheme.withAlpha(theme.accent500, 0x40));
                // Accent indicator bar on left
                context.fill(x, tabY + 4, x + 3, tabY + TAB_HEIGHT - 6, theme.accent500);
            } else if (isHovered) {
                // Hovered: subtle highlight
                context.fill(x + 2, tabY, x + WIDTH - 2, tabY + TAB_HEIGHT - 2,
                    MiracleTheme.withAlpha(theme.surfaceSecondary, 0x60));
            }

            // Tab icon (centered)
            int iconX = x + (WIDTH - ICON_SIZE) / 2;
            int iconY = tabY + (TAB_HEIGHT - 2 - ICON_SIZE) / 2;

            // Tint color based on state
            int tint = isSelected ? theme.accent400 :
                      (isHovered ? theme.textPrimary : theme.textSecondary);
            // Convert RGB to ARGB (full opacity)
            int color = 0xFF000000 | (tint & 0x00FFFFFF);

            // Draw icon texture with tint
            //? if >=1.21.6 {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, tab.getIconTexture(), iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, color);
            //?} else {
            /*context.drawTexture(RenderLayer::getGuiTextured, tab.getIconTexture(), iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, color);
            *///?}

            tabY += TAB_HEIGHT;
        }
    }

    /**
     * Handle mouse click on tab bar.
     * @return The clicked tab, or null if no tab was clicked
     */
    public Tab handleClick(double mouseX, double mouseY, int button) {
        if (button != 0) return null;  // Left click only
        if (mouseX < x || mouseX >= x + WIDTH) return null;

        int tabY = y + TAB_PADDING;
        for (Tab tab : Tab.values()) {
            if (mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
                UISounds.click();
                return tab;
            }
            tabY += TAB_HEIGHT;
        }
        return null;
    }

    /**
     * Get the currently selected tab.
     */
    public Tab getSelectedTab() {
        return selectedTab;
    }

    /**
     * Set the selected tab.
     */
    public void setSelectedTab(Tab tab) {
        this.selectedTab = tab;
    }

    /**
     * Get the total height needed for all tabs.
     */
    public int getRequiredHeight() {
        return TAB_PADDING * 2 + Tab.values().length * TAB_HEIGHT;
    }
}
