package gg.miracle.gui.widget;

import gg.miracle.gui.UISounds;
import gg.miracle.gui.render.GradientRenderer;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * Checkbox widget with label and optional description.
 * Different from Toggle - uses checkmark visual instead of switch.
 */
public class MiracleCheckbox extends MiracleWidget {

    private String label = "";
    private String description = "";
    private boolean checked = false;
    private Consumer<Boolean> onChecked = null;

    private static final int BOX_SIZE = 18;
    private static final int BOX_PADDING = 8;
    private static final int CHECK_THICKNESS = 2;

    public MiracleCheckbox() {
        super();
    }

    // Builder methods
    public MiracleCheckbox label(String label) {
        this.label = label != null ? label : "";
        return this;
    }

    public MiracleCheckbox description(String description) {
        this.description = description != null ? description : "";
        return this;
    }

    public MiracleCheckbox checked(boolean checked) {
        this.checked = checked;
        return this;
    }

    public MiracleCheckbox onChecked(Consumer<Boolean> onChecked) {
        this.onChecked = onChecked;
        return this;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = theme();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float hover = getHoverProgress();
        float press = getPressProgress();

        // Calculate checkbox box position
        int boxX = x;
        int boxY = y + (height - BOX_SIZE) / 2;

        // Render checkbox box
        renderCheckbox(context, theme, boxX, boxY, hover, press);

        // Render label and description
        if (!label.isEmpty() || !description.isEmpty()) {
            int textX = x + BOX_SIZE + BOX_PADDING;
            int textY = y + (height - getTextHeight(textRenderer)) / 2;

            // Label
            if (!label.isEmpty()) {
                int labelColor = MiracleTheme.lerpColor(theme.textSecondary, theme.textPrimary, hover);
                context.drawText(textRenderer, label, textX, textY, labelColor, false);
            }

            // Description (below label)
            if (!description.isEmpty()) {
                int descY = textY + textRenderer.fontHeight + 2;
                context.drawText(textRenderer, description, textX, descY, theme.textMuted, false);
            }
        }
    }

    private void renderCheckbox(DrawContext context, MiracleTheme.Theme theme, int boxX, int boxY, float hover, float press) {
        // Background - changes based on checked state
        int bgColor;
        if (checked) {
            bgColor = MiracleTheme.lerpColor(theme.accent500, theme.accent600, press * 0.3f);
        } else {
            bgColor = MiracleTheme.lerpColor(theme.glassBg, theme.accent500, hover * 0.1f);
        }

        GradientRenderer.fillRoundedRect(context, boxX, boxY, BOX_SIZE, BOX_SIZE, bgColor, 4);

        // Border
        int borderColor = checked
            ? theme.accent500
            : MiracleTheme.lerpColor(theme.glassBorder, theme.accent500, hover);
        GradientRenderer.drawBorder(context, boxX, boxY, BOX_SIZE, BOX_SIZE, borderColor, 1);

        // Glow when hovered and checked
        if (checked && hover > 0) {
            int glowAlpha = (int)(0x20 * hover);
            int glowColor = MiracleTheme.withAlpha(theme.accent400, glowAlpha);
            GradientRenderer.drawGlow(context, boxX, boxY, BOX_SIZE, BOX_SIZE, glowColor, 6);
        }

        // Checkmark when checked
        if (checked) {
            drawCheckmark(context, boxX + BOX_SIZE / 2, boxY + BOX_SIZE / 2, BOX_SIZE * 0.6f, theme.textPrimary);
        }
    }

    private void drawCheckmark(DrawContext context, int centerX, int centerY, float size, int color) {
        // Draw checkmark as two lines forming a check
        float halfSize = size / 2;

        // Short vertical line (left part of check)
        int x1 = (int)(centerX - halfSize * 0.4f);
        int y1 = (int)(centerY - halfSize * 0.1f);
        int x2 = (int)(centerX - halfSize * 0.15f);
        int y2 = (int)(centerY + halfSize * 0.3f);

        drawThickLine(context, x1, y1, x2, y2, CHECK_THICKNESS, color);

        // Long diagonal line (right part of check)
        int x3 = x2;
        int y3 = y2;
        int x4 = (int)(centerX + halfSize * 0.5f);
        int y4 = (int)(centerY - halfSize * 0.5f);

        drawThickLine(context, x3, y3, x4, y4, CHECK_THICKNESS, color);
    }

    private void drawThickLine(DrawContext context, int x1, int y1, int x2, int y2, int thickness, int color) {
        // Bresenham's line algorithm with thickness
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            // Draw thick point
            for (int ox = -thickness / 2; ox <= thickness / 2; ox++) {
                for (int oy = -thickness / 2; oy <= thickness / 2; oy++) {
                    context.fill(x1 + ox, y1 + oy, x1 + ox + 1, y1 + oy + 1, color);
                }
            }

            if (x1 == x2 && y1 == y2) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private int getTextHeight(TextRenderer textRenderer) {
        int height = 0;
        if (!label.isEmpty()) {
            height += textRenderer.fontHeight;
        }
        if (!description.isEmpty()) {
            if (!label.isEmpty()) height += 2; // Spacing
            height += textRenderer.fontHeight;
        }
        return height;
    }

    @Override
    protected boolean onClicked() {
        checked = !checked;
        UISounds.click();
        if (onChecked != null) {
            onChecked.accept(checked);
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width &&
               mouseY >= y && mouseY < y + height;
    }
}
