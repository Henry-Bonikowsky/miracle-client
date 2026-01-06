package gg.miracle.gui.widget;

import gg.miracle.gui.UISounds;
import gg.miracle.gui.render.GradientRenderer;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
//? if >=1.21.6
import net.minecraft.client.gl.RenderPipelines;
//? if <1.21.6
/*import net.minecraft.client.render.RenderLayer;*/

/**
 * Customizable button with multiple styles, optional icon, and built-in sounds.
 */
public class MiracleButton extends MiracleWidget {

    public enum Style {
        PRIMARY,    // Accent colored, glow on hover
        SECONDARY,  // Outline style, subtle hover
        GHOST,      // Text only, underline on hover
        DANGER      // Red accent for destructive actions
    }

    private String label = "";
    private Identifier icon = null;
    private Style style = Style.PRIMARY;
    private Runnable onClick = null;

    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = 6;
    private static final int TEXT_PADDING = 12;
    private static final int GHOST_PADDING = 4;  // Small padding around ghost text

    // Cached text bounds for ghost style hit detection
    private int textX, textY, textWidth, textHeight;

    public MiracleButton() {
        super();
    }

    // Builder methods
    public MiracleButton label(String label) {
        this.label = label != null ? label : "";
        return this;
    }

    public MiracleButton icon(Identifier icon) {
        this.icon = icon;
        return this;
    }

    public MiracleButton style(Style style) {
        this.style = style;
        return this;
    }

    public MiracleButton onClick(Runnable onClick) {
        this.onClick = onClick;
        return this;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = theme();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float hoverProgress = getHoverProgress();

        switch (style) {
            case PRIMARY -> renderPrimary(context, theme, textRenderer, hoverProgress);
            case SECONDARY -> renderSecondary(context, theme, textRenderer, hoverProgress);
            case GHOST -> renderGhost(context, theme, textRenderer, hoverProgress);
            case DANGER -> renderDanger(context, theme, textRenderer, hoverProgress);
        }
    }

    private void renderPrimary(DrawContext context, MiracleTheme.Theme theme, TextRenderer textRenderer, float hover) {
        float press = getPressProgress();

        // Background color interpolates with hover, darkens when pressed
        int bgColor = MiracleTheme.lerpColor(theme.accent500, theme.accent600, hover);
        if (press > 0) {
            bgColor = MiracleTheme.lerpColor(bgColor, theme.accent700, press * 0.5f);
        }

        // Glow on hover (reduced when pressed)
        if (hover > 0 && press < 1) {
            int glowAlpha = (int)(0x30 * hover * (1 - press * 0.5f));
            int glowColor = MiracleTheme.withAlpha(theme.accent400, glowAlpha);
            GradientRenderer.drawGlow(context, x, y, width, height, glowColor, 8);
        }

        // Inner shadow when pressed
        if (press > 0) {
            int shadowColor = MiracleTheme.withAlpha(0xFF000000, (int)(0x30 * press));
            context.fill(x, y, x + width, y + 2, shadowColor);
        }

        // Rounded background
        GradientRenderer.fillRoundedRect(context, x, y, width, height, bgColor, MiracleTheme.RADIUS_MEDIUM);

        // Content
        renderContent(context, textRenderer, theme.textPrimary);
    }

    private void renderSecondary(DrawContext context, MiracleTheme.Theme theme, TextRenderer textRenderer, float hover) {
        float press = getPressProgress();

        // Background on hover/press
        float bgIntensity = Math.max(hover, press);
        if (bgIntensity > 0) {
            int bgAlpha = (int)(0x20 * bgIntensity + 0x10 * press);
            GradientRenderer.fillRoundedRect(context, x, y, width, height,
                MiracleTheme.withAlpha(theme.accent500, bgAlpha), MiracleTheme.RADIUS_MEDIUM);
        }

        // Border - brighter when pressed
        int borderColor = MiracleTheme.lerpColor(theme.glassBorder, theme.accent500, Math.max(hover, press));
        GradientRenderer.drawBorder(context, x, y, width, height, borderColor, 1);

        // Content
        int textColor = MiracleTheme.lerpColor(theme.textSecondary, theme.textPrimary, Math.max(hover, press));
        renderContent(context, textRenderer, textColor);
    }

    private void renderGhost(DrawContext context, MiracleTheme.Theme theme, TextRenderer textRenderer, float hover) {
        float press = getPressProgress();

        // Calculate text bounds for hit detection
        boolean hasIcon = icon != null;
        boolean hasLabel = !label.isEmpty();

        int contentWidth = 0;
        if (hasIcon) contentWidth += ICON_SIZE;
        if (hasIcon && hasLabel) contentWidth += ICON_PADDING;
        if (hasLabel) contentWidth += textRenderer.getWidth(label);

        // Cache bounds for hit detection
        textWidth = contentWidth + GHOST_PADDING * 2;
        textHeight = Math.max(ICON_SIZE, textRenderer.fontHeight) + GHOST_PADDING * 2;
        textX = x + (width - textWidth) / 2;
        textY = y + (height - textHeight) / 2;

        // Subtle background when pressed
        if (press > 0) {
            int bgColor = MiracleTheme.withAlpha(theme.accent500, (int)(0x20 * press));
            context.fill(textX, textY, textX + textWidth, textY + textHeight, bgColor);
        }

        // Text color changes on hover, brighter when pressed
        float intensity = Math.max(hover, press);
        int textColor = MiracleTheme.lerpColor(theme.textSecondary, theme.accent400, intensity);
        renderContent(context, textRenderer, textColor);
    }

    private void renderDanger(DrawContext context, MiracleTheme.Theme theme, TextRenderer textRenderer, float hover) {
        float press = getPressProgress();

        // Red colors - darker when pressed
        int dangerBase = 0xFFDC2626;
        int dangerHover = 0xFFB91C1C;
        int dangerPressed = 0xFF991B1B;
        int bgColor = MiracleTheme.lerpColor(dangerBase, dangerHover, hover);
        if (press > 0) {
            bgColor = MiracleTheme.lerpColor(bgColor, dangerPressed, press * 0.5f);
        }

        // Glow on hover (reduced when pressed)
        if (hover > 0 && press < 1) {
            int glowAlpha = (int)(0x30 * hover * (1 - press * 0.5f));
            int glowColor = MiracleTheme.withAlpha(0xFFEF4444, glowAlpha);
            GradientRenderer.drawGlow(context, x, y, width, height, glowColor, 8);
        }

        // Inner shadow when pressed
        if (press > 0) {
            int shadowColor = MiracleTheme.withAlpha(0xFF000000, (int)(0x30 * press));
            context.fill(x, y, x + width, y + 2, shadowColor);
        }

        // Rounded background
        GradientRenderer.fillRoundedRect(context, x, y, width, height, bgColor, MiracleTheme.RADIUS_MEDIUM);

        // Content
        renderContent(context, textRenderer, 0xFFFFFFFF);
    }

    private void renderContent(DrawContext context, TextRenderer textRenderer, int textColor) {
        boolean hasIcon = icon != null;
        boolean hasLabel = !label.isEmpty();

        int contentWidth = 0;
        if (hasIcon) contentWidth += ICON_SIZE;
        if (hasIcon && hasLabel) contentWidth += ICON_PADDING;
        if (hasLabel) contentWidth += textRenderer.getWidth(label);

        int contentX = x + (width - contentWidth) / 2;
        int contentY = y + (height - (hasIcon ? ICON_SIZE : textRenderer.fontHeight)) / 2;

        // Draw icon
        if (hasIcon) {
            int iconY = y + (height - ICON_SIZE) / 2;
            //? if >=1.21.6 {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, contentX, iconY, 0, 0,
                ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, textColor);
            //?} else {
            /*context.drawTexture(RenderLayer::getGuiTextured, icon, contentX, iconY, 0, 0,
                ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, textColor);
            *///?}
            contentX += ICON_SIZE + ICON_PADDING;
        }

        // Draw label
        if (hasLabel) {
            int textY = y + (height - textRenderer.fontHeight) / 2;
            context.drawText(textRenderer, label, contentX, textY, textColor, false);
        }
    }

    @Override
    protected boolean onClicked() {
        UISounds.click();
        if (onClick != null) {
            onClick.run();
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Ghost style uses tight text bounds
        if (style == Style.GHOST && textWidth > 0) {
            return mouseX >= textX && mouseX < textX + textWidth &&
                   mouseY >= textY && mouseY < textY + textHeight;
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    // Getters
    public String getLabel() { return label; }
    public Identifier getIcon() { return icon; }
    public Style getStyle() { return style; }
}
