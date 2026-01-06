package gg.miracle.gui.widget;

import gg.miracle.gui.UISounds;
import gg.miracle.gui.render.GradientRenderer;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * Toggle switch widget with iOS-style sliding animation.
 */
public class MiracleToggle extends MiracleWidget {

    private String label = "";
    private boolean value = false;
    private Consumer<Boolean> onToggle = null;

    public MiracleToggle() {
        super();
    }

    // Builder methods
    public MiracleToggle label(String label) {
        this.label = label != null ? label : "";
        return this;
    }

    public MiracleToggle value(boolean value) {
        this.value = value;
        return this;
    }

    public MiracleToggle onToggle(Consumer<Boolean> onToggle) {
        this.onToggle = onToggle;
        return this;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = theme();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float hover = getHoverProgress();
        float press = getPressProgress();

        // Calculate positions - use theme constants for consistent sizing
        int switchWidth = MiracleTheme.TOGGLE_WIDTH;
        int switchHeight = MiracleTheme.TOGGLE_HEIGHT;
        int switchX = x + width - switchWidth;
        int switchY = y + (height - switchHeight) / 2;

        // Draw label on left
        if (!label.isEmpty()) {
            int labelY = y + (height - textRenderer.fontHeight) / 2;
            int labelColor = MiracleTheme.lerpColor(theme.textSecondary, theme.textPrimary, hover);
            context.drawText(textRenderer, label, x, labelY, labelColor, false);
        }

        // Press shadow for satisfying feedback
        if (press > 0) {
            int shadowColor = MiracleTheme.withAlpha(0xFF000000, (int)(0x20 * press));
            context.fill(switchX, switchY, switchX + switchWidth, switchY + 2, shadowColor);
        }

        // Use high-quality toggle renderer
        float animProgress = value ? 1.0f : 0.0f;
        GradientRenderer.drawToggleSwitch(context, switchX, switchY, value, animProgress);
    }

    @Override
    protected boolean onClicked() {
        value = !value;
        // Play toggle sound (with vanilla fallback)
        try {
            UISounds.toggle(value);
        } catch (Exception e) {
            // Fallback to vanilla click
            net.minecraft.client.MinecraftClient.getInstance().getSoundManager()
                .play(net.minecraft.client.sound.PositionedSoundInstance.master(
                    net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.5f));
        }
        if (onToggle != null) {
            onToggle.accept(value);
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Full width hitbox (label + switch)
        return mouseX >= x && mouseX < x + width &&
               mouseY >= y && mouseY < y + height;
    }
}
