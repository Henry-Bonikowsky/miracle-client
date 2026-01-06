package gg.miracle.replay;

import gg.miracle.api.DirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages saved cinematic clips.
 * Handles the 10-clip limit and favorites system.
 */
public class ClipManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ClipManager");
    private static final int MAX_NON_FAVORITE_CLIPS = 10;
    
    private final Path clipsDirectory;
    private final List<ClipMetadata> clips = new ArrayList<>();
    
    public ClipManager() {
        this.clipsDirectory = DirectoryManager.getBackgroundVideosDir();
        
        // Ensure directory exists
        try {
            Files.createDirectories(clipsDirectory);
        } catch (IOException e) {
            LOGGER.error("Failed to create clips directory", e);
        }
        
        // Load existing clips
        loadClips();
    }
    
    /**
     * Load all clip metadata from disk.
     */
    private void loadClips() {
        clips.clear();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(clipsDirectory, "*.meta")) {
            for (Path metaFile : stream) {
                try {
                    ClipMetadata metadata = ClipMetadata.load(metaFile);
                    clips.add(metadata);
                } catch (IOException e) {
                    LOGGER.warn("Failed to load clip metadata: {}", metaFile, e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to list clip metadata files", e);
        }
        
        LOGGER.info("Loaded {} clips ({} favorites)", clips.size(), getFavoriteClips().size());
    }
    
    /**
     * Save a new clip and enforce the 10-clip limit.
     */
    public void saveClip(ClipMetadata metadata) {
        clips.add(metadata);
        
        // Save metadata to disk
        try {
            metadata.save(clipsDirectory);
        } catch (IOException e) {
            LOGGER.error("Failed to save clip metadata", e);
        }
        
        // Enforce limit
        enforceClipLimit();
        
        LOGGER.info("Saved clip: {} ({})", metadata.getClipId(), metadata.getCameraStyle());
    }
    
    /**
     * Delete oldest non-favorite clips if over the limit.
     */
    private void enforceClipLimit() {
        List<ClipMetadata> nonFavorites = getNonFavoriteClips();
        
        if (nonFavorites.size() <= MAX_NON_FAVORITE_CLIPS) {
            return;
        }
        
        // Sort by timestamp (oldest first)
        nonFavorites.sort(Comparator.comparingLong(ClipMetadata::getTimestamp));
        
        // Delete oldest until we're at the limit
        int toDelete = nonFavorites.size() - MAX_NON_FAVORITE_CLIPS;
        for (int i = 0; i < toDelete; i++) {
            deleteClip(nonFavorites.get(i));
        }
        
        LOGGER.info("Deleted {} old clips to maintain limit", toDelete);
    }
    
    /**
     * Delete a clip and its metadata.
     */
    public void deleteClip(ClipMetadata metadata) {
        clips.remove(metadata);
        
        try {
            // Delete video file
            Path videoFile = clipsDirectory.resolve(metadata.getVideoFileName());
            Files.deleteIfExists(videoFile);
            
            // Delete metadata file
            Path metaFile = clipsDirectory.resolve(metadata.getMetadataFileName());
            Files.deleteIfExists(metaFile);
            
            LOGGER.info("Deleted clip: {}", metadata.getClipId());
        } catch (IOException e) {
            LOGGER.error("Failed to delete clip files", e);
        }
    }
    
    /**
     * Toggle favorite status for a clip.
     */
    public void toggleFavorite(ClipMetadata metadata) {
        metadata.setFavorite(!metadata.isFavorite());
        
        try {
            metadata.save(clipsDirectory);
            LOGGER.info("Toggled favorite for clip: {} (now: {})", 
                metadata.getClipId(), metadata.isFavorite());
        } catch (IOException e) {
            LOGGER.error("Failed to save favorite status", e);
        }
    }
    
    /**
     * Get all clips.
     */
    public List<ClipMetadata> getAllClips() {
        return new ArrayList<>(clips);
    }
    
    /**
     * Get non-favorite clips.
     */
    public List<ClipMetadata> getNonFavoriteClips() {
        return clips.stream()
            .filter(clip -> !clip.isFavorite())
            .collect(Collectors.toList());
    }
    
    /**
     * Get favorite clips.
     */
    public List<ClipMetadata> getFavoriteClips() {
        return clips.stream()
            .filter(ClipMetadata::isFavorite)
            .collect(Collectors.toList());
    }
    
    /**
     * Get a random clip (for launcher background).
     */
    public ClipMetadata getRandomClip() {
        if (clips.isEmpty()) {
            return null;
        }
        
        Random random = new Random();
        return clips.get(random.nextInt(clips.size()));
    }
    
    /**
     * Get the clips directory path.
     */
    public Path getClipsDirectory() {
        return clipsDirectory;
    }
}
