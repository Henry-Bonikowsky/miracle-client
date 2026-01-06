package gg.miracle.api.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.miracle.MiracleClient;
import gg.miracle.api.modules.Module;
import gg.miracle.gui.theme.MiracleTheme;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir;
    private final Path configFile;
    private boolean isLoading = false;

    public ConfigManager() {
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("miracle");
        this.configFile = configDir.resolve("config.json");
    }

    public void load() {
        isLoading = true;
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (!Files.exists(configFile)) {
                isLoading = false;
                save();
                return;
            }

            String json = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // Load theme
            if (root.has("theme")) {
                String themeName = root.get("theme").getAsString();
                MiracleTheme.setTheme(themeName);
                MiracleClient.LOGGER.info("Loaded theme: {}", themeName);
            }

            if (root.has("modules")) {
                JsonObject modules = root.getAsJsonObject("modules");

                for (Module module : MiracleClient.getInstance().getModuleManager().getAll()) {
                    if (modules.has(module.getName())) {
                        JsonObject moduleJson = modules.getAsJsonObject(module.getName());

                        if (moduleJson.has("enabled")) {
                            module.setEnabled(moduleJson.get("enabled").getAsBoolean());
                        }

                        if (moduleJson.has("keybind")) {
                            module.setKeyBind(moduleJson.get("keybind").getAsInt());
                        }

                        if (moduleJson.has("settings")) {
                            loadSettings(module, moduleJson.getAsJsonObject("settings"));
                        }
                    }
                }
            }

            MiracleClient.LOGGER.info("Configuration loaded");
        } catch (IOException e) {
            MiracleClient.LOGGER.error("Failed to load configuration", e);
        } finally {
            isLoading = false;
        }
    }

    public void save() {
        // Don't save while loading config
        if (isLoading) return;
        
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            JsonObject root = new JsonObject();

            // Save theme
            root.addProperty("theme", MiracleTheme.getCurrentThemeId().name());

            JsonObject modules = new JsonObject();

            for (Module module : MiracleClient.getInstance().getModuleManager().getAll()) {
                JsonObject moduleJson = new JsonObject();
                moduleJson.addProperty("enabled", module.isEnabled());
                moduleJson.addProperty("keybind", module.getKeyBind());

                JsonObject settings = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    settings.add(setting.getName(), GSON.toJsonTree(setting.get()));
                }
                moduleJson.add("settings", settings);

                modules.add(module.getName(), moduleJson);
            }

            root.add("modules", modules);

            Files.writeString(configFile, GSON.toJson(root));
            MiracleClient.LOGGER.info("Configuration saved");
        } catch (IOException e) {
            MiracleClient.LOGGER.error("Failed to save configuration", e);
        }
    }

    /**
     * Set the current theme and save it to config.
     * @param themeName The theme name (e.g., "MIDNIGHT", "AMETHYST")
     */
    public void setTheme(String themeName) {
        MiracleTheme.setTheme(themeName);
        save();
    }

    @SuppressWarnings("unchecked")
    private void loadSettings(Module module, JsonObject settingsJson) {
        for (Setting<?> setting : module.getSettings()) {
            if (settingsJson.has(setting.getName())) {
                try {
                    Object value = GSON.fromJson(
                            settingsJson.get(setting.getName()),
                            setting.getDefaultValue().getClass()
                    );
                    ((Setting<Object>) setting).set(value);
                } catch (Exception e) {
                    MiracleClient.LOGGER.warn("Failed to load setting {} for {}", setting.getName(), module.getName());
                }
            }
        }
    }
}
