package gg.miracle.gui;

import gg.miracle.MiracleClient;
import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import gg.miracle.gui.animation.AnimationManager;
import gg.miracle.gui.render.CinematicBackground;
import gg.miracle.gui.render.GradientRenderer;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
/*? if MC_1_21_11 {*/
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
/*?}*/

import java.util.List;

/**
 * Premium themed main menu for Miracle Client.
 * Features animated cinematic background, glassmorphism panels,
 * and full theme support matching the launcher aesthetic.
 */
public class MiracleMainMenu extends Screen {
    private final Screen parent;
    private Category selectedCategory = Category.COMBAT;
    private Module expandedModule = null;
    private int menuX, menuY;
    private int menuWidth = 500;
    private int menuHeight = 400;
    private int scrollOffset = 0;

    // Animation manager
    private final AnimationManager animations = new AnimationManager();

    // Cinematic background
    private final CinematicBackground cinematicBackground = new CinematicBackground();

    // Tooltip state
    private String tooltipText = null;
    private int tooltipX, tooltipY;

    // Theme selector state
    private boolean showingThemeSelector = false;

    public MiracleMainMenu() {
        this(null);
    }

    public MiracleMainMenu(Screen parent) {
        super(Text.literal("Miracle Client"));
        this.parent = parent;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    protected void init() {
        menuX = (width - menuWidth) / 2;
        menuY = (height - menuHeight) / 2;
        scrollOffset = 0;
        animations.clear();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        tooltipText = null;
        animations.tick();

        MiracleTheme.Theme theme = MiracleTheme.current();

        // Render cinematic background (full screen replacement)
        cinematicBackground.render(context, width, height);

        // Main glass panel
        GradientRenderer.fillGlassPanel(context, menuX, menuY, menuWidth, menuHeight);

        // Title bar with glass effect
        int titleBarHeight = 36;
        context.fill(menuX, menuY, menuX + menuWidth, menuY + titleBarHeight,
                MiracleTheme.withAlpha(theme.surfacePrimary, 0x60));

        // Title with gradient text
        String title = MiracleClient.MOD_NAME;
        GradientRenderer.drawGradientTextWithShadow(context, title, menuX + 16, menuY + 12,
                theme.accent400, theme.accent600);

        // Version badge
        String version = "v" + MiracleClient.MOD_VERSION;
        int versionX = menuX + 16 + textRenderer.getWidth(title) + 8;
        context.drawText(textRenderer, version, versionX, menuY + 14, theme.textMuted, false);

        // Theme selector button (top right)
        int themeButtonX = menuX + menuWidth - 80;
        int themeButtonY = menuY + 8;
        boolean themeButtonHovered = mouseX >= themeButtonX && mouseX <= themeButtonX + 70 &&
                mouseY >= themeButtonY && mouseY <= themeButtonY + 20;
        renderThemeButton(context, themeButtonX, themeButtonY, themeButtonHovered);

        // Sidebar with glass effect
        int sidebarWidth = 100;
        int sidebarX = menuX + 8;
        int sidebarY = menuY + titleBarHeight + 8;
        int sidebarHeight = menuHeight - titleBarHeight - 16;

        GradientRenderer.fillGlassPanelRounded(context, sidebarX, sidebarY, sidebarWidth, sidebarHeight,
                MiracleTheme.RADIUS_MEDIUM);

        // Category tabs
        int tabY = sidebarY + 8;
        for (Category category : Category.values()) {
            boolean selected = category == selectedCategory;
            boolean hovered = mouseX >= sidebarX + 4 && mouseX <= sidebarX + sidebarWidth - 4 &&
                    mouseY >= tabY && mouseY <= tabY + 26;

            String hoverKey = AnimationManager.categoryKey(category.getName(), "hover");
            float hoverProgress = animations.getHoverAnimation(hoverKey, hovered && !selected, MiracleTheme.ANIM_QUICK);

            // Tab background
            GradientRenderer.drawCategoryTab(context, sidebarX + 4, tabY, sidebarWidth - 8, 26,
                    selected, hoverProgress > 0.01f);

            // Tab icon (placeholder - could add icons later)
            // context.drawText(textRenderer, getCategoryIcon(category), sidebarX + 10, tabY + 9, iconColor, false);

            // Tab text
            int textColor = selected ? theme.accent400 :
                    (hovered ? theme.textPrimary : theme.textSecondary);
            context.drawText(textRenderer, category.getName(), sidebarX + 14, tabY + 9, textColor, false);

            tabY += 30;
        }

        // Module list area
        int listX = sidebarX + sidebarWidth + 8;
        int listY = sidebarY;
        int listWidth = menuWidth - sidebarWidth - 24;
        int listHeight = sidebarHeight;

        // Module panel glass background
        GradientRenderer.fillGlassPanelRounded(context, listX, listY, listWidth, listHeight,
                MiracleTheme.RADIUS_MEDIUM);

        // Category header in module panel
        context.drawText(textRenderer, selectedCategory.getName() + " Modules",
                listX + 12, listY + 8, theme.textPrimary, false);

        // Separator line
        context.fill(listX + 8, listY + 24, listX + listWidth - 8, listY + 25,
                MiracleTheme.withAlpha(theme.glassBorder, 0x40));

        // Draw modules (scrollable area starts after header)
        int moduleListY = listY + 32;
        int moduleListHeight = listHeight - 40;

        // Enable scissor/clipping would be ideal here, but we'll manage with bounds checking
        List<Module> modules = MiracleClient.getInstance().getModuleManager().getByCategory(selectedCategory);
        int moduleY = moduleListY + scrollOffset;

        for (Module module : modules) {
            int moduleHeight = getModuleHeight(module);

            if (moduleY + moduleHeight < moduleListY || moduleY > moduleListY + moduleListHeight) {
                moduleY += moduleHeight + 6;
                continue;
            }

            boolean isExpanded = module == expandedModule;
            boolean hovered = mouseX >= listX + 8 && mouseX <= listX + listWidth - 8 &&
                    mouseY >= moduleY && mouseY <= moduleY + 36;

            String hoverKey = AnimationManager.moduleKey(module.getName(), "hover");
            String expandKey = AnimationManager.moduleKey(module.getName(), "expand");
            String toggleKey = AnimationManager.moduleKey(module.getName(), "toggle");

            float hoverProgress = animations.getHoverAnimation(hoverKey, hovered, MiracleTheme.ANIM_QUICK);
            float expandProgress = animations.getExpandAnimation(expandKey, isExpanded, MiracleTheme.ANIM_SLOW);
            float toggleProgress = animations.getToggleAnimation(toggleKey, module.isEnabled(), MiracleTheme.ANIM_NORMAL);

            // Module card
            GradientRenderer.drawModuleCard(context, listX + 8, moduleY, listWidth - 16, moduleHeight,
                    module.isEnabled(), hoverProgress > 0.01f);

            // Module name
            int nameColor = module.isEnabled() ?
                    MiracleTheme.lerpColor(theme.textPrimary, theme.accent400, toggleProgress * 0.5f) :
                    theme.textPrimary;
            context.drawText(textRenderer, module.getName(), listX + 20, moduleY + 8, nameColor, false);

            // Module description
            String desc = module.getDescription();
            if (desc != null && !desc.isEmpty()) {
                String shortDesc = desc.length() > 35 ? desc.substring(0, 32) + "..." : desc;
                context.drawText(textRenderer, shortDesc, listX + 20, moduleY + 22, theme.textMuted, false);

                if (hovered && desc.length() > 35) {
                    tooltipText = desc;
                    tooltipX = mouseX;
                    tooltipY = mouseY;
                }
            }

            // Expand/collapse indicator
            if (!module.getSettings().isEmpty()) {
                String indicator = isExpanded ? "-" : "+";
                int indicatorColor = isExpanded ? theme.accent500 : theme.textMuted;
                int indicatorX = listX + listWidth - 80;
                context.drawText(textRenderer, "[" + indicator + "]", indicatorX, moduleY + 14, indicatorColor, false);
            }

            // Toggle switch
            int toggleX = listX + listWidth - 50;
            int toggleY = moduleY + 10;
            GradientRenderer.drawToggleSwitch(context, toggleX, toggleY, module.isEnabled(), toggleProgress);

            // Draw settings if expanded
            if (isExpanded && !module.getSettings().isEmpty()) {
                int settingY = moduleY + 40;
                for (Setting<?> setting : module.getSettings()) {
                    renderSetting(context, module, setting, listX + 20, settingY, listWidth - 40, mouseX, mouseY);
                    settingY += 24;
                }
            }

            moduleY += moduleHeight + 6;
        }

        // Scrollbar
        int totalHeight = calculateTotalHeight(modules);
        if (totalHeight > moduleListHeight) {
            float viewportRatio = (float) moduleListHeight / totalHeight;
            int maxScroll = totalHeight - moduleListHeight;
            float scrollProgress = maxScroll > 0 ? (float) -scrollOffset / maxScroll : 0;

            GradientRenderer.drawScrollbar(context, listX + listWidth - 6, moduleListY,
                    4, moduleListHeight, scrollProgress, viewportRatio);
        }

        // Footer hint
        String hint = "Right Shift: toggle menu | Click: expand settings";
        int hintWidth = textRenderer.getWidth(hint);
        context.drawText(textRenderer, hint, menuX + (menuWidth - hintWidth) / 2,
                menuY + menuHeight - 14, theme.textMuted, false);

        // Theme selector overlay
        if (showingThemeSelector) {
            renderThemeSelector(context, mouseX, mouseY);
        }

        // Render tooltip last
        if (tooltipText != null && !showingThemeSelector) {
            renderTooltip(context, tooltipText, tooltipX, tooltipY);
        }
    }

    private void renderThemeButton(DrawContext context, int x, int y, boolean hovered) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Button background
        int bgColor = hovered ? MiracleTheme.withAlpha(theme.accent500, 0x40) :
                MiracleTheme.withAlpha(theme.surfaceSecondary, 0x80);
        GradientRenderer.fillRoundedRect(context, x, y, 70, 20, bgColor, 4);

        // Current theme color swatch
        context.fill(x + 4, y + 4, x + 16, y + 16, theme.accent500);

        // Text
        int textColor = hovered ? theme.textPrimary : theme.textSecondary;
        context.drawText(textRenderer, "Theme", x + 22, y + 6, textColor, false);
    }

    private void renderThemeSelector(DrawContext context, int mouseX, int mouseY) {
        MiracleTheme.Theme theme = MiracleTheme.current();
        MiracleTheme.ThemeId currentTheme = MiracleTheme.getCurrentThemeId();

        // Overlay background
        context.fill(0, 0, width, height, 0x80000000);

        // Selector panel
        int panelWidth = 300;
        int panelHeight = 280;
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        GradientRenderer.fillGlassPanel(context, panelX, panelY, panelWidth, panelHeight);

        // Title
        context.drawText(textRenderer, "Select Theme", panelX + 12, panelY + 12, theme.textPrimary, false);

        // Close button
        int closeX = panelX + panelWidth - 24;
        int closeY = panelY + 8;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 16 && mouseY >= closeY && mouseY <= closeY + 16;
        context.drawText(textRenderer, "X", closeX + 4, closeY + 4, closeHovered ? theme.accent400 : theme.textMuted, false);

        // Theme grid (2 columns x 5 rows)
        int gridX = panelX + 16;
        int gridY = panelY + 36;
        int swatchSize = 50;
        int swatchGap = 12;

        MiracleTheme.ThemeId[] allThemes = MiracleTheme.getAllThemes();
        for (int i = 0; i < allThemes.length; i++) {
            MiracleTheme.ThemeId themeId = allThemes[i];
            MiracleTheme.Theme themeData = MiracleTheme.getThemeData(themeId);

            int col = i % 2;
            int row = i / 2;
            int swatchX = gridX + col * (swatchSize + swatchGap + 80);
            int swatchY = gridY + row * (swatchSize + swatchGap - 10);

            boolean isSelected = themeId == currentTheme;
            boolean isHovered = mouseX >= swatchX && mouseX <= swatchX + swatchSize &&
                    mouseY >= swatchY && mouseY <= swatchY + swatchSize - 10;

            // Swatch background (gradient preview)
            GradientRenderer.fillVerticalGradient(context, swatchX, swatchY, swatchSize, swatchSize - 10,
                    themeData.bgFrom, themeData.bgTo);

            // Accent color circle
            int circleX = swatchX + swatchSize / 2;
            int circleY = swatchY + (swatchSize - 10) / 2;
            context.fill(circleX - 8, circleY - 8, circleX + 8, circleY + 8, themeData.accent500);

            // Selection/hover border
            if (isSelected) {
                GradientRenderer.drawBorder(context, swatchX - 2, swatchY - 2, swatchSize + 4, swatchSize - 6,
                        theme.accent400, 2);
            } else if (isHovered) {
                GradientRenderer.drawBorder(context, swatchX - 1, swatchY - 1, swatchSize + 2, swatchSize - 8,
                        theme.textMuted, 1);
            }

            // Theme name
            context.drawText(textRenderer, themeId.getDisplayName(), swatchX + swatchSize + 8, swatchY + 6,
                    isSelected ? theme.accent400 : theme.textPrimary, false);

            // Theme description
            context.drawText(textRenderer, themeId.getDescription(), swatchX + swatchSize + 8, swatchY + 18,
                    theme.textMuted, false);
        }
    }

    private void renderSetting(DrawContext context, Module module, Setting<?> setting,
                               int x, int y, int width, int mouseX, int mouseY) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Setting name
        context.drawText(textRenderer, setting.getName(), x, y + 4, theme.textSecondary, false);

        Object value = setting.get();
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20;

        String hoverKey = AnimationManager.settingKey(module.getName(), setting.getName(), "hover");
        float hoverProgress = animations.getHoverAnimation(hoverKey, hovered, MiracleTheme.ANIM_QUICK);

        // Boolean toggle
        if (value instanceof Boolean) {
            boolean enabled = (Boolean) value;
            String toggleKey = AnimationManager.settingKey(module.getName(), setting.getName(), "toggle");
            float toggleProgress = animations.getToggleAnimation(toggleKey, enabled, MiracleTheme.ANIM_NORMAL);

            int toggleX = x + width - 36;
            GradientRenderer.drawToggleSwitch(context, toggleX, y + 2, enabled, toggleProgress);
        }
        // Slider
        else if (setting.hasRange()) {
            int sliderX = x + width - 100;
            int sliderWidth = 80;

            double min, max, current;
            if (value instanceof Integer) {
                min = ((Integer) setting.getMin()).doubleValue();
                max = ((Integer) setting.getMax()).doubleValue();
                current = ((Integer) value).doubleValue();
            } else if (value instanceof Float) {
                min = ((Float) setting.getMin()).doubleValue();
                max = ((Float) setting.getMax()).doubleValue();
                current = ((Float) value).doubleValue();
            } else {
                min = (Double) setting.getMin();
                max = (Double) setting.getMax();
                current = (Double) value;
            }

            float percent = (float) ((current - min) / (max - min));
            GradientRenderer.drawSlider(context, sliderX, y, sliderWidth, percent, hoverProgress > 0.5f);

            // Value display
            String valueStr = value instanceof Integer ? value.toString() : String.format("%.1f", current);
            context.drawText(textRenderer, valueStr, sliderX + sliderWidth + 6, y + 4, theme.accent400, false);
        }
        // String/other
        else {
            String valueStr = value.toString();
            if (valueStr.length() > 15) valueStr = valueStr.substring(0, 12) + "...";
            int textWidth = textRenderer.getWidth(valueStr);
            context.drawText(textRenderer, valueStr, x + width - textWidth, y + 4, theme.textMuted, false);
        }

        // Tooltip on hover
        if (hovered && setting.getDescription() != null && !setting.getDescription().isEmpty()) {
            tooltipText = setting.getDescription();
            tooltipX = mouseX;
            tooltipY = mouseY;
        }
    }

    private void renderTooltip(DrawContext context, String text, int x, int y) {
        MiracleTheme.Theme theme = MiracleTheme.current();

        int padding = 8;
        int textWidth = textRenderer.getWidth(text);
        int boxWidth = textWidth + padding * 2;
        int boxHeight = 12 + padding * 2;

        int boxX = x + 12;
        int boxY = y - boxHeight - 4;
        if (boxX + boxWidth > width) boxX = width - boxWidth - 4;
        if (boxY < 0) boxY = y + 18;

        // Background
        GradientRenderer.fillGlassPanel(context, boxX, boxY, boxWidth, boxHeight);

        // Text
        context.drawText(textRenderer, text, boxX + padding, boxY + padding + 1, theme.textPrimary, false);
    }

    private int getModuleHeight(Module module) {
        if (module == expandedModule && !module.getSettings().isEmpty()) {
            return 36 + module.getSettings().size() * 24 + 8;
        }
        return 36;
    }

    private int calculateTotalHeight(List<Module> modules) {
        int total = 0;
        for (Module module : modules) {
            total += getModuleHeight(module) + 6;
        }
        return total;
    }

    /*? if MC_1_21_11 {*/
    @Override
    public boolean mouseClicked(Click click, boolean down) {
        if (handleMouseClick(click.x(), click.y(), click.button(), down)) return true;
        return super.mouseClicked(click, down);
    }
    /*?} else {*/
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleMouseClick(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }*/
    /*?}*/

    private boolean handleMouseClick(double mouseX, double mouseY) {
        return handleMouseClick(mouseX, mouseY, 0, false);
    }

    private boolean handleMouseClick(double mouseX, double mouseY, int button) {
        return handleMouseClick(mouseX, mouseY, button, false);
    }

    private boolean handleMouseClick(double mouseX, double mouseY, int button, boolean down) {
        // Only handle on release (when down=false)
        if (down) return false;
        MiracleTheme.Theme theme = MiracleTheme.current();

        // Handle theme selector
        if (showingThemeSelector) {
            int panelWidth = 300;
            int panelHeight = 280;
            int panelX = (width - panelWidth) / 2;
            int panelY = (height - panelHeight) / 2;

            // Close button
            int closeX = panelX + panelWidth - 24;
            int closeY = panelY + 8;
            if (mouseX >= closeX && mouseX <= closeX + 16 && mouseY >= closeY && mouseY <= closeY + 16) {
                showingThemeSelector = false;
                return true;
            }

            // Theme selection
            int gridX = panelX + 16;
            int gridY = panelY + 36;
            int swatchSize = 50;
            int swatchGap = 12;

            MiracleTheme.ThemeId[] allThemes = MiracleTheme.getAllThemes();
            for (int i = 0; i < allThemes.length; i++) {
                int col = i % 2;
                int row = i / 2;
                int swatchX = gridX + col * (swatchSize + swatchGap + 80);
                int swatchY = gridY + row * (swatchSize + swatchGap - 10);

                if (mouseX >= swatchX && mouseX <= swatchX + swatchSize &&
                        mouseY >= swatchY && mouseY <= swatchY + swatchSize - 10) {
                    MiracleTheme.setTheme(allThemes[i]);
                    // Save theme preference
                    MiracleClient.getInstance().getConfigManager().setTheme(allThemes[i].name());
                    return true;
                }
            }

            // Click outside to close
            if (mouseX < panelX || mouseX > panelX + panelWidth ||
                    mouseY < panelY || mouseY > panelY + panelHeight) {
                showingThemeSelector = false;
                return true;
            }

            return true;
        }

        // Theme button click
        int themeButtonX = menuX + menuWidth - 80;
        int themeButtonY = menuY + 8;
        if (mouseX >= themeButtonX && mouseX <= themeButtonX + 70 &&
                mouseY >= themeButtonY && mouseY <= themeButtonY + 20) {
            showingThemeSelector = true;
            return true;
        }

        // Handle category clicks
        int sidebarX = menuX + 8;
        int sidebarWidth = 100;
        int tabY = menuY + 36 + 8 + 8;
        for (Category category : Category.values()) {
            if (mouseX >= sidebarX + 4 && mouseX <= sidebarX + sidebarWidth - 4 &&
                    mouseY >= tabY && mouseY <= tabY + 26) {
                selectedCategory = category;
                expandedModule = null;
                scrollOffset = 0;
                return true;
            }
            tabY += 30;
        }

        // Handle module interactions
        int listX = sidebarX + sidebarWidth + 8;
        int listWidth = menuWidth - sidebarWidth - 24;
        int moduleListY = menuY + 36 + 8 + 32;

        List<Module> modules = MiracleClient.getInstance().getModuleManager().getByCategory(selectedCategory);
        int moduleY = moduleListY + scrollOffset;

        for (Module module : modules) {
            int moduleHeight = getModuleHeight(module);
            int toggleX = listX + listWidth - 50;
            int toggleY = moduleY + 10;

            // Toggle switch click
            if (mouseX >= toggleX && mouseX <= toggleX + MiracleTheme.TOGGLE_WIDTH &&
                    mouseY >= toggleY && mouseY <= toggleY + MiracleTheme.TOGGLE_HEIGHT) {
                module.toggle();
                return true;
            }

            // Expand/collapse indicator click
            if (!module.getSettings().isEmpty()) {
                int indicatorX = listX + listWidth - 80;
                if (mouseX >= indicatorX && mouseX <= indicatorX + 30 &&
                        mouseY >= moduleY && mouseY <= moduleY + 36) {
                    expandedModule = (expandedModule == module) ? null : module;
                    return true;
                }
            }

            // Module name/description area click
            if (mouseX >= listX + 8 && mouseX <= listX + listWidth - 100 &&
                    mouseY >= moduleY && mouseY <= moduleY + 36) {
                if (!module.getSettings().isEmpty()) {
                    expandedModule = (expandedModule == module) ? null : module;
                } else {
                    module.toggle();
                }
                return true;
            }

            // Handle settings clicks if expanded
            if (module == expandedModule && !module.getSettings().isEmpty()) {
                int settingY = moduleY + 40;
                for (Setting<?> setting : module.getSettings()) {
                    if (handleSettingClick(setting, listX + 20, settingY, listWidth - 40, mouseX, mouseY)) {
                        return true;
                    }
                    settingY += 24;
                }
            }

            moduleY += moduleHeight + 6;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean handleSettingClick(Setting<?> setting, int x, int y, int width, double mouseX, double mouseY) {
        if (mouseY < y || mouseY > y + 20) return false;

        Object value = setting.get();

        if (value instanceof Boolean) {
            int toggleX = x + width - 36;
            if (mouseX >= toggleX && mouseX <= toggleX + MiracleTheme.TOGGLE_WIDTH) {
                ((Setting<Boolean>) setting).set(!(Boolean) value);
                return true;
            }
        } else if (setting.hasRange()) {
            int sliderX = x + width - 100;
            int sliderWidth = 80;

            if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth) {
                double percent = (mouseX - sliderX) / sliderWidth;
                percent = Math.max(0, Math.min(1, percent));

                if (value instanceof Integer) {
                    int min = (Integer) setting.getMin();
                    int max = (Integer) setting.getMax();
                    ((Setting<Integer>) setting).set(min + (int) (percent * (max - min)));
                } else if (value instanceof Float) {
                    float min = (Float) setting.getMin();
                    float max = (Float) setting.getMax();
                    ((Setting<Float>) setting).set(min + (float) (percent * (max - min)));
                } else if (value instanceof Double) {
                    double min = (Double) setting.getMin();
                    double max = (Double) setting.getMax();
                    ((Setting<Double>) setting).set(min + percent * (max - min));
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (showingThemeSelector) return true;

        List<Module> modules = MiracleClient.getInstance().getModuleManager().getByCategory(selectedCategory);
        int moduleListHeight = menuHeight - 36 - 8 - 32 - 16 - 8;
        int totalHeight = calculateTotalHeight(modules);

        if (totalHeight > moduleListHeight) {
            scrollOffset += (int) (verticalAmount * 24);
            int maxScroll = -(totalHeight - moduleListHeight + 10);
            scrollOffset = Math.min(0, Math.max(maxScroll, scrollOffset));
        }

        return true;
    }

    /*? if MC_1_21_11 {*/
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (handleMouseDrag(click.x(), click.y())) return true;
        return super.mouseDragged(click, deltaX, deltaY);
    }
    /*?} else {*/
    /*@Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (handleMouseDrag(mouseX, mouseY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }*/
    /*?}*/

    private boolean handleMouseDrag(double mouseX, double mouseY) {
        if (showingThemeSelector) return false;

        if (expandedModule != null && !expandedModule.getSettings().isEmpty()) {
            int sidebarWidth = 100;
            int listX = menuX + 8 + sidebarWidth + 8;
            int listWidth = menuWidth - sidebarWidth - 24;

            List<Module> modules = MiracleClient.getInstance().getModuleManager().getByCategory(selectedCategory);
            int moduleY = menuY + 36 + 8 + 32 + scrollOffset;

            for (Module module : modules) {
                if (module == expandedModule) {
                    int settingY = moduleY + 40;
                    for (Setting<?> setting : module.getSettings()) {
                        if (handleSettingClick(setting, listX + 20, settingY, listWidth - 40, mouseX, mouseY)) {
                            return true;
                        }
                        settingY += 24;
                    }
                    break;
                }
                moduleY += getModuleHeight(module) + 6;
            }
        }
        return false;
    }

    /*? if MC_1_21_11 {*/
    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (keyCode == 256) { // Escape
            if (showingThemeSelector) {
                showingThemeSelector = false;
                return true;
            }
            close();
            return true;
        }
        if (keyCode == 344) { // Right Shift
            close();
            return true;
        }
        return super.keyPressed(input);
    }
    /*?} else {*/
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            if (showingThemeSelector) {
                showingThemeSelector = false;
                return true;
            }
            close();
            return true;
        }
        if (keyCode == 344) { // Right Shift
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }*/
    /*?}*/

    @Override
    public boolean shouldPause() {
        return false;
    }
}
