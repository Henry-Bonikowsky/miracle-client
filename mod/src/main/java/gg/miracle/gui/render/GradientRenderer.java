package gg.miracle.gui.render;

import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Utility class for rendering gradients, glows, and other premium visual effects.
 */
public final class GradientRenderer {

    private GradientRenderer() {} // Prevent instantiation

    /**
     * Fill a rectangle with a vertical gradient (top to bottom).
     */
    public static void fillVerticalGradient(DrawContext context, int x, int y, int width, int height,
                                            int colorTop, int colorBottom) {
        context.fillGradient(x, y, x + width, y + height, colorTop, colorBottom);
    }

    /**
     * Fill a rectangle with a horizontal gradient (left to right).
     */
    public static void fillHorizontalGradient(DrawContext context, int x, int y, int width, int height,
                                              int colorLeft, int colorRight) {
        // DrawContext.fillGradient is vertical, so we use multiple thin vertical strips for horizontal
        int steps = Math.max(1, width / 4);
        for (int i = 0; i < steps; i++) {
            float ratio = (float) i / steps;
            float nextRatio = (float) (i + 1) / steps;
            int color1 = lerpColor(colorLeft, colorRight, ratio);
            int color2 = lerpColor(colorLeft, colorRight, nextRatio);
            int stripX = x + (width * i / steps);
            int stripWidth = x + (width * (i + 1) / steps) - stripX;
            context.fillGradient(stripX, y, stripX + stripWidth, y + height, color1, color2);
        }
    }

    private static int lerpColor(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Fill a rectangle with a three-color vertical gradient.
     */
    public static void fillThreeColorGradient(DrawContext context, int x, int y, int width, int height,
                                              int colorTop, int colorMid, int colorBottom) {
        int midY = y + height / 2;
        fillVerticalGradient(context, x, y, width, height / 2, colorTop, colorMid);
        fillVerticalGradient(context, x, midY, width, height - height / 2, colorMid, colorBottom);
    }

    /**
     * Draw a soft glow rectangle (multiple layers with decreasing alpha).
     */
    public static void drawGlow(DrawContext context, int x, int y, int width, int height,
                                int color, int glowSize) {
        int baseAlpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;

        for (int i = glowSize; i > 0; i--) {
            int alpha = (int) (baseAlpha * (1.0f - (float) i / glowSize) * 0.5f);
            int glowColor = (alpha << 24) | rgb;

            context.fill(x - i, y - i, x + width + i, y + height + i, glowColor);
        }
    }

    /**
     * Draw an outer glow border around a rectangle.
     */
    public static void drawGlowBorder(DrawContext context, int x, int y, int width, int height,
                                      int color, float intensity, int glowSize) {
        int baseAlpha = (int) (((color >> 24) & 0xFF) * intensity);
        int rgb = color & 0x00FFFFFF;

        for (int i = 1; i <= glowSize; i++) {
            int alpha = (int) (baseAlpha * (1.0f - (float) i / glowSize));
            int glowColor = (alpha << 24) | rgb;

            // Top
            context.fill(x - i, y - i, x + width + i, y - i + 1, glowColor);
            // Bottom
            context.fill(x - i, y + height + i - 1, x + width + i, y + height + i, glowColor);
            // Left
            context.fill(x - i, y - i, x - i + 1, y + height + i, glowColor);
            // Right
            context.fill(x + width + i - 1, y - i, x + width + i, y + height + i, glowColor);
        }
    }

    /**
     * Draw a border with optional gradient.
     */
    public static void drawBorder(DrawContext context, int x, int y, int width, int height,
                                  int color, int thickness) {
        // Top
        context.fill(x, y, x + width, y + thickness, color);
        // Bottom
        context.fill(x, y + height - thickness, x + width, y + height, color);
        // Left
        context.fill(x, y, x + thickness, y + height, color);
        // Right
        context.fill(x + width - thickness, y, x + width, y + height, color);
    }

    /**
     * Draw a gradient border.
     */
    public static void drawGradientBorder(DrawContext context, int x, int y, int width, int height,
                                          int colorTop, int colorBottom, int thickness) {
        // Top (use top color)
        context.fill(x, y, x + width, y + thickness, colorTop);
        // Bottom (use bottom color)
        context.fill(x, y + height - thickness, x + width, y + height, colorBottom);
        // Left (gradient)
        fillVerticalGradient(context, x, y, thickness, height, colorTop, colorBottom);
        // Right (gradient)
        fillVerticalGradient(context, x + width - thickness, y, thickness, height, colorTop, colorBottom);
    }

    /**
     * Draw an inner shadow effect (darker edges fading inward).
     */
    public static void drawInnerShadow(DrawContext context, int x, int y, int width, int height,
                                       int shadowColor, int depth) {
        int baseAlpha = (shadowColor >> 24) & 0xFF;
        int rgb = shadowColor & 0x00FFFFFF;

        for (int i = 0; i < depth; i++) {
            int alpha = (int) (baseAlpha * (1.0f - (float) i / depth));
            int color = (alpha << 24) | rgb;

            // Top edge
            context.fill(x + i, y + i, x + width - i, y + i + 1, color);
            // Left edge
            context.fill(x + i, y + i, x + i + 1, y + height - i, color);
        }
    }

    /**
     * Fill a rounded rectangle (approximation using multiple rectangles).
     */
    public static void fillRoundedRect(DrawContext context, int x, int y, int width, int height,
                                       int color, int radius) {
        if (radius <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        radius = Math.min(radius, Math.min(width, height) / 2);

        // Main body (minus corners)
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        // Corner circles (approximated with filled arcs)
        drawCorner(context, x + radius, y + radius, radius, color, 0); // Top-left
        drawCorner(context, x + width - radius, y + radius, radius, color, 1); // Top-right
        drawCorner(context, x + radius, y + height - radius, radius, color, 2); // Bottom-left
        drawCorner(context, x + width - radius, y + height - radius, radius, color, 3); // Bottom-right
    }

    /**
     * Fill a rounded rectangle with a vertical gradient.
     */
    public static void fillRoundedGradient(DrawContext context, int x, int y, int width, int height,
                                           int colorTop, int colorBottom, int radius) {
        if (radius <= 0) {
            fillVerticalGradient(context, x, y, width, height, colorTop, colorBottom);
            return;
        }

        radius = Math.min(radius, Math.min(width, height) / 2);

        // Main body gradient
        fillVerticalGradient(context, x + radius, y, width - radius * 2, height, colorTop, colorBottom);
        fillVerticalGradient(context, x, y + radius, radius, height - radius * 2, colorTop, colorBottom);
        fillVerticalGradient(context, x + width - radius, y + radius, radius, height - radius * 2, colorTop, colorBottom);

        // Corners (simplified - just use interpolated colors)
        float topBlend = (float) radius / height;
        int cornerTopColor = colorTop;
        int cornerBottomColor = colorBottom;

        drawCorner(context, x + radius, y + radius, radius, cornerTopColor, 0);
        drawCorner(context, x + width - radius, y + radius, radius, cornerTopColor, 1);
        drawCorner(context, x + radius, y + height - radius, radius, cornerBottomColor, 2);
        drawCorner(context, x + width - radius, y + height - radius, radius, cornerBottomColor, 3);
    }

    /**
     * Draw a quarter circle for rounded corners.
     * @param quadrant 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
     */
    private static void drawCorner(DrawContext context, int cx, int cy, int radius, int color, int quadrant) {
        // Use simple filled rectangles to approximate corner
        for (int i = 0; i < radius; i++) {
            double angle = (Math.PI / 2) * i / radius;
            int dx = (int) (Math.cos(angle) * (radius - i));
            int dy = (int) (Math.sin(angle) * (radius - i));

            int px = cx + (quadrant == 0 || quadrant == 2 ? -dx : dx - 1);
            int py = cy + (quadrant == 0 || quadrant == 1 ? -dy : dy - 1);
            context.fill(px, py, px + 1, py + 1, color);
        }
        // Fill the remaining area with a simple approximation
        int xStart = quadrant == 0 || quadrant == 2 ? cx - radius : cx;
        int xEnd = quadrant == 0 || quadrant == 2 ? cx : cx + radius;
        int yStart = quadrant == 0 || quadrant == 1 ? cy - radius : cy;
        int yEnd = quadrant == 0 || quadrant == 1 ? cy : cy + radius;
        context.fill(xStart, yStart, xEnd, yEnd, color);
    }

    /**
     * Draw a progress bar with gradient fill.
     */
    public static void drawProgressBar(DrawContext context, int x, int y, int width, int height,
                                       float progress, int bgColor, int fillColorStart, int fillColorEnd) {
        // Background
        context.fill(x, y, x + width, y + height, bgColor);

        // Fill
        int fillWidth = (int) (width * Math.max(0, Math.min(1, progress)));
        if (fillWidth > 0) {
            fillHorizontalGradient(context, x, y, fillWidth, height, fillColorStart, fillColorEnd);
        }
    }

    /**
     * Draw a circular progress indicator.
     */
    public static void drawCircularProgress(DrawContext context, int cx, int cy, int radius,
                                            float progress, int bgColor, int fgColor, int thickness) {
        // Simplified circular progress using filled rectangles
        int segments = 32;
        int progressSegments = (int) (segments * progress);

        for (int i = 0; i < progressSegments; i++) {
            double angle = -Math.PI / 2 + (2 * Math.PI * i / segments);
            double nextAngle = -Math.PI / 2 + (2 * Math.PI * (i + 1) / segments);

            int x1 = cx + (int) (Math.cos(angle) * (radius - thickness / 2));
            int y1 = cy + (int) (Math.sin(angle) * (radius - thickness / 2));
            int x2 = cx + (int) (Math.cos(nextAngle) * (radius - thickness / 2));
            int y2 = cy + (int) (Math.sin(nextAngle) * (radius - thickness / 2));

            // Draw a small filled rect at each segment
            context.fill(Math.min(x1, x2) - 1, Math.min(y1, y2) - 1,
                        Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, fgColor);
        }
    }

    // ==================== GLASS PANEL RENDERING ====================

    /**
     * Draw a glass panel with the current theme's glass styling.
     * @param context Draw context
     * @param x Left position
     * @param y Top position
     * @param width Panel width
     * @param height Panel height
     */
    public static void fillGlassPanel(DrawContext context, int x, int y, int width, int height) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Fill with glass background
        context.fill(x, y, x + width, y + height, theme.glassBg);

        // Draw border
        drawBorder(context, x, y, width, height, theme.glassBorder, 1);
    }

    /**
     * Draw a glass panel with rounded corners.
     * @param context Draw context
     * @param x Left position
     * @param y Top position
     * @param width Panel width
     * @param height Panel height
     * @param radius Corner radius
     */
    public static void fillGlassPanelRounded(DrawContext context, int x, int y, int width, int height, int radius) {
        MiracleTheme.Theme theme = MiracleTheme.current();
        fillRoundedRect(context, x, y, width, height, theme.glassBg, radius);
        // Border would need rounded border implementation - skip for now
    }

    /**
     * Draw a glow button with theme accent color.
     * @param context Draw context
     * @param x Left position
     * @param y Top position
     * @param width Button width
     * @param height Button height
     * @param isHovered Whether the button is hovered
     * @param isPressed Whether the button is pressed
     */
    public static void drawGlowButton(DrawContext context, int x, int y, int width, int height,
                                      boolean isHovered, boolean isPressed) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Background color based on state
        int bgColor;
        if (isPressed) {
            bgColor = theme.accent700;
        } else if (isHovered) {
            bgColor = theme.accent600;
        } else {
            bgColor = theme.accent500;
        }

        // Draw glow when hovered
        if (isHovered) {
            int glowColor = MiracleTheme.withAlpha(theme.accent500, 0x40);
            drawGlow(context, x, y, width, height, glowColor, 6);
        }

        // Draw button background with rounded corners
        fillRoundedRect(context, x, y, width, height, bgColor, MiracleTheme.RADIUS_MEDIUM);
    }

    /**
     * Draw a secondary (outline) button.
     */
    public static void drawOutlineButton(DrawContext context, int x, int y, int width, int height,
                                         boolean isHovered, boolean isPressed) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Background
        int bgColor;
        if (isPressed) {
            bgColor = MiracleTheme.withAlpha(theme.accent500, 0x40);
        } else if (isHovered) {
            bgColor = MiracleTheme.withAlpha(theme.accent500, 0x20);
        } else {
            bgColor = 0x00000000; // Transparent
        }

        if ((bgColor >> 24 & 0xFF) > 0) {
            fillRoundedRect(context, x, y, width, height, bgColor, MiracleTheme.RADIUS_MEDIUM);
        }

        // Border
        int borderColor = isHovered ? theme.accent400 : theme.accent500;
        drawBorder(context, x, y, width, height, borderColor, 1);
    }

    // ==================== GRADIENT TEXT RENDERING ====================

    /**
     * Draw text with a horizontal gradient color.
     * @param context Draw context
     * @param text Text to render
     * @param x X position
     * @param y Y position
     * @param colorStart Starting color (left)
     * @param colorEnd Ending color (right)
     */
    public static void drawGradientText(DrawContext context, String text, int x, int y,
                                        int colorStart, int colorEnd) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (text == null || text.isEmpty()) return;

        int totalWidth = client.textRenderer.getWidth(text);
        int currentX = x;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String charStr = String.valueOf(c);
            int charWidth = client.textRenderer.getWidth(charStr);

            // Calculate progress for this character (center of character)
            float progress = totalWidth > 0 ? (currentX - x + charWidth / 2f) / totalWidth : 0;
            int color = lerpColor(colorStart, colorEnd, progress);

            context.drawText(client.textRenderer, charStr, currentX, y, color, false);
            currentX += charWidth;
        }
    }

    /**
     * Draw text with a horizontal gradient color, with shadow.
     */
    public static void drawGradientTextWithShadow(DrawContext context, String text, int x, int y,
                                                  int colorStart, int colorEnd) {
        // Draw shadow
        int shadowColorStart = MiracleTheme.darken(colorStart, 0.5f);
        int shadowColorEnd = MiracleTheme.darken(colorEnd, 0.5f);
        drawGradientText(context, text, x + 1, y + 1, shadowColorStart, shadowColorEnd);

        // Draw main text
        drawGradientText(context, text, x, y, colorStart, colorEnd);
    }

    // ==================== THEME-AWARE HELPERS ====================

    /**
     * Draw a module card background with current theme styling.
     */
    public static void drawModuleCard(DrawContext context, int x, int y, int width, int height,
                                      boolean isEnabled, boolean isHovered) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Background
        int bgColor;
        if (isHovered) {
            bgColor = MiracleTheme.brighten(theme.surfaceSecondary, 0.1f);
        } else {
            bgColor = theme.surfaceSecondary;
        }

        fillRoundedRect(context, x, y, width, height, bgColor, MiracleTheme.RADIUS_MEDIUM);

        // Accent indicator if enabled
        if (isEnabled) {
            int indicatorColor = theme.accent500;
            context.fill(x, y + 4, x + 3, y + height - 4, indicatorColor);
        }
    }

    /**
     * Draw a category tab with current theme styling.
     */
    public static void drawCategoryTab(DrawContext context, int x, int y, int width, int height,
                                       boolean isSelected, boolean isHovered) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        if (isSelected) {
            // Selected: accent background
            int bgColor = MiracleTheme.withAlpha(theme.accent900, 0xAA);
            fillRoundedRect(context, x, y, width, height, bgColor, MiracleTheme.RADIUS_SMALL);
            // Accent bar
            context.fill(x, y + 2, x + 3, y + height - 2, theme.accent500);
        } else if (isHovered) {
            // Hovered: subtle highlight
            int bgColor = MiracleTheme.withAlpha(theme.surfaceSecondary, 0x80);
            fillRoundedRect(context, x, y, width, height, bgColor, MiracleTheme.RADIUS_SMALL);
        }
    }

    /**
     * Draw a scrollbar track and thumb with theme styling.
     */
    public static void drawScrollbar(DrawContext context, int x, int y, int width, int height,
                                     float scrollProgress, float viewportRatio) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Track
        int trackColor = MiracleTheme.withAlpha(theme.surfacePrimary, 0x40);
        fillRoundedRect(context, x, y, width, height, trackColor, width / 2);

        // Thumb
        int thumbHeight = Math.max(20, (int) (height * viewportRatio));
        int thumbY = y + (int) ((height - thumbHeight) * scrollProgress);
        int thumbColor = MiracleTheme.withAlpha(theme.accent500, 0x80);
        fillRoundedRect(context, x, thumbY, width, thumbHeight, thumbColor, width / 2);
    }

    /**
     * Draw a toggle switch with theme styling.
     */
    public static void drawToggleSwitch(DrawContext context, int x, int y, boolean isEnabled, float animProgress) {
        MiracleTheme.Theme theme = MiracleTheme.current();
        int width = MiracleTheme.TOGGLE_WIDTH;
        int height = MiracleTheme.TOGGLE_HEIGHT;

        // Track color (interpolate based on animation)
        int trackOff = theme.surfaceSecondary;
        int trackOn = theme.accent500;
        int trackColor = lerpColor(trackOff, trackOn, animProgress);
        fillRoundedRect(context, x, y, width, height, trackColor, height / 2);

        // Thumb
        int thumbSize = height - 4;
        int thumbX = x + 2 + (int) ((width - thumbSize - 4) * animProgress);
        int thumbColor = theme.textPrimary;
        fillRoundedRect(context, thumbX, y + 2, thumbSize, thumbSize, thumbColor, thumbSize / 2);
    }

    /**
     * Draw a slider track and thumb with theme styling.
     */
    public static void drawSlider(DrawContext context, int x, int y, int width, float value, boolean isHovered) {
        MiracleTheme.Theme theme = MiracleTheme.current();
        int height = MiracleTheme.SLIDER_HEIGHT;
        int thumbSize = MiracleTheme.SLIDER_THUMB;

        // Track background
        int trackY = y + (thumbSize - height) / 2;
        fillRoundedRect(context, x, trackY, width, height, theme.surfaceSecondary, height / 2);

        // Filled portion
        int fillWidth = (int) (width * value);
        if (fillWidth > 0) {
            fillRoundedRect(context, x, trackY, fillWidth, height, theme.accent500, height / 2);
        }

        // Thumb
        int thumbX = x + fillWidth - thumbSize / 2;
        thumbX = Math.max(x, Math.min(thumbX, x + width - thumbSize));
        int thumbColor = isHovered ? theme.accent400 : theme.textPrimary;

        // Thumb glow on hover
        if (isHovered) {
            int glowColor = MiracleTheme.withAlpha(theme.accent500, 0x40);
            drawGlow(context, thumbX, y, thumbSize, thumbSize, glowColor, 4);
        }

        fillRoundedRect(context, thumbX, y, thumbSize, thumbSize, thumbColor, thumbSize / 2);
    }
}
