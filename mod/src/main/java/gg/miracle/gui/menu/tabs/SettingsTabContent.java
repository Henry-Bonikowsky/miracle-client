package gg.miracle.gui.menu.tabs;

import gg.miracle.MiracleClient;
import gg.miracle.gui.menu.TabContent;
import gg.miracle.gui.theme.MiracleTheme;
import gg.miracle.gui.widget.*;import gg.miracle.gui.widget.MiracleDropdown.Option;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.option.OptionsScreen;

/**
 * Settings tab content showing theme picker and settings access.
 */
public class SettingsTabContent implements TabContent {

    private static final int THEME_BUTTON_SIZE = 32;
    private static final int THEME_BUTTON_SPACING = 8;
    private static final int THEMES_PER_ROW = 5;
    private static final int SECTION_SPACING = 20;

    private MiracleTheme.ThemeId hoveredTheme = null;
    private int hoveredButtonIndex = -1;  // For settings buttons

    // Layout cache
    private int themesX, themesY;
    private int mcSettingsY;

    // Test widgets
    private final MiracleToggle testToggle1 = new MiracleToggle()
        .label("Show FPS")
        .value(true)
        .onToggle(val -> System.out.println("FPS toggle: " + val));

    private final MiracleToggle testToggle2 = new MiracleToggle()
        .label("Fullbright")
        .value(false)
        .onToggle(val -> System.out.println("Fullbright toggle: " + val));

    private final MiracleSlider testSlider1 = new MiracleSlider()
        .label("Volume")
        .range(0, 100)
        .value(50)
        .step(1)
        .decimalPlaces(0)
        .valueSuffix("%")
        .onChange(val -> System.out.println("Volume: " + val.intValue() + "%"));

    private final MiracleSlider testSlider2 = new MiracleSlider()
        .label("Brightness")
        .range(0.0, 2.0)
        .value(1.0)
        .step(0.1)
        .decimalPlaces(1)
        .onChange(val -> System.out.println("Brightness: " + val));

    private final MiracleDropdown<String> testDropdown = new MiracleDropdown<String>()
        .label("Render Distance")
        .addOption("2 chunks", "2")
        .addOption("4 chunks", "4")
        .addOption("8 chunks", "8")
        .addOption("12 chunks", "12")
        .addOption("16 chunks", "16")
        .selected(2)
        .onChange(val -> System.out.println("Render distance: " + val));

    private final MiracleTextField testTextField = new MiracleTextField()
        .label("Username")
        .text("Player123")
        .placeholder("Enter username...")
        .maxLength(16)
        .onChange(val -> System.out.println("Username: " + val));

    private final MiracleCheckbox testCheckbox1 = new MiracleCheckbox()
        .label("Enable Particles")
        .description("Show particle effects")
        .checked(true)
        .onChecked(val -> System.out.println("Particles: " + val));

    private final MiracleCheckbox testCheckbox2 = new MiracleCheckbox()
        .label("Auto-Jump")
        .checked(false)
        .onChecked(val -> System.out.println("Auto-Jump: " + val));

    private final MiracleColorPicker testColorPicker = new MiracleColorPicker()
        .label("Crosshair Color")
        .color(0xFFFFFFFF)
        .onChange(val -> System.out.println("Color: #" + Integer.toHexString(val & 0xFFFFFF)));

    @Override
    public void render(DrawContext context, int x, int y, int width, int height,
                      int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = MiracleTheme.current();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int currentY = y;

        // === Theme Section ===
        String themeHeader = hoveredTheme != null ? "Theme: " + hoveredTheme.getDisplayName() : "Theme";
        context.drawText(textRenderer, themeHeader, x, currentY, theme.accent400, false);
        currentY += 14;

        themesX = x;
        themesY = currentY;
        currentY = renderThemeGrid(context, textRenderer, theme, x, currentY, width, mouseX, mouseY);
        currentY += SECTION_SPACING;

        // === Test Toggles Section ===
        context.drawText(textRenderer, "Options", x, currentY, theme.accent400, false);
        currentY += 14;

        // Position and render toggles
        testToggle1.setBounds(x, currentY, width, 24);
        testToggle1.render(context, mouseX, mouseY, delta);
        currentY += 28;

        testToggle2.setBounds(x, currentY, width, 24);
        testToggle2.render(context, mouseX, mouseY, delta);
        currentY += 28;

        testSlider1.setBounds(x, currentY, width, 24);
        testSlider1.render(context, mouseX, mouseY, delta);
        currentY += 28;

        testSlider2.setBounds(x, currentY, width, 24);
        testSlider2.render(context, mouseX, mouseY, delta);
        currentY += 28;

        testDropdown.setBounds(x, currentY, width, 24);
        testDropdown.render(context, mouseX, mouseY, delta);
        currentY += 28;

        testTextField.setBounds(x, currentY, width, 24);
        testTextField.render(context, mouseX, mouseY, delta);
        currentY += 28;

        testCheckbox1.setBounds(x, currentY, width, 40);
        testCheckbox1.render(context, mouseX, mouseY, delta);
        currentY += 44;

        testCheckbox2.setBounds(x, currentY, width, 24);
        testCheckbox2.render(context, mouseX, mouseY, delta);
        currentY += 28;

        testColorPicker.setBounds(x, currentY, width, 24);
        testColorPicker.render(context, mouseX, mouseY, delta);
        currentY += 28 + SECTION_SPACING;

        // === Minecraft Settings Button ===
        mcSettingsY = currentY;
        context.drawText(textRenderer, "Game Settings", x, currentY, theme.accent400, false);
        currentY += 14;

        renderSettingsButton(context, textRenderer, theme, x, currentY, width, mouseX, mouseY);
    }

    /**
     * Render the theme selection grid.
     */
    private int renderThemeGrid(DrawContext context, TextRenderer textRenderer,
                                MiracleTheme.Theme currentTheme, int x, int y, int width,
                                int mouseX, int mouseY) {
        hoveredTheme = null;
        MiracleTheme.ThemeId[] themes = MiracleTheme.getAllThemes();
        MiracleTheme.ThemeId currentId = MiracleTheme.getCurrentThemeId();

        int row = 0;
        int col = 0;

        for (MiracleTheme.ThemeId themeId : themes) {
            int buttonX = x + col * (THEME_BUTTON_SIZE + THEME_BUTTON_SPACING);
            int buttonY = y + row * (THEME_BUTTON_SIZE + THEME_BUTTON_SPACING);

            // Check hover
            boolean hovered = mouseX >= buttonX && mouseX < buttonX + THEME_BUTTON_SIZE &&
                             mouseY >= buttonY && mouseY < buttonY + THEME_BUTTON_SIZE;
            if (hovered) {
                hoveredTheme = themeId;
            }

            boolean selected = (themeId == currentId);
            MiracleTheme.Theme previewTheme = MiracleTheme.getThemeData(themeId);

            // Draw theme button
            renderThemeButton(context, buttonX, buttonY, previewTheme, hovered, selected, currentTheme);

            col++;
            if (col >= THEMES_PER_ROW) {
                col = 0;
                row++;
            }
        }

        int rows = (themes.length + THEMES_PER_ROW - 1) / THEMES_PER_ROW;
        int gridHeight = rows * (THEME_BUTTON_SIZE + THEME_BUTTON_SPACING);

        return y + gridHeight;
    }

    /**
     * Render a single theme button.
     */
    private void renderThemeButton(DrawContext context, int x, int y,
                                   MiracleTheme.Theme previewTheme,
                                   boolean hovered, boolean selected,
                                   MiracleTheme.Theme currentTheme) {
        // Border
        if (selected) {
            context.fill(x - 2, y - 2, x + THEME_BUTTON_SIZE + 2, y + THEME_BUTTON_SIZE + 2, currentTheme.accent500);
        } else if (hovered) {
            context.fill(x - 1, y - 1, x + THEME_BUTTON_SIZE + 1, y + THEME_BUTTON_SIZE + 1, currentTheme.glassBorder);
        }

        // Background gradient preview
        context.fillGradient(x, y, x + THEME_BUTTON_SIZE, y + THEME_BUTTON_SIZE,
            previewTheme.bgFrom, previewTheme.bgTo);

        // Accent color circle in center
        int circleRadius = 6;
        int circleX = x + THEME_BUTTON_SIZE / 2;
        int circleY = y + THEME_BUTTON_SIZE / 2;
        drawFilledCircle(context, circleX, circleY, circleRadius, previewTheme.accent500);
    }

    /**
     * Draw a filled circle using horizontal slices.
     */
    private void drawFilledCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(radius * radius - dy * dy);
            if (dx > 0) {
                context.fill(centerX - dx, centerY + dy, centerX + dx, centerY + dy + 1, color);
            }
        }
    }

    /**
     * Render the Minecraft settings button.
     */
    private void renderSettingsButton(DrawContext context, TextRenderer textRenderer,
                                      MiracleTheme.Theme theme, int x, int y, int width,
                                      int mouseX, int mouseY) {
        int buttonHeight = 28;
        boolean hovered = mouseX >= x && mouseX < x + width &&
                         mouseY >= y && mouseY < y + buttonHeight;

        hoveredButtonIndex = hovered ? 0 : -1;

        // Button background
        int bgColor = hovered ?
            MiracleTheme.withAlpha(theme.accent500, 0x40) :
            MiracleTheme.withAlpha(theme.surfaceSecondary, 0x60);
        context.fill(x, y, x + width, y + buttonHeight, bgColor);

        // Left accent bar when hovered
        if (hovered) {
            context.fill(x, y + 2, x + 3, y + buttonHeight - 2, theme.accent500);
        }

        // Button text
        int textColor = hovered ? theme.textPrimary : theme.textSecondary;
        int textY = y + (buttonHeight - 9) / 2;
        context.drawText(textRenderer, "Minecraft Options...", x + 8, textY, textColor, false);

        // Arrow icon
        String arrow = ">";
        int arrowWidth = textRenderer.getWidth(arrow);
        context.drawText(textRenderer, arrow, x + width - arrowWidth - 8, textY,
            hovered ? theme.accent400 : theme.textMuted, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean down) {
        if (button != 0) return false;

        // Check all widgets
        if (testToggle1.mouseClicked(mouseX, mouseY, button, down)) return true;
        if (testToggle2.mouseClicked(mouseX, mouseY, button, down)) return true;
        if (testSlider1.mouseClicked(mouseX, mouseY, button, down)) return true;
        if (testSlider2.mouseClicked(mouseX, mouseY, button, down)) return true;
        if (testDropdown.mouseClicked(mouseX, mouseY, button, down)) return true;
        if (testTextField.mouseClicked(mouseX, mouseY, button, down)) return true;
        if (testCheckbox1.mouseClicked(mouseX, mouseY, button, down)) return true;
        if (testCheckbox2.mouseClicked(mouseX, mouseY, button, down)) return true;
        if (testColorPicker.mouseClicked(mouseX, mouseY, button, down)) return true;

        // Only handle theme/settings clicks on release
        if (!down) {
            // Check theme buttons
            if (hoveredTheme != null) {
                // Use ConfigManager to set theme AND save to disk (for launcher sync)
                MiracleClient.getInstance().getConfigManager().setTheme(hoveredTheme.name());
                return true;
            }

            // Check settings button
            if (hoveredButtonIndex == 0) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.setScreen(new OptionsScreen(client.currentScreen, client.options));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0) return false;
        if (testSlider1.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        if (testSlider2.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        if (testColorPicker.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (testSlider1.mouseReleased(mouseX, mouseY, button)) return true;
        if (testSlider2.mouseReleased(mouseX, mouseY, button)) return true;
        if (testColorPicker.mouseReleased(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Allow focused text field to handle key presses
        return testTextField.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Allow focused text field to handle character input
        return testTextField.charTyped(chr, modifiers);
    }
}
