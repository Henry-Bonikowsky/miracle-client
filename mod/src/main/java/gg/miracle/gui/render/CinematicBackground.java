package gg.miracle.gui.render;

import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.gui.DrawContext;

/**
 * Simple gradient background matching the launcher's no-clip fallback.
 */
public class CinematicBackground {

    public CinematicBackground() {
    }

    /**
     * Render gradient background matching launcher style.
     */
    public void render(DrawContext context, int width, int height) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Main gradient (top-left to bottom-right approximation)
        // Draw as top-to-bottom, the color choice gives diagonal feel
        context.fillGradient(0, 0, width, height, theme.bgFrom, theme.bgTo);

        // Bottom fade for content area (like launcher)
        int fadeHeight = height / 3;
        int fadeStart = height - fadeHeight;
        context.fillGradient(0, fadeStart, width, height, 0x00000000, MiracleTheme.withAlpha(theme.bgFrom, 0xCC));
    }

    /**
     * Draw a glass panel with the current theme's glass styling.
     * @param context Draw context
     * @param x Left position
     * @param y Top position
     * @param width Panel width
     * @param height Panel height
     */
    public static void drawGlassPanel(DrawContext context, int x, int y, int width, int height) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Fill with glass background
        context.fill(x, y, x + width, y + height, theme.glassBg);

        // Draw border
        int borderColor = theme.glassBorder;
        // Top
        context.fill(x, y, x + width, y + 1, borderColor);
        // Bottom
        context.fill(x, y + height - 1, x + width, y + height, borderColor);
        // Left
        context.fill(x, y, x + 1, y + height, borderColor);
        // Right
        context.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    /**
     * Draw a glass panel with rounded corners (approximate using corner cutoffs).
     * @param context Draw context
     * @param x Left position
     * @param y Top position
     * @param width Panel width
     * @param height Panel height
     * @param radius Corner radius
     */
    public static void drawGlassPanelRounded(DrawContext context, int x, int y, int width, int height, int radius) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // For simplicity, draw main body and handle corners
        // Main body (excluding corners)
        context.fill(x + radius, y, x + width - radius, y + height, theme.glassBg);
        context.fill(x, y + radius, x + radius, y + height - radius, theme.glassBg);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, theme.glassBg);

        // Draw corner arcs (approximated as filled quarter circles)
        drawCornerFill(context, x + radius, y + radius, radius, theme.glassBg, 0);           // Top-left
        drawCornerFill(context, x + width - radius - 1, y + radius, radius, theme.glassBg, 1);  // Top-right
        drawCornerFill(context, x + radius, y + height - radius - 1, radius, theme.glassBg, 2); // Bottom-left
        drawCornerFill(context, x + width - radius - 1, y + height - radius - 1, radius, theme.glassBg, 3); // Bottom-right

        // Border
        int borderColor = theme.glassBorder;
        // Top edge
        context.fill(x + radius, y, x + width - radius, y + 1, borderColor);
        // Bottom edge
        context.fill(x + radius, y + height - 1, x + width - radius, y + height, borderColor);
        // Left edge
        context.fill(x, y + radius, x + 1, y + height - radius, borderColor);
        // Right edge
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, borderColor);
    }

    /**
     * Draw a filled quarter circle for rounded corners.
     * @param quadrant 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
     */
    private static void drawCornerFill(DrawContext context, int centerX, int centerY, int radius, int color, int quadrant) {
        for (int dy = 0; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(radius * radius - dy * dy);
            int x1, x2, y;
            switch (quadrant) {
                case 0: // Top-left
                    x1 = centerX - dx;
                    x2 = centerX;
                    y = centerY - dy;
                    break;
                case 1: // Top-right
                    x1 = centerX;
                    x2 = centerX + dx;
                    y = centerY - dy;
                    break;
                case 2: // Bottom-left
                    x1 = centerX - dx;
                    x2 = centerX;
                    y = centerY + dy;
                    break;
                case 3: // Bottom-right
                default:
                    x1 = centerX;
                    x2 = centerX + dx;
                    y = centerY + dy;
                    break;
            }
            if (x2 > x1) {
                context.fill(x1, y, x2, y + 1, color);
            }
        }
    }
}
