package gg.miracle.gui.menu;

import gg.miracle.MiracleClient;
import gg.miracle.gui.render.GradientRenderer;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Content ribbon panel for the Miracle Client menu.
 * Displays the animated title and tab-specific content.
 */
public class Ribbon {

    public static final int WIDTH = 250;
    private static final int TITLE_HEIGHT = 60;
    private static final int FOOTER_HEIGHT = 24;
    private static final int PADDING = 12;

    private int x, y, height;
    private Tab currentTab = Tab.HOME;
    private TabContent content;

    // Animation state for title
    private final long startTime = System.currentTimeMillis();

    /**
     * Set the position and size of the ribbon.
     */
    public void setBounds(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
    }

    /**
     * Set the current tab and its content.
     */
    public void setTab(Tab tab, TabContent content) {
        if (this.content != null) {
            this.content.onDeactivate();
        }
        this.currentTab = tab;
        this.content = content;
        if (this.content != null) {
            this.content.init();
        }
    }

    /**
     * Render the ribbon.
     */
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = MiracleTheme.current();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        // Glass background
        context.fill(x, y, x + WIDTH, y + height, theme.glassBg);
        // Right border
        context.fill(x + WIDTH - 1, y, x + WIDTH, y + height, theme.glassBorder);

        // Render animated title
        renderTitle(context, theme, textRenderer);

        // Separator after title
        context.fill(x + PADDING, y + TITLE_HEIGHT, x + WIDTH - PADDING, y + TITLE_HEIGHT + 1,
            MiracleTheme.withAlpha(theme.glassBorder, 0x60));

        // Render tab content
        if (content != null) {
            int contentY = y + TITLE_HEIGHT + PADDING;
            int contentHeight = height - TITLE_HEIGHT - FOOTER_HEIGHT - PADDING * 2;
            content.render(context, x + PADDING, contentY, WIDTH - PADDING * 2, contentHeight,
                mouseX, mouseY, delta);
        }

        // Render footer
        renderFooter(context, theme, textRenderer);
    }

    /**
     * Render the animated "MIRACLE" title.
     */
    private void renderTitle(DrawContext context, MiracleTheme.Theme theme, TextRenderer textRenderer) {
        String title = "MIRACLE";
        String subtitle = "CLIENT";

        // Calculate shimmer animation offset
        long elapsed = System.currentTimeMillis() - startTime;
        float shimmerProgress = (elapsed % 3000) / 3000f;  // 3 second shimmer cycle

        // Draw main title with gradient
        int titleWidth = textRenderer.getWidth(title);
        int titleX = x + (WIDTH - titleWidth) / 2;
        int titleY = y + 16;

        // Animate gradient colors for shimmer effect
        int color1 = interpolateColor(theme.accent400, theme.accent600, shimmerProgress);
        int color2 = interpolateColor(theme.accent600, theme.accent400, shimmerProgress);
        GradientRenderer.drawGradientText(context, title, titleX, titleY, color1, color2);

        // Draw subtitle
        int subtitleWidth = textRenderer.getWidth(subtitle);
        int subtitleX = x + (WIDTH - subtitleWidth) / 2;
        int subtitleY = titleY + 14;
        context.drawText(textRenderer, subtitle, subtitleX, subtitleY, theme.textSecondary, false);
    }

    /**
     * Interpolate between two colors.
     */
    private int interpolateColor(int color1, int color2, float progress) {
        int a1 = (color1 >> 24) & 0xFF, a2 = (color2 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF, r2 = (color2 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF, g2 = (color2 >> 8) & 0xFF;
        int b1 = color1 & 0xFF, b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * progress);
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Render the footer with version info.
     */
    private void renderFooter(DrawContext context, MiracleTheme.Theme theme, TextRenderer textRenderer) {
        int footerY = y + height - FOOTER_HEIGHT;

        // Separator
        context.fill(x + PADDING, footerY, x + WIDTH - PADDING, footerY + 1,
            MiracleTheme.withAlpha(theme.glassBorder, 0x40));

        // Version info
        String version = "v" + MiracleClient.MOD_VERSION;
        String mcVersion = "MC " + getMinecraftVersion();

        int versionY = footerY + (FOOTER_HEIGHT - 9) / 2 + 2;
        context.drawText(textRenderer, version, x + PADDING, versionY, theme.textMuted, false);

        int mcVersionWidth = textRenderer.getWidth(mcVersion);
        context.drawText(textRenderer, mcVersion, x + WIDTH - PADDING - mcVersionWidth, versionY,
            theme.textMuted, false);
    }

    /**
     * Get the Minecraft version string.
     */
    private String getMinecraftVersion() {
        /*? if MC_1_21_11 {*/
        return "1.21.11";
        /*?} elif MC_1_21_8 {*/
        /*return "1.21.8";*/
        /*?} elif MC_1_21_5 {*/
        /*return "1.21.5";*/
        /*?} else {*/
        /*return "1.21.4";*/
        /*?}*/
    }

    /**
     * Handle mouse click (legacy - triggers on release).
     */
    public boolean handleClick(double mouseX, double mouseY, int button) {
        return handleClick(mouseX, mouseY, button, false);
    }

    /**
     * Handle mouse click with press/release state.
     */
    public boolean handleClick(double mouseX, double mouseY, int button, boolean down) {
        if (content == null) return false;
        if (mouseX < x || mouseX >= x + WIDTH) {
            return false;
        }

        int contentY = y + TITLE_HEIGHT + PADDING;
        int contentHeight = height - TITLE_HEIGHT - FOOTER_HEIGHT - PADDING * 2;

        if (mouseY >= contentY && mouseY < contentY + contentHeight) {
            return content.mouseClicked(mouseX, mouseY, button, down);
        }
        return false;
    }

    /**
     * Handle mouse scroll.
     */
    public boolean handleScroll(double mouseX, double mouseY, double amount) {
        if (content == null) return false;
        return content.mouseScrolled(mouseX, mouseY, amount);
    }

    /**
     * Handle key press.
     */
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (content == null) return false;
        return content.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Handle mouse drag.
     */
    public boolean handleDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (content == null) return false;
        return content.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    /**
     * Handle mouse release.
     */
    public boolean handleRelease(double mouseX, double mouseY, int button) {
        if (content == null) return false;
        return content.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Tick the content.
     */
    public void tick() {
        if (content != null) {
            content.tick();
        }
    }

    /**
     * Get current tab.
     */
    public Tab getCurrentTab() {
        return currentTab;
    }
}
