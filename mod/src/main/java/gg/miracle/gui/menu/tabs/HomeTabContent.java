package gg.miracle.gui.menu.tabs;

import gg.miracle.gui.menu.TabContent;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Home tab content showing quick access features.
 * - Quick Join: Last 2 servers played
 * - Friends Online: Count of online friends
 * - Mod Updates: Notification of available updates
 * - Changelog: Recent changes snippet
 */
public class HomeTabContent implements TabContent {

    private static final int SECTION_SPACING = 20;
    private static final int BUTTON_HEIGHT = 28;
    private static final int BUTTON_SPACING = 6;

    // Quick Join hover state
    private int hoveredServerIndex = -1;

    // Mock data (will be replaced with real launcher bridge data)
    private final String[] recentServers = { "hypixel.net", "mc.example.com" };
    private final int friendsOnline = 3;
    private final boolean hasModUpdates = true;

    @Override
    public void init() {
        // Load recent servers from launcher bridge
        // Load friends count from Supabase
        // Check for mod updates
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height,
                      int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = MiracleTheme.current();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int currentY = y;

        // === Quick Join Section ===
        currentY = renderQuickJoinSection(context, textRenderer, theme, x, currentY, width, mouseX, mouseY);
        currentY += SECTION_SPACING;

        // === Friends Online Section ===
        currentY = renderFriendsSection(context, textRenderer, theme, x, currentY, width);
        currentY += SECTION_SPACING;

        // === Mod Updates Section ===
        currentY = renderUpdatesSection(context, textRenderer, theme, x, currentY, width);
        currentY += SECTION_SPACING;

        // === Changelog Section ===
        renderChangelogSection(context, textRenderer, theme, x, currentY, width);
    }

    /**
     * Render the Quick Join section with last 2 servers.
     */
    private int renderQuickJoinSection(DrawContext context, TextRenderer textRenderer,
                                        MiracleTheme.Theme theme, int x, int y, int width,
                                        int mouseX, int mouseY) {
        // Section header
        context.drawText(textRenderer, "Quick Join", x, y, theme.accent400, false);
        y += 14;

        // Update hover state
        hoveredServerIndex = -1;
        for (int i = 0; i < recentServers.length; i++) {
            int buttonY = y + i * (BUTTON_HEIGHT + BUTTON_SPACING);
            if (mouseX >= x && mouseX < x + width &&
                mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT) {
                hoveredServerIndex = i;
                break;
            }
        }

        // Render server buttons
        for (int i = 0; i < recentServers.length; i++) {
            int buttonY = y + i * (BUTTON_HEIGHT + BUTTON_SPACING);
            boolean hovered = (i == hoveredServerIndex);
            renderServerButton(context, textRenderer, theme, x, buttonY, width, recentServers[i], hovered);
        }

        return y + recentServers.length * (BUTTON_HEIGHT + BUTTON_SPACING);
    }

    /**
     * Render a single server button.
     */
    private void renderServerButton(DrawContext context, TextRenderer textRenderer,
                                    MiracleTheme.Theme theme, int x, int y, int width,
                                    String serverAddress, boolean hovered) {
        // Button background
        int bgColor = hovered ?
            MiracleTheme.withAlpha(theme.accent500, 0x40) :
            MiracleTheme.withAlpha(theme.surfaceSecondary, 0x60);
        context.fill(x, y, x + width, y + BUTTON_HEIGHT, bgColor);

        // Left accent bar when hovered
        if (hovered) {
            context.fill(x, y + 2, x + 3, y + BUTTON_HEIGHT - 2, theme.accent500);
        }

        // Server address text
        int textColor = hovered ? theme.textPrimary : theme.textSecondary;
        int textY = y + (BUTTON_HEIGHT - 9) / 2;
        context.drawText(textRenderer, serverAddress, x + 8, textY, textColor, false);

        // Play icon on right
        String playIcon = ">";
        int iconWidth = textRenderer.getWidth(playIcon);
        context.drawText(textRenderer, playIcon, x + width - iconWidth - 8, textY,
            hovered ? theme.accent400 : theme.textMuted, false);
    }

    /**
     * Render the Friends Online section.
     */
    private int renderFriendsSection(DrawContext context, TextRenderer textRenderer,
                                      MiracleTheme.Theme theme, int x, int y, int width) {
        // Section header
        context.drawText(textRenderer, "Friends", x, y, theme.accent400, false);
        y += 14;

        // Friends count
        String friendsText = friendsOnline + " online";
        int countColor = friendsOnline > 0 ? 0xFF00E676 : theme.textMuted;  // Green if friends online
        context.drawText(textRenderer, friendsText, x, y, countColor, false);

        return y + 12;
    }

    /**
     * Render the Mod Updates section.
     */
    private int renderUpdatesSection(DrawContext context, TextRenderer textRenderer,
                                      MiracleTheme.Theme theme, int x, int y, int width) {
        // Section header
        context.drawText(textRenderer, "Updates", x, y, theme.accent400, false);
        y += 14;

        if (hasModUpdates) {
            // Update available notification
            context.drawText(textRenderer, "New update available!", x, y, 0xFFFFA500, false);  // Orange
            y += 12;
            context.drawText(textRenderer, "Click to download", x, y, theme.textMuted, false);
        } else {
            context.drawText(textRenderer, "You're up to date", x, y, theme.textSecondary, false);
        }

        return y + 12;
    }

    /**
     * Render the Changelog section.
     */
    private void renderChangelogSection(DrawContext context, TextRenderer textRenderer,
                                        MiracleTheme.Theme theme, int x, int y, int width) {
        // Section header
        context.drawText(textRenderer, "What's New", x, y, theme.accent400, false);
        y += 14;

        // Changelog entries (placeholder)
        String[] changelog = {
            "- New tab-based menu design",
            "- Video clip backgrounds",
            "- Theme improvements"
        };

        for (String entry : changelog) {
            context.drawText(textRenderer, entry, x, y, theme.textSecondary, false);
            y += 11;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean down) {
        if (button != 0) return false;
        if (down) return false;  // Only handle release

        // Check if a server was clicked
        if (hoveredServerIndex >= 0 && hoveredServerIndex < recentServers.length) {
            String server = recentServers[hoveredServerIndex];
            gg.miracle.MiracleClient.LOGGER.info("[HomeTab] Quick join: {}", server);
            return true;
        }

        return false;
    }
}
