package gg.miracle.api;

import gg.miracle.MiracleClient;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages custom directory paths for profile-specific content.
 * Reads JVM system properties set by the launcher to override default Minecraft directories.
 */
public class DirectoryManager {
    private static Path customResourcePacksDir = null;
    private static Path customShaderPacksDir = null;
    private static Path customDataPacksDir = null;

    /**
     * Initialize custom directories from JVM system properties.
     * Called during mod initialization.
     */
    public static void init() {
        // Read custom directories from JVM args
        String resourcePacksPath = System.getProperty("miracle.resourcepacksFolder");
        String shaderPacksPath = System.getProperty("miracle.shaderpacksFolder");
        String dataPacksPath = System.getProperty("miracle.datapacksFolder");

        if (resourcePacksPath != null && !resourcePacksPath.isEmpty()) {
            customResourcePacksDir = Paths.get(resourcePacksPath);
            MiracleClient.LOGGER.info("Custom resourcepacks directory: {}", customResourcePacksDir);
        }

        if (shaderPacksPath != null && !shaderPacksPath.isEmpty()) {
            customShaderPacksDir = Paths.get(shaderPacksPath);
            MiracleClient.LOGGER.info("Custom shaderpacks directory: {}", customShaderPacksDir);
        }

        if (dataPacksPath != null && !dataPacksPath.isEmpty()) {
            customDataPacksDir = Paths.get(dataPacksPath);
            MiracleClient.LOGGER.info("Custom datapacks directory: {}", customDataPacksDir);
        }

        if (customResourcePacksDir == null && customShaderPacksDir == null && customDataPacksDir == null) {
            MiracleClient.LOGGER.info("No custom directories specified, using Minecraft defaults");
        }
    }

    /**
     * Get the custom resource packs directory, or null if not set.
     */
    public static Path getCustomResourcePacksDir() {
        return customResourcePacksDir;
    }

    /**
     * Get the custom shader packs directory, or null if not set.
     */
    public static Path getCustomShaderPacksDir() {
        return customShaderPacksDir;
    }

    /**
     * Get the custom data packs directory, or null if not set.
     */
    public static Path getCustomDataPacksDir() {
        return customDataPacksDir;
    }

    /**
     * Check if custom resource packs directory is set.
     */
    public static boolean hasCustomResourcePacksDir() {
        return customResourcePacksDir != null;
    }

    /**
     * Check if custom shader packs directory is set.
     */
    public static boolean hasCustomShaderPacksDir() {
        return customShaderPacksDir != null;
    }

    /**
     * Check if custom data packs directory is set.
     */
    public static boolean hasCustomDataPacksDir() {
        return customDataPacksDir != null;
    }
    
    /**
     * Get the background videos directory (for cinematic clips).
     * Stored in AppData/MiracleClient/background-videos/
     */
    public static Path getBackgroundVideosDir() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            // Fallback for non-Windows or if APPDATA not set
            appData = System.getProperty("user.home");
        }
        return Paths.get(appData, "MiracleClient", "background-videos");
    }
}
