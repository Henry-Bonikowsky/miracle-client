package gg.miracle.gui.widget;

import gg.miracle.gui.UISounds;
import gg.miracle.gui.render.GradientRenderer;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * Slider widget for numeric value selection.
 * Supports integer, float, and double values with customizable range.
 */
public class MiracleSlider extends MiracleWidget {

    private String label = "";
    private double value = 0.0;
    private double min = 0.0;
    private double max = 1.0;
    private double step = 0.0;  // 0 = continuous
    private boolean showValue = true;
    private String valueSuffix = "";
    private int decimalPlaces = 1;
    private Consumer<Double> onChange = null;
    private boolean dragging = false;

    public MiracleSlider() {
        super();
    }

    // Builder methods
    public MiracleSlider label(String label) {
        this.label = label != null ? label : "";
        return this;
    }

    public MiracleSlider value(double value) {
        this.value = clamp(value);
        return this;
    }

    public MiracleSlider range(double min, double max) {
        this.min = min;
        this.max = max;
        this.value = clamp(this.value);
        return this;
    }

    public MiracleSlider step(double step) {
        this.step = step;
        return this;
    }

    public MiracleSlider showValue(boolean show) {
        this.showValue = show;
        return this;
    }

    public MiracleSlider valueSuffix(String suffix) {
        this.valueSuffix = suffix != null ? suffix : "";
        return this;
    }

    public MiracleSlider decimalPlaces(int places) {
        this.decimalPlaces = Math.max(0, places);
        return this;
    }

    public MiracleSlider onChange(Consumer<Double> onChange) {
        this.onChange = onChange;
        return this;
    }

    public double getValue() {
        return value;
    }

    public int getValueInt() {
        return (int) Math.round(value);
    }

    public float getValueFloat() {
        return (float) value;
    }

    public void setValue(double value) {
        double newValue = clamp(value);
        if (newValue != this.value) {
            this.value = newValue;
            if (onChange != null) {
                onChange.accept(this.value);
            }
        }
    }

    private double clamp(double v) {
        v = Math.max(min, Math.min(max, v));
        if (step > 0) {
            v = Math.round((v - min) / step) * step + min;
            v = Math.max(min, Math.min(max, v));
        }
        return v;
    }

    private float getPercent() {
        if (max == min) return 0;
        return (float) ((value - min) / (max - min));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = theme();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float hover = getHoverProgress();
        float press = getPressProgress();

        // Calculate slider track area
        int sliderWidth = 80;
        int sliderX = x + width - sliderWidth;
        int sliderY = y + (height - MiracleTheme.SLIDER_THUMB) / 2;

        // Draw label on left
        if (!label.isEmpty()) {
            int labelY = y + (height - textRenderer.fontHeight) / 2;
            int labelColor = MiracleTheme.lerpColor(theme.textSecondary, theme.textPrimary, hover);
            context.drawText(textRenderer, label, x, labelY, labelColor, false);
        }

        // Press shadow when dragging or clicking
        if ((press > 0 || dragging) && hover > 0) {
            int shadowColor = MiracleTheme.withAlpha(0xFF000000, (int)(0x15 * Math.max(press, dragging ? 1f : 0f)));
            context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 1, shadowColor);
        }

        // Draw slider track and thumb
        GradientRenderer.drawSlider(context, sliderX, sliderY, sliderWidth, getPercent(), hovered || dragging);

        // Draw value text with press feedback
        if (showValue) {
            String valueText = formatValue() + valueSuffix;
            int valueX = sliderX - textRenderer.getWidth(valueText) - 8;
            int valueY = y + (height - textRenderer.fontHeight) / 2;
            int valueColor = MiracleTheme.lerpColor(theme.accent400, theme.accent300, dragging ? 0.3f : 0f);
            context.drawText(textRenderer, valueText, valueX, valueY, valueColor, false);
        }
    }

    private String formatValue() {
        if (decimalPlaces == 0 || step >= 1) {
            return String.valueOf((int) Math.round(value));
        }
        return String.format("%." + decimalPlaces + "f", value);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean down) {
        if (!visible || !enabled) return false;
        if (button != 0) return false;

        int sliderWidth = 80;
        int sliderX = x + width - sliderWidth;

        if (down) {
            // Check if click is on slider track
            if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth &&
                mouseY >= y && mouseY <= y + height) {
                dragging = true;
                updateValueFromMouse(mouseX, sliderX, sliderWidth);
                return true;
            }
        } else {
            if (dragging) {
                dragging = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible || !enabled) return false;
        if (button != 0) return false;

        if (dragging) {
            int sliderWidth = 80;
            int sliderX = x + width - sliderWidth;
            updateValueFromMouse(mouseX, sliderX, sliderWidth);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    private void updateValueFromMouse(double mouseX, int sliderX, int sliderWidth) {
        double percent = (mouseX - sliderX) / sliderWidth;
        percent = Math.max(0, Math.min(1, percent));
        double newValue = min + percent * (max - min);
        setValue(newValue);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Full width hitbox (label + value + slider)
        return mouseX >= x && mouseX < x + width &&
               mouseY >= y && mouseY < y + height;
    }
}
