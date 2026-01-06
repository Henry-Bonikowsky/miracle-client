package gg.miracle.gui.render;

import gg.miracle.MiracleClient;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.gui.DrawContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Menu background renderer that plays gameplay clips via WATERMeDIA.
 * Falls back to animated gradient + orbs if WATERMeDIA unavailable or no clips.
 */
public class MenuBackground {

    private static final long CLIP_CYCLE_MS = 30000;  // Change clip every 30 seconds
    private static final Random RANDOM = new Random();

    // Fallback renderer (gradient + floating orbs)
    private final CinematicBackground fallbackBackground = new CinematicBackground();

    // Clip playback state
    private List<Path> clips = new ArrayList<>();
    private int currentClipIndex = -1;
    private long lastClipChange = 0;
    private boolean videoAvailable = false;

    // WATERMeDIA player (lazy initialized)
    private Object videoPlayer = null;  // Will be actual WATERMeDIA type when available

    public MenuBackground() {
        scanForClips();
        initVideoPlayer();
    }

    /**
     * Scan the MiracleClient clips folder for video files.
     */
    private void scanForClips() {
        try {
            Path clipsDir = getClipsDirectory();
            if (Files.exists(clipsDir) && Files.isDirectory(clipsDir)) {
                try (Stream<Path> stream = Files.list(clipsDir)) {
                    clips = stream
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mkv");
                        })
                        .toList();
                }
                MiracleClient.LOGGER.info("Found {} clips in clips folder", clips.size());
            }
        } catch (IOException e) {
            MiracleClient.LOGGER.warn("Failed to scan clips folder", e);
        }
    }

    /**
     * Get the clips directory path.
     */
    private Path getClipsDirectory() {
        // Clips are stored in MiracleClient/clips/ (shared across versions)
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        Path dataDir;
        if (os.contains("win")) {
            dataDir = Path.of(System.getenv("APPDATA"), "MiracleClient");
        } else if (os.contains("mac")) {
            dataDir = Path.of(userHome, "Library", "Application Support", "MiracleClient");
        } else {
            dataDir = Path.of(userHome, ".local", "share", "MiracleClient");
        }

        return dataDir.resolve("clips");
    }

    /**
     * Initialize WATERMeDIA video player if available.
     */
    private void initVideoPlayer() {
        try {
            // Check if WATERMeDIA is available
            Class.forName("org.watermedia.api.player.SyncVideoPlayer");
            videoAvailable = true;
            MiracleClient.LOGGER.info("WATERMeDIA available - video backgrounds enabled");

            // Pick initial clip
            if (!clips.isEmpty()) {
                currentClipIndex = RANDOM.nextInt(clips.size());
                lastClipChange = System.currentTimeMillis();
                startClip();
            }
        } catch (ClassNotFoundException e) {
            videoAvailable = false;
            MiracleClient.LOGGER.info("WATERMeDIA not available - using fallback background");
        }
    }

    /**
     * Start playing the current clip.
     */
    private void startClip() {
        if (!videoAvailable || clips.isEmpty() || currentClipIndex < 0) return;

        try {
            Path clipPath = clips.get(currentClipIndex);
            // TODO: Implement actual WATERMeDIA video playback
            // For now, we'll just log and fall back
            MiracleClient.LOGGER.debug("Would play clip: {}", clipPath.getFileName());
        } catch (Exception e) {
            MiracleClient.LOGGER.warn("Failed to start clip playback", e);
        }
    }

    /**
     * Cycle to the next random clip.
     */
    private void cycleClip() {
        if (clips.size() <= 1) return;

        int newIndex;
        do {
            newIndex = RANDOM.nextInt(clips.size());
        } while (newIndex == currentClipIndex);

        currentClipIndex = newIndex;
        lastClipChange = System.currentTimeMillis();
        startClip();
    }

    /**
     * Render the background.
     */
    public void render(DrawContext context, int width, int height) {
        long now = System.currentTimeMillis();

        // Check if it's time to cycle clips
        if (videoAvailable && !clips.isEmpty() && (now - lastClipChange) >= CLIP_CYCLE_MS) {
            cycleClip();
        }

        // Try to render video, fall back to gradient/orbs
        boolean renderedVideo = false;

        if (videoAvailable && videoPlayer != null) {
            // TODO: Render video frame via WATERMeDIA
            // renderedVideo = renderVideoFrame(context, width, height);
        }

        if (!renderedVideo) {
            // Fall back to animated gradient + orbs
            fallbackBackground.render(context, width, height);
        } else {
            // Add overlay for readability when video is playing
            MiracleTheme.Theme theme = MiracleTheme.current();
            context.fill(0, 0, width, height, MiracleTheme.withAlpha(theme.bgFrom, 0x99));
        }
    }

    /**
     * Check if video playback is available.
     */
    public boolean isVideoAvailable() {
        return videoAvailable && !clips.isEmpty();
    }

    /**
     * Get the number of available clips.
     */
    public int getClipCount() {
        return clips.size();
    }

    /**
     * Refresh the clips list (e.g., after recording new clip).
     */
    public void refreshClips() {
        scanForClips();
    }

    /**
     * Clean up resources when closing.
     */
    public void close() {
        if (videoPlayer != null) {
            // TODO: Stop and release video player
            videoPlayer = null;
        }
    }
}
