package gg.miracle.gui.widget;

import gg.miracle.gui.UISounds;
import gg.miracle.gui.render.GradientRenderer;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * Color picker widget with HSV selection and hex input.
 */
public class MiracleColorPicker extends MiracleWidget {

    private String label = "";
    private int color = 0xFFFFFFFF; // ARGB
    private boolean pickerExpanded = false;
    private Consumer<Integer> onChange = null;

    // HSV values (0-1 range)
    private float hue = 0f;
    private float saturation = 1f;
    private float value = 1f;

    private boolean draggingHue = false;
    private boolean draggingSV = false;

    private static final int SWATCH_SIZE = 24;
    private static final int PICKER_WIDTH = 200;
    private static final int PICKER_HEIGHT = 180;
    private static final int HUE_BAR_WIDTH = 20;
    private static final int SV_PANEL_SIZE = 150;

    public MiracleColorPicker() {
        super();
        updateHSVFromColor();
    }

    // Builder methods
    public MiracleColorPicker label(String label) {
        this.label = label != null ? label : "";
        return this;
    }

    public MiracleColorPicker color(int color) {
        this.color = color;
        updateHSVFromColor();
        return this;
    }

    public MiracleColorPicker onChange(Consumer<Integer> onChange) {
        this.onChange = onChange;
        return this;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
        updateHSVFromColor();
        if (onChange != null) {
            onChange.accept(this.color);
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = theme();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float hover = getHoverProgress();

        // Draw label on left
        if (!label.isEmpty()) {
            int labelY = y + (height - textRenderer.fontHeight) / 2;
            int labelColor = MiracleTheme.lerpColor(theme.textSecondary, theme.textPrimary, hover);
            context.drawText(textRenderer, label, x, labelY, labelColor, false);
        }

        // Color swatch button
        int swatchX = x + width - SWATCH_SIZE;
        int swatchY = y + (height - SWATCH_SIZE) / 2;

        renderColorSwatch(context, theme, swatchX, swatchY, hover);

        // Hex value display
        String hexText = String.format("#%06X", color & 0xFFFFFF);
        int hexX = swatchX - textRenderer.getWidth(hexText) - 8;
        int hexY = y + (height - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, hexText, hexX, hexY, theme.textSecondary, false);

        // Expanded color picker
        if (pickerExpanded) {
            renderColorPicker(context, textRenderer, theme, swatchX - PICKER_WIDTH + SWATCH_SIZE,
                            y + height + 4, mouseX, mouseY);
        }
    }

    private void renderColorSwatch(DrawContext context, MiracleTheme.Theme theme, int swatchX, int swatchY, float hover) {
        // Border with hover effect
        int borderColor = pickerExpanded
            ? theme.accent500
            : MiracleTheme.lerpColor(theme.glassBorder, theme.accent500, hover);

        // Checkerboard background for transparency
        drawCheckerboard(context, swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE);

        // Color fill
        GradientRenderer.fillRoundedRect(context, swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE,
                                        color, MiracleTheme.RADIUS_MEDIUM);

        // Border
        GradientRenderer.drawBorder(context, swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, borderColor, 1);
    }

    private void renderColorPicker(DrawContext context, TextRenderer textRenderer, MiracleTheme.Theme theme,
                                   int pickerX, int pickerY, int mouseX, int mouseY) {
        // Background panel
        GradientRenderer.fillRoundedRect(context, pickerX, pickerY, PICKER_WIDTH, PICKER_HEIGHT,
                                        theme.glassBg, MiracleTheme.RADIUS_MEDIUM);
        GradientRenderer.drawBorder(context, pickerX, pickerY, PICKER_WIDTH, PICKER_HEIGHT, theme.accent500, 1);

        int contentX = pickerX + 10;
        int contentY = pickerY + 10;

        // Saturation/Value panel (left)
        int svX = contentX;
        int svY = contentY;
        renderSVPanel(context, svX, svY, SV_PANEL_SIZE, SV_PANEL_SIZE);

        // Hue bar (right of SV panel)
        int hueX = svX + SV_PANEL_SIZE + 10;
        int hueY = svY;
        renderHueBar(context, hueX, hueY, HUE_BAR_WIDTH, SV_PANEL_SIZE);

        // Preview swatch (bottom)
        int previewY = svY + SV_PANEL_SIZE + 10;
        int previewSize = 30;
        drawCheckerboard(context, contentX, previewY, previewSize, previewSize);
        GradientRenderer.fillRoundedRect(context, contentX, previewY, previewSize, previewSize,
                                        color, MiracleTheme.RADIUS_MEDIUM);

        // Hex input (bottom right)
        String hexText = String.format("#%06X", color & 0xFFFFFF);
        int hexInputX = contentX + previewSize + 10;
        int hexInputY = previewY + (previewSize - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, hexText, hexInputX, hexInputY, theme.textPrimary, false);
    }

    private void renderSVPanel(DrawContext context, int x, int y, int width, int height) {
        // Draw saturation/value grid
        // For performance, draw a simplified gradient version

        // Full saturation, varying value (left edge)
        int leftColor = hsvToRgb(hue, 1f, 1f);
        // Black (bottom right)
        int blackColor = 0xFF000000;
        // White (top left)
        int whiteColor = 0xFFFFFFFF;

        // Simple gradient approximation - horizontal saturation, vertical value
        for (int py = 0; py < height; py++) {
            float v = 1.0f - ((float) py / height); // Value from 1 (top) to 0 (bottom)

            for (int px = 0; px < width; px++) {
                float s = (float) px / width; // Saturation from 0 (left) to 1 (right)

                int pixelColor = hsvToRgb(hue, s, v);
                context.fill(x + px, y + py, x + px + 1, y + py + 1, pixelColor);
            }
        }

        // Draw selection cursor
        int cursorX = x + (int) (saturation * width);
        int cursorY = y + (int) ((1.0f - value) * height);
        int cursorSize = 8;

        // Outer ring (white/black for contrast)
        GradientRenderer.drawBorder(context, cursorX - cursorSize / 2, cursorY - cursorSize / 2, cursorSize, cursorSize, 0xFFFFFFFF, 1);
        GradientRenderer.drawBorder(context, cursorX - cursorSize / 2 + 1, cursorY - cursorSize / 2 + 1, cursorSize - 2, cursorSize - 2, 0xFF000000, 1);
    }

    private void renderHueBar(DrawContext context, int x, int y, int width, int height) {
        // Draw vertical hue gradient
        for (int py = 0; py < height; py++) {
            float h = (float) py / height;
            int hueColor = hsvToRgb(h, 1f, 1f) | 0xFF000000;

            context.fill(x, y + py, x + width, y + py + 1, hueColor);
        }

        // Draw selection indicator
        int indicatorY = y + (int) (hue * height);
        GradientRenderer.drawBorder(context, x - 1, indicatorY - 2, width + 2, 4, 0xFFFFFFFF, 1);
        GradientRenderer.drawBorder(context, x, indicatorY - 1, width, 2, 0xFF000000, 1);
    }

    private void drawCheckerboard(DrawContext context, int x, int y, int width, int height) {
        int checkSize = 4;
        for (int py = 0; py < height; py += checkSize) {
            for (int px = 0; px < width; px += checkSize) {
                boolean isWhite = ((px / checkSize) + (py / checkSize)) % 2 == 0;
                int color = isWhite ? 0xFFCCCCCC : 0xFF999999;
                context.fill(x + px, y + py, x + px + checkSize, y + py + checkSize, color);
            }
        }
    }

    @Override
    protected boolean onClicked() {
        pickerExpanded = !pickerExpanded;
        UISounds.click();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean down) {
        if (!visible || !enabled) return false;
        if (button != 0) return false;

        if (pickerExpanded && !down) {
            int swatchX = x + width - SWATCH_SIZE;
            int pickerX = swatchX - PICKER_WIDTH + SWATCH_SIZE;
            int pickerY = y + height + 4;

            int svX = pickerX + 10;
            int svY = pickerY + 10;
            int hueX = svX + SV_PANEL_SIZE + 10;
            int hueY = svY;

            // Check SV panel click
            if (mouseX >= svX && mouseX <= svX + SV_PANEL_SIZE &&
                mouseY >= svY && mouseY <= svY + SV_PANEL_SIZE) {
                draggingSV = true;
                updateSVFromMouse(mouseX, mouseY, svX, svY, SV_PANEL_SIZE);
                return true;
            }

            // Check hue bar click
            if (mouseX >= hueX && mouseX <= hueX + HUE_BAR_WIDTH &&
                mouseY >= hueY && mouseY <= hueY + SV_PANEL_SIZE) {
                draggingHue = true;
                updateHueFromMouse(mouseY, hueY, SV_PANEL_SIZE);
                return true;
            }

            // Click outside picker - close it
            if (mouseX < pickerX || mouseX > pickerX + PICKER_WIDTH ||
                mouseY < pickerY || mouseY > pickerY + PICKER_HEIGHT) {
                pickerExpanded = false;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button, down);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible || !enabled) return false;

        if (draggingSV) {
            int swatchX = x + width - SWATCH_SIZE;
            int pickerX = swatchX - PICKER_WIDTH + SWATCH_SIZE;
            int svX = pickerX + 10;
            int svY = (y + height + 4) + 10;
            updateSVFromMouse(mouseX, mouseY, svX, svY, SV_PANEL_SIZE);
            return true;
        }

        if (draggingHue) {
            int swatchX = x + width - SWATCH_SIZE;
            int pickerX = swatchX - PICKER_WIDTH + SWATCH_SIZE;
            int hueY = (y + height + 4) + 10;
            updateHueFromMouse(mouseY, hueY, SV_PANEL_SIZE);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSV = false;
        draggingHue = false;
        return false;
    }

    private void updateSVFromMouse(double mouseX, double mouseY, int svX, int svY, int size) {
        saturation = Math.max(0, Math.min(1, (float) (mouseX - svX) / size));
        value = Math.max(0, Math.min(1, 1.0f - (float) (mouseY - svY) / size));
        updateColorFromHSV();
    }

    private void updateHueFromMouse(double mouseY, int hueY, int height) {
        hue = Math.max(0, Math.min(1, (float) (mouseY - hueY) / height));
        updateColorFromHSV();
    }

    private void updateColorFromHSV() {
        int rgb = hsvToRgb(hue, saturation, value);
        int alpha = (color >>> 24) & 0xFF;
        color = (alpha << 24) | (rgb & 0xFFFFFF);

        if (onChange != null) {
            onChange.accept(color);
        }
    }

    private void updateHSVFromColor() {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        // Value
        value = max;

        // Saturation
        saturation = max == 0 ? 0 : delta / max;

        // Hue
        if (delta == 0) {
            hue = 0;
        } else if (max == rf) {
            hue = ((gf - bf) / delta) % 6;
        } else if (max == gf) {
            hue = ((bf - rf) / delta) + 2;
        } else {
            hue = ((rf - gf) / delta) + 4;
        }
        hue = (hue / 6f + 1f) % 1f;
    }

    private int hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }

        int ri = (int) (r * 255);
        int gi = (int) (g * 255);
        int bi = (int) (b * 255);

        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Include picker area when expanded
        if (pickerExpanded) {
            int swatchX = x + width - SWATCH_SIZE;
            int pickerX = swatchX - PICKER_WIDTH + SWATCH_SIZE;
            int pickerY = y + height + 4;

            if (mouseX >= pickerX && mouseX <= pickerX + PICKER_WIDTH &&
                mouseY >= pickerY && mouseY <= pickerY + PICKER_HEIGHT) {
                return true;
            }
        }

        return mouseX >= x && mouseX < x + width &&
               mouseY >= y && mouseY < y + height;
    }
}
