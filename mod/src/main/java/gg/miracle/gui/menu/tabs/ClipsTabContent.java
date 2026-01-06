package gg.miracle.gui.menu.tabs;

import gg.miracle.gui.menu.TabContent;
import gg.miracle.gui.render.GradientRenderer;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Clips tab content showing recorded clips.
 * Displays a grid of clips with thumbnails, duration, and metadata.
 */
public class ClipsTabContent implements TabContent {

    private static final int GRID_COLS = 2;
    private static final int CARD_WIDTH = 180;
    private static final int CARD_HEIGHT = 120;
    private static final int CARD_SPACING = 10;
    private static final int THUMBNAIL_HEIGHT = 80;

    private List<ClipInfo> clips = new ArrayList<>();
    private int hoveredClipIndex = -1;
    private int scrollOffset = 0;

    @Override
    public void init() {
        loadClips();
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height,
                      int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = MiracleTheme.current();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        if (clips.isEmpty()) {
            renderEmptyState(context, textRenderer, theme, x, y, width, height);
            return;
        }

        // Header
        context.drawText(textRenderer, "Your Clips", x, y, theme.accent400, false);
        context.drawText(textRenderer, clips.size() + " clip" + (clips.size() == 1 ? "" : "s"),
                        x + width - textRenderer.getWidth(clips.size() + " clips"), y,
                        theme.textSecondary, false);
        y += 20;

        // Render clips grid
        int currentY = y - scrollOffset;
        hoveredClipIndex = -1;

        for (int i = 0; i < clips.size(); i++) {
            int row = i / GRID_COLS;
            int col = i % GRID_COLS;
            int cardX = x + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = currentY + row * (CARD_HEIGHT + CARD_SPACING);

            // Skip if off screen
            if (cardY + CARD_HEIGHT < y || cardY > y + height) {
                continue;
            }

            boolean hovered = mouseX >= cardX && mouseX < cardX + CARD_WIDTH &&
                            mouseY >= cardY && mouseY < cardY + CARD_HEIGHT &&
                            cardY >= y && cardY + CARD_HEIGHT <= y + height;

            if (hovered) {
                hoveredClipIndex = i;
            }

            renderClipCard(context, textRenderer, theme, clips.get(i),
                          cardX, cardY, CARD_WIDTH, CARD_HEIGHT, hovered);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredClipIndex >= 0 && hoveredClipIndex < clips.size()) {
            ClipInfo clip = clips.get(hoveredClipIndex);
            openClip(clip);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int totalRows = (int) Math.ceil(clips.size() / (double) GRID_COLS);
        int maxScroll = Math.max(0, totalRows * (CARD_HEIGHT + CARD_SPACING) - 400);

        scrollOffset -= (int) (amount * 20);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    /**
     * Render a single clip card.
     */
    private void renderClipCard(DrawContext context, TextRenderer textRenderer,
                                MiracleTheme.Theme theme, ClipInfo clip,
                                int x, int y, int width, int height, boolean hovered) {
        // Card background
        int bgColor = hovered ?
            MiracleTheme.withAlpha(theme.accent500, 0x30) :
            MiracleTheme.withAlpha(theme.surfacePrimary, 0x80);
        context.fill(x, y, x + width, y + height, bgColor);

        // Border
        int borderColor = hovered ? theme.accent500 : MiracleTheme.withAlpha(theme.glassBorder, 0x40);
        GradientRenderer.drawBorder(context, x, y, width, height, borderColor, 1);

        // Thumbnail placeholder (dark background)
        context.fill(x + 4, y + 4, x + width - 4, y + THUMBNAIL_HEIGHT, 0xFF1a1a1a);

        // Play icon overlay
        if (hovered) {
            int iconSize = 24;
            int iconX = x + (width - iconSize) / 2;
            int iconY = y + (THUMBNAIL_HEIGHT - iconSize) / 2;
            renderPlayIcon(context, iconX, iconY, iconSize, theme);
        }

        // Duration badge (bottom right of thumbnail)
        String duration = clip.getDurationFormatted();
        int durationWidth = textRenderer.getWidth(duration) + 6;
        int badgeX = x + width - durationWidth - 8;
        int badgeY = y + THUMBNAIL_HEIGHT - 18;
        context.fill(badgeX, badgeY, badgeX + durationWidth, badgeY + 14,
                    MiracleTheme.withAlpha(0x000000, 0xB0));
        context.drawText(textRenderer, duration, badgeX + 3, badgeY + 3, 0xFFFFFFFF, false);

        // Clip info
        int infoY = y + THUMBNAIL_HEIGHT + 6;

        // Filename (truncated)
        String filename = clip.getFilename();
        if (textRenderer.getWidth(filename) > width - 12) {
            while (textRenderer.getWidth(filename + "...") > width - 12 && filename.length() > 0) {
                filename = filename.substring(0, filename.length() - 1);
            }
            filename += "...";
        }
        context.drawText(textRenderer, filename, x + 6, infoY, theme.textPrimary, false);
        infoY += 12;

        // Date and size
        String metadata = clip.getDateFormatted() + " • " + clip.getSizeFormatted();
        context.drawText(textRenderer, metadata, x + 6, infoY, theme.textSecondary, false);
    }

    /**
     * Render play icon.
     */
    private void renderPlayIcon(DrawContext context, int x, int y, int size, MiracleTheme.Theme theme) {
        // Circle background
        int circleColor = MiracleTheme.withAlpha(0xFFFFFF, 0x90);
        context.fill(x, y + size / 2 - 1, x + size, y + size / 2 + 1, circleColor);
        context.fill(x + size / 2 - 1, y, x + size / 2 + 1, y + size, circleColor);

        // Triangle (simplified as rectangle for now - Minecraft rendering limitations)
        int triSize = size / 3;
        int triX = x + size / 2 - triSize / 3;
        int triY = y + size / 2 - triSize / 2;
        context.fill(triX, triY, triX + triSize, triY + triSize, theme.accent500);
    }

    /**
     * Render empty state when no clips exist.
     */
    private void renderEmptyState(DrawContext context, TextRenderer textRenderer,
                                  MiracleTheme.Theme theme, int x, int y, int width, int height) {
        int centerY = y + height / 2 - 30;

        String title = "No clips yet";
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title, x + (width - titleWidth) / 2, centerY,
                        theme.textPrimary, false);

        String subtitle = "Record clips in-game to see them here";
        int subtitleWidth = textRenderer.getWidth(subtitle);
        context.drawText(textRenderer, subtitle, x + (width - subtitleWidth) / 2, centerY + 14,
                        theme.textSecondary, false);

        // Open folder button
        int buttonWidth = 120;
        int buttonHeight = 24;
        int buttonX = x + (width - buttonWidth) / 2;
        int buttonY = centerY + 40;

        boolean hovered = false; // Will implement properly with mouse tracking
        int bgColor = hovered ?
            MiracleTheme.withAlpha(theme.accent500, 0x40) :
            MiracleTheme.withAlpha(theme.accent500, 0x20);

        context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, bgColor);
        GradientRenderer.drawBorder(context, buttonX, buttonY, buttonWidth, buttonHeight, theme.accent500, 1);

        String buttonText = "Open Clips Folder";
        int textWidth = textRenderer.getWidth(buttonText);
        context.drawText(textRenderer, buttonText, buttonX + (buttonWidth - textWidth) / 2,
                        buttonY + (buttonHeight - 8) / 2, theme.accent400, false);
    }

    /**
     * Load clips from the clips directory.
     */
    private void loadClips() {
        clips.clear();

        try {
            // Get clips directory from system property or default location
            String userHome = System.getProperty("user.home");
            String appData = System.getProperty("os.name").toLowerCase().contains("win") ?
                System.getenv("APPDATA") : userHome;
            Path clipsDir = Paths.get(appData, "MiracleClient", "clips");

            if (!Files.exists(clipsDir)) {
                return;
            }

            Files.list(clipsDir)
                .filter(path -> {
                    String name = path.getFileName().toString().toLowerCase();
                    return name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv");
                })
                .forEach(path -> {
                    try {
                        clips.add(new ClipInfo(path));
                    } catch (Exception e) {
                        // Skip invalid clips
                    }
                });

            // Sort by creation time (newest first)
            clips.sort((a, b) -> Long.compare(b.getCreationTime(), a.getCreationTime()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Open a clip in the system's default video player.
     */
    private void openClip(ClipInfo clip) {
        try {
            java.awt.Desktop.getDesktop().open(clip.getFile());
        } catch (Exception e) {
            e.printStackTrace();
            MinecraftClient.getInstance().player.sendMessage(
                Text.literal("Failed to open clip: " + e.getMessage()).formatted(Formatting.RED),
                false
            );
        }
    }

    /**
     * Info about a single clip.
     */
    private static class ClipInfo {
        private final File file;
        private final long size;
        private final long creationTime;

        public ClipInfo(Path path) throws Exception {
            this.file = path.toFile();
            this.size = Files.size(path);
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            this.creationTime = attrs.creationTime().toMillis();
        }

        public String getFilename() {
            return file.getName();
        }

        public File getFile() {
            return file;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public String getDurationFormatted() {
            // TODO: Read actual duration from metadata if available
            return "0:00";
        }

        public String getDateFormatted() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a");
            return sdf.format(new Date(creationTime));
        }

        public String getSizeFormatted() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
