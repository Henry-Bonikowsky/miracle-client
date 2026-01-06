package gg.miracle.replay;

import gg.miracle.replay.camera.CameraStyle;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

/**
 * Metadata for a saved cinematic clip.
 */
public class ClipMetadata {
    private final String clipId;
    private final long timestamp;
    private final int durationSeconds;
    private final CameraStyle cameraStyle;
    private boolean favorite;
    
    public ClipMetadata(String clipId, long timestamp, int durationSeconds, CameraStyle cameraStyle, boolean favorite) {
        this.clipId = clipId;
        this.timestamp = timestamp;
        this.durationSeconds = durationSeconds;
        this.cameraStyle = cameraStyle;
        this.favorite = favorite;
    }
    
    public String getClipId() { return clipId; }
    public long getTimestamp() { return timestamp; }
    public int getDurationSeconds() { return durationSeconds; }
    public CameraStyle getCameraStyle() { return cameraStyle; }
    public boolean isFavorite() { return favorite; }
    
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
    
    /**
     * Get the expected video file path for this clip.
     */
    public String getVideoFileName() {
        return clipId + ".mp4";
    }
    
    /**
     * Get the metadata file path for this clip.
     */
    public String getMetadataFileName() {
        return clipId + ".meta";
    }
    
    /**
     * Save metadata to a file.
     */
    public void save(Path directory) throws IOException {
        Properties props = new Properties();
        props.setProperty("clipId", clipId);
        props.setProperty("timestamp", String.valueOf(timestamp));
        props.setProperty("durationSeconds", String.valueOf(durationSeconds));
        props.setProperty("cameraStyle", cameraStyle.name());
        props.setProperty("favorite", String.valueOf(favorite));
        
        Path metaFile = directory.resolve(getMetadataFileName());
        try (OutputStream out = Files.newOutputStream(metaFile)) {
            props.store(out, "Miracle Client - Cinematic Clip Metadata");
        }
    }
    
    /**
     * Load metadata from a file.
     */
    public static ClipMetadata load(Path metaFile) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(metaFile)) {
            props.load(in);
        }
        
        return new ClipMetadata(
            props.getProperty("clipId"),
            Long.parseLong(props.getProperty("timestamp")),
            Integer.parseInt(props.getProperty("durationSeconds")),
            CameraStyle.valueOf(props.getProperty("cameraStyle")),
            Boolean.parseBoolean(props.getProperty("favorite"))
        );
    }
    
    /**
     * Create a new clip metadata with current timestamp.
     */
    public static ClipMetadata create(int durationSeconds, CameraStyle cameraStyle) {
        long now = Instant.now().toEpochMilli();
        String clipId = "clip_" + now;
        return new ClipMetadata(clipId, now, durationSeconds, cameraStyle, false);
    }
}
