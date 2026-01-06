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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dropdown/ComboBox widget with expandable menu for option selection.
 */
public class MiracleDropdown<T> extends MiracleWidget {

    private String label = "";
    private List<Option<T>> options = new ArrayList<>();
    private int selectedIndex = 0;
    private boolean expanded = false;
    private Consumer<T> onChange = null;
    private int hoveredOptionIndex = -1;

    private static final int OPTION_HEIGHT = 24;
    private static final int MAX_VISIBLE_OPTIONS = 6;
    private static final int ARROW_SIZE = 6;
    private static final int ICON_SIZE = 12;
    private static final int ICON_PADDING = 4;

    public static class Option<T> {
        public final String text;
        public final T value;
        public final Identifier icon;

        public Option(String text, T value) {
            this(text, value, null);
        }

        public Option(String text, T value, Identifier icon) {
            this.text = text;
            this.value = value;
            this.icon = icon;
        }
    }

    public MiracleDropdown() {
        super();
    }

    // Builder methods
    public MiracleDropdown<T> label(String label) {
        this.label = label != null ? label : "";
        return this;
    }

    public MiracleDropdown<T> addOption(String text, T value) {
        options.add(new Option<>(text, value));
        return this;
    }

    public MiracleDropdown<T> addOption(String text, T value, Identifier icon) {
        options.add(new Option<>(text, value, icon));
        return this;
    }

    public MiracleDropdown<T> options(List<Option<T>> options) {
        this.options = new ArrayList<>(options);
        return this;
    }

    public MiracleDropdown<T> selected(int index) {
        if (index >= 0 && index < options.size()) {
            this.selectedIndex = index;
        }
        return this;
    }

    public MiracleDropdown<T> onChange(Consumer<T> onChange) {
        this.onChange = onChange;
        return this;
    }

    public T getSelectedValue() {
        return selectedIndex >= 0 && selectedIndex < options.size()
            ? options.get(selectedIndex).value
            : null;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size() && index != selectedIndex) {
            selectedIndex = index;
            if (onChange != null && options.get(selectedIndex).value != null) {
                onChange.accept(options.get(selectedIndex).value);
            }
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = theme();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float hover = getHoverProgress();
        float press = getPressProgress();

        // Calculate dropdown button area
        int dropdownWidth = 160;
        int dropdownX = x + width - dropdownWidth;
        int dropdownY = y;

        // Draw label on left
        if (!label.isEmpty()) {
            int labelY = y + (height - textRenderer.fontHeight) / 2;
            int labelColor = MiracleTheme.lerpColor(theme.textSecondary, theme.textPrimary, hover);
            context.drawText(textRenderer, label, x, labelY, labelColor, false);
        }

        // Render main dropdown button
        renderDropdownButton(context, textRenderer, theme, dropdownX, dropdownY, dropdownWidth, hover, press);

        // Render expanded menu
        if (expanded && !options.isEmpty()) {
            renderExpandedMenu(context, textRenderer, theme, dropdownX, dropdownY + height + 4, dropdownWidth, mouseX, mouseY);
        }
    }

    private void renderDropdownButton(DrawContext context, TextRenderer textRenderer, MiracleTheme.Theme theme,
                                       int btnX, int btnY, int btnWidth, float hover, float press) {
        // Background with hover effect
        int bgColor = MiracleTheme.lerpColor(theme.glassBg, theme.accent500, hover * 0.15f);
        if (press > 0) {
            bgColor = MiracleTheme.lerpColor(bgColor, theme.accent600, press * 0.3f);
        }
        if (expanded) {
            bgColor = MiracleTheme.lerpColor(bgColor, theme.accent500, 0.2f);
        }

        GradientRenderer.fillRoundedRect(context, btnX, btnY, btnWidth, height, bgColor, MiracleTheme.RADIUS_MEDIUM);

        // Border
        int borderColor = expanded
            ? theme.accent500
            : MiracleTheme.lerpColor(theme.glassBorder, theme.accent500, hover);
        GradientRenderer.drawBorder(context, btnX, btnY, btnWidth, height, borderColor, 1);

        // Selected option text + icon
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            Option<T> selected = options.get(selectedIndex);
            int contentX = btnX + 8;
            int contentY = btnY + (height - textRenderer.fontHeight) / 2;

            // Icon
            if (selected.icon != null) {
                int iconY = btnY + (height - ICON_SIZE) / 2;
                //? if >=1.21.6 {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, selected.icon, contentX, iconY, 0, 0,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, theme.textPrimary);
                //?} else {
                /*context.drawTexture(RenderLayer::getGuiTextured, selected.icon, contentX, iconY, 0, 0,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, theme.textPrimary);
                *///?}
                contentX += ICON_SIZE + ICON_PADDING;
            }

            // Text
            String displayText = selected.text;
            int maxTextWidth = btnWidth - 24 - (selected.icon != null ? ICON_SIZE + ICON_PADDING : 0);
            if (textRenderer.getWidth(displayText) > maxTextWidth) {
                displayText = textRenderer.trimToWidth(displayText, maxTextWidth - textRenderer.getWidth("...")) + "...";
            }
            context.drawText(textRenderer, displayText, contentX, contentY, theme.textPrimary, false);
        }

        // Arrow icon
        int arrowX = btnX + btnWidth - ARROW_SIZE - 8;
        int arrowY = btnY + height / 2;
        drawArrow(context, arrowX, arrowY, expanded, theme.textSecondary);
    }

    private void renderExpandedMenu(DrawContext context, TextRenderer textRenderer, MiracleTheme.Theme theme,
                                     int menuX, int menuY, int menuWidth, int mouseX, int mouseY) {
        int visibleOptions = Math.min(options.size(), MAX_VISIBLE_OPTIONS);
        int menuHeight = visibleOptions * OPTION_HEIGHT + 8;

        // Background glass panel
        GradientRenderer.fillRoundedRect(context, menuX, menuY, menuWidth, menuHeight, theme.glassBg, MiracleTheme.RADIUS_MEDIUM);
        GradientRenderer.drawBorder(context, menuX, menuY, menuWidth, menuHeight, theme.accent500, 1);

        // Render options
        hoveredOptionIndex = -1;
        for (int i = 0; i < visibleOptions; i++) {
            int optionY = menuY + 4 + i * OPTION_HEIGHT;
            boolean optionHovered = mouseX >= menuX && mouseX <= menuX + menuWidth &&
                                   mouseY >= optionY && mouseY <= optionY + OPTION_HEIGHT;

            if (optionHovered) {
                hoveredOptionIndex = i;
            }

            renderOption(context, textRenderer, theme, menuX + 4, optionY, menuWidth - 8, i, optionHovered);
        }

        // Scrollbar hint if more options exist
        if (options.size() > MAX_VISIBLE_OPTIONS) {
            int scrollbarX = menuX + menuWidth - 6;
            int scrollbarY = menuY + 4;
            int scrollbarHeight = menuHeight - 8;
            context.fill(scrollbarX, scrollbarY, scrollbarX + 2, scrollbarY + scrollbarHeight,
                        MiracleTheme.withAlpha(theme.textMuted, 0x40));
        }
    }

    private void renderOption(DrawContext context, TextRenderer textRenderer, MiracleTheme.Theme theme,
                              int optX, int optY, int optWidth, int index, boolean hovered) {
        Option<T> option = options.get(index);
        boolean isSelected = index == selectedIndex;

        // Hover background
        if (hovered) {
            int hoverBg = MiracleTheme.withAlpha(theme.accent500, 0x30);
            context.fill(optX, optY, optX + optWidth, optY + OPTION_HEIGHT, hoverBg);
        }

        // Selected indicator
        if (isSelected) {
            int selectedBg = MiracleTheme.withAlpha(theme.accent500, 0x20);
            context.fill(optX, optY, optX + optWidth, optY + OPTION_HEIGHT, selectedBg);
            context.fill(optX, optY, optX + 2, optY + OPTION_HEIGHT, theme.accent500);
        }

        // Icon
        int contentX = optX + 6;
        int contentY = optY + (OPTION_HEIGHT - textRenderer.fontHeight) / 2;

        if (option.icon != null) {
            int iconY = optY + (OPTION_HEIGHT - ICON_SIZE) / 2;
            //? if >=1.21.6 {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, option.icon, contentX, iconY, 0, 0,
                ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, theme.textPrimary);
            //?} else {
            /*context.drawTexture(RenderLayer::getGuiTextured, option.icon, contentX, iconY, 0, 0,
                ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, theme.textPrimary);
            *///?}
            contentX += ICON_SIZE + ICON_PADDING;
        }

        // Text
        int textColor = isSelected ? theme.accent400 : (hovered ? theme.textPrimary : theme.textSecondary);
        context.drawText(textRenderer, option.text, contentX, contentY, textColor, false);
    }

    private void drawArrow(DrawContext context, int x, int y, boolean pointUp, int color) {
        if (pointUp) {
            // Up arrow
            context.fill(x + ARROW_SIZE / 2, y - ARROW_SIZE / 2, x + ARROW_SIZE / 2 + 1, y - ARROW_SIZE / 2 + 1, color);
            for (int i = 1; i <= ARROW_SIZE / 2; i++) {
                context.fill(x + ARROW_SIZE / 2 - i, y - ARROW_SIZE / 2 + i,
                           x + ARROW_SIZE / 2 + i + 1, y - ARROW_SIZE / 2 + i + 1, color);
            }
        } else {
            // Down arrow
            context.fill(x + ARROW_SIZE / 2, y + ARROW_SIZE / 2, x + ARROW_SIZE / 2 + 1, y + ARROW_SIZE / 2 + 1, color);
            for (int i = 1; i <= ARROW_SIZE / 2; i++) {
                context.fill(x + ARROW_SIZE / 2 - i, y + ARROW_SIZE / 2 - i,
                           x + ARROW_SIZE / 2 + i + 1, y + ARROW_SIZE / 2 - i + 1, color);
            }
        }
    }

    @Override
    protected boolean onClicked() {
        if (options.isEmpty()) return false;
        expanded = !expanded;
        UISounds.click();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean down) {
        if (!visible || !enabled) return false;
        if (button != 0) return false;

        // Check if clicking inside expanded menu
        if (expanded && !options.isEmpty() && !down) {
            int dropdownWidth = 160;
            int dropdownX = x + width - dropdownWidth;
            int menuY = y + height + 4;
            int visibleOptions = Math.min(options.size(), MAX_VISIBLE_OPTIONS);
            int menuHeight = visibleOptions * OPTION_HEIGHT + 8;

            if (mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth &&
                mouseY >= menuY && mouseY <= menuY + menuHeight) {

                // Click on option
                if (hoveredOptionIndex >= 0 && hoveredOptionIndex < options.size()) {
                    setSelectedIndex(hoveredOptionIndex);
                    expanded = false;
                    UISounds.click();
                    return true;
                }
            } else {
                // Click outside menu - close it
                expanded = false;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button, down);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Full width hitbox (label + dropdown)
        if (mouseX >= x && mouseX < x + width &&
            mouseY >= y && mouseY < y + height) {
            return true;
        }

        // Extended hitbox when expanded (includes menu)
        if (expanded && !options.isEmpty()) {
            int dropdownWidth = 160;
            int dropdownX = x + width - dropdownWidth;
            int menuY = y + height + 4;
            int visibleOptions = Math.min(options.size(), MAX_VISIBLE_OPTIONS);
            int menuHeight = visibleOptions * OPTION_HEIGHT + 8;

            return mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth &&
                   mouseY >= menuY && mouseY <= menuY + menuHeight;
        }

        return false;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
