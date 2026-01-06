# Miracle Client Mod - Code Backup

This file contains all the source code from the mod before the Stonecutter migration.

## Project Structure

```
src/main/java/gg/miracle/
├── MiracleClient.java          # Main mod entry point
├── api/
│   ├── config/
│   │   ├── ConfigManager.java  # JSON config save/load
│   │   └── Setting.java        # Setting wrapper class
│   ├── events/
│   │   ├── Cancellable.java    # Cancellable event interface
│   │   ├── Event.java          # Base event class
│   │   ├── EventBus.java       # Event dispatcher
│   │   └── Subscribe.java      # Event subscription annotation
│   └── modules/
│       ├── Category.java       # Module categories enum
│       ├── Module.java         # Base module class
│       └── ModuleManager.java  # Module registry
├── gui/
│   └── MiracleMainMenu.java    # In-game settings GUI
├── integration/
│   └── ModMenuIntegration.java # ModMenu API integration
├── mixin/
│   ├── GameRendererMixin.java  # NoHurtCam implementation
│   ├── InGameHudMixin.java     # HUD rendering hook
│   ├── KeyboardMixin.java      # Keybind handling
│   ├── MinecraftClientMixin.java # Shutdown hook
│   ├── TitleScreenMixin.java   # Title screen branding
│   └── WindowTitleMixin.java   # Window title override
└── modules/
    ├── combat/
    │   └── AutoSprint.java
    ├── hud/
    │   ├── ArmorDisplay.java
    │   ├── CoordsDisplay.java
    │   └── FPSDisplay.java
    ├── misc/
    │   └── DiscordRPC.java
    └── render/
        ├── Fullbright.java
        └── NoHurtCam.java
```

## Resources

```
src/main/resources/
├── fabric.mod.json
├── miracle.accesswidener
└── miracle.mixins.json
```

---

## Source Files

### MiracleClient.java
```java
package gg.miracle;

import gg.miracle.api.config.ConfigManager;
import gg.miracle.api.events.EventBus;
import gg.miracle.api.modules.ModuleManager;
import gg.miracle.gui.MiracleMainMenu;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiracleClient implements ClientModInitializer {
    public static final String MOD_ID = "miracle";
    public static final String MOD_NAME = "Miracle Client";
    public static final String MOD_VERSION = "1.0.0";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static MiracleClient instance;
    private static MinecraftClient mc;

    private EventBus eventBus;
    private ModuleManager moduleManager;
    private ConfigManager configManager;

    private KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        instance = this;
        mc = MinecraftClient.getInstance();

        LOGGER.info("Initializing {} v{}", MOD_NAME, MOD_VERSION);

        // Initialize core systems
        this.eventBus = new EventBus();
        this.configManager = new ConfigManager();
        this.moduleManager = new ModuleManager();

        // Load configuration
        configManager.load();

        // Register keybindings
        registerKeybindings();

        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Set window title
        setWindowTitle();

        LOGGER.info("{} initialized successfully!", MOD_NAME);
    }

    private void registerKeybindings() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.miracle.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.miracle"
        ));
    }

    private void setWindowTitle() {
        try {
            long window = mc.getWindow().getHandle();
            GLFW.glfwSetWindowTitle(window, MOD_NAME);
            LOGGER.info("Window title set to: {}", MOD_NAME);
        } catch (Exception e) {
            LOGGER.error("Failed to set window title", e);
        }
    }

    private void onClientTick(MinecraftClient client) {
        // Tick modules
        if (client.player != null && client.world != null) {
            moduleManager.onTick();
        }
    }

    public void shutdown() {
        LOGGER.info("Shutting down {}...", MOD_NAME);
        configManager.save();
    }

    public static MiracleClient getInstance() { return instance; }
    public static MinecraftClient getMinecraft() { return mc; }
    public EventBus getEventBus() { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
}
```

### api/modules/Module.java
```java
package gg.miracle.api.modules;

import gg.miracle.MiracleClient;
import gg.miracle.api.config.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected final MinecraftClient mc = MiracleClient.getMinecraft();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled = false;
    private int keyBind = -1;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            onEnable();
            MiracleClient.getInstance().getEventBus().register(this);
        } else {
            MiracleClient.getInstance().getEventBus().unregister(this);
            onDisable();
        }
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}

    protected <T> Setting<T> register(Setting<T> setting) {
        settings.add(setting);
        return setting;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKeyBind() { return keyBind; }
    public void setKeyBind(int keyBind) { this.keyBind = keyBind; }
    public List<Setting<?>> getSettings() { return settings; }
}
```

### api/modules/Category.java
```java
package gg.miracle.api.modules;

public enum Category {
    COMBAT("Combat", "Combat-related modules"),
    RENDER("Render", "Visual enhancements and overlays"),
    MOVEMENT("Movement", "Movement modifications"),
    PLAYER("Player", "Player utility modules"),
    WORLD("World", "World interaction modules"),
    HUD("HUD", "Heads-up display elements"),
    MISC("Misc", "Miscellaneous modules");

    private final String name;
    private final String description;

    Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}
```

### api/modules/ModuleManager.java
```java
package gg.miracle.api.modules;

import gg.miracle.MiracleClient;
import gg.miracle.modules.combat.*;
import gg.miracle.modules.hud.*;
import gg.miracle.modules.misc.*;
import gg.miracle.modules.render.*;

import java.util.*;

public class ModuleManager {
    private final Map<Class<? extends Module>, Module> modules = new LinkedHashMap<>();

    public ModuleManager() {
        registerModules();
        MiracleClient.LOGGER.info("Registered {} modules", modules.size());
    }

    private void registerModules() {
        // Combat
        register(new AutoSprint());
        // Render
        register(new Fullbright());
        register(new NoHurtCam());
        // HUD
        register(new FPSDisplay());
        register(new CoordsDisplay());
        register(new ArmorDisplay());
        // Misc
        register(new DiscordRPC());
    }

    private void register(Module module) { modules.put(module.getClass(), module); }

    public void onTick() {
        for (Module module : modules.values()) {
            if (module.isEnabled()) module.onTick();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> clazz) { return (T) modules.get(clazz); }

    public Module getByName(String name) {
        return modules.values().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public Collection<Module> getAll() { return modules.values(); }

    public List<Module> getByCategory(Category category) {
        return modules.values().stream()
                .filter(m -> m.getCategory() == category)
                .toList();
    }
}
```

### api/events/EventBus.java
```java
package gg.miracle.api.events;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private final Map<Class<?>, List<EventSubscriber>> subscribers = new ConcurrentHashMap<>();

    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                if (method.getParameterCount() != 1) {
                    throw new IllegalArgumentException("Event handler must have exactly one parameter: " + method);
                }
                Class<?> eventType = method.getParameterTypes()[0];
                Subscribe annotation = method.getAnnotation(Subscribe.class);
                method.setAccessible(true);
                subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                        .add(new EventSubscriber(listener, method, annotation.priority()));
            }
        }
        subscribers.values().forEach(list ->
                list.sort(Comparator.comparingInt(EventSubscriber::priority).reversed()));
    }

    public void unregister(Object listener) {
        subscribers.values().forEach(list -> list.removeIf(sub -> sub.instance() == listener));
    }

    public <T extends Event> T post(T event) {
        List<EventSubscriber> subs = subscribers.get(event.getClass());
        if (subs != null) {
            for (EventSubscriber sub : subs) {
                if (event instanceof Cancellable cancellable && cancellable.isCancelled()) break;
                try { sub.method().invoke(sub.instance(), event); }
                catch (Exception e) { e.printStackTrace(); }
            }
        }
        return event;
    }

    private record EventSubscriber(Object instance, Method method, int priority) {}
}
```

### api/events/Event.java
```java
package gg.miracle.api.events;

public abstract class Event {}
```

### api/events/Subscribe.java
```java
package gg.miracle.api.events;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
    int priority() default 0;
}
```

### api/events/Cancellable.java
```java
package gg.miracle.api.events;

public interface Cancellable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}
```

### api/config/Setting.java
```java
package gg.miracle.api.config;

import java.util.function.Consumer;

public class Setting<T> {
    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;
    private Consumer<T> onChange;
    private T min;
    private T max;

    public Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public static <T> Setting<T> of(String name, String description, T defaultValue) {
        return new Setting<>(name, description, defaultValue);
    }

    public static Setting<Integer> ofInt(String name, String description, int defaultValue, int min, int max) {
        Setting<Integer> setting = new Setting<>(name, description, defaultValue);
        setting.min = min;
        setting.max = max;
        return setting;
    }

    public static Setting<Double> ofDouble(String name, String description, double defaultValue, double min, double max) {
        Setting<Double> setting = new Setting<>(name, description, defaultValue);
        setting.min = min;
        setting.max = max;
        return setting;
    }

    public static Setting<Float> ofFloat(String name, String description, float defaultValue, float min, float max) {
        Setting<Float> setting = new Setting<>(name, description, defaultValue);
        setting.min = min;
        setting.max = max;
        return setting;
    }

    public Setting<T> onChange(Consumer<T> onChange) { this.onChange = onChange; return this; }
    public T get() { return value; }
    public void set(T value) { this.value = value; if (onChange != null) onChange.accept(value); }
    public void reset() { set(defaultValue); }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public T getDefaultValue() { return defaultValue; }
    public T getMin() { return min; }
    public T getMax() { return max; }
    public boolean hasRange() { return min != null && max != null; }
}
```

### api/config/ConfigManager.java
```java
package gg.miracle.api.config;

import com.google.gson.*;
import gg.miracle.MiracleClient;
import gg.miracle.api.modules.Module;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir;
    private final Path configFile;

    public ConfigManager() {
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("miracle");
        this.configFile = configDir.resolve("config.json");
    }

    public void load() {
        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            if (!Files.exists(configFile)) { save(); return; }

            String json = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (root.has("modules")) {
                JsonObject modules = root.getAsJsonObject("modules");
                for (Module module : MiracleClient.getInstance().getModuleManager().getAll()) {
                    if (modules.has(module.getName())) {
                        JsonObject moduleJson = modules.getAsJsonObject(module.getName());
                        if (moduleJson.has("enabled")) module.setEnabled(moduleJson.get("enabled").getAsBoolean());
                        if (moduleJson.has("keybind")) module.setKeyBind(moduleJson.get("keybind").getAsInt());
                        if (moduleJson.has("settings")) loadSettings(module, moduleJson.getAsJsonObject("settings"));
                    }
                }
            }
            MiracleClient.LOGGER.info("Configuration loaded");
        } catch (IOException e) {
            MiracleClient.LOGGER.error("Failed to load configuration", e);
        }
    }

    public void save() {
        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            JsonObject root = new JsonObject();
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

    @SuppressWarnings("unchecked")
    private void loadSettings(Module module, JsonObject settingsJson) {
        for (Setting<?> setting : module.getSettings()) {
            if (settingsJson.has(setting.getName())) {
                try {
                    Object value = GSON.fromJson(settingsJson.get(setting.getName()), setting.getDefaultValue().getClass());
                    ((Setting<Object>) setting).set(value);
                } catch (Exception e) {
                    MiracleClient.LOGGER.warn("Failed to load setting {} for {}", setting.getName(), module.getName());
                }
            }
        }
    }
}
```

### gui/MiracleMainMenu.java
```java
package gg.miracle.gui;

import gg.miracle.MiracleClient;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class MiracleMainMenu extends Screen {
    private final Screen parent;
    private Category selectedCategory = Category.COMBAT;
    private int menuX, menuY;
    private int menuWidth = 400;
    private int menuHeight = 300;

    public MiracleMainMenu() { this(null); }

    public MiracleMainMenu(Screen parent) {
        super(Text.literal("Miracle Client"));
        this.parent = parent;
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }

    @Override
    protected void init() {
        menuX = (width - menuWidth) / 2;
        menuY = (height - menuHeight) / 2;

        int buttonY = menuY + 30;
        for (Category category : Category.values()) {
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(category.getName()),
                    button -> selectedCategory = category
            ).dimensions(menuX + 10, buttonY, 80, 20).build());
            buttonY += 25;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xCC000000);
        context.drawBorder(menuX, menuY, menuWidth, menuHeight, 0xFF00BFFF);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(MiracleClient.MOD_NAME + " v" + MiracleClient.MOD_VERSION),
                width / 2, menuY + 8, 0x00BFFF);

        List<Module> modules = MiracleClient.getInstance().getModuleManager().getByCategory(selectedCategory);
        int moduleY = menuY + 30;
        int moduleX = menuX + 100;

        for (Module module : modules) {
            int color = module.isEnabled() ? 0x00FF00 : 0xFFFFFF;
            context.drawTextWithShadow(textRenderer, Text.literal(module.getName()), moduleX, moduleY, color);
            context.fill(moduleX + 150, moduleY - 2, moduleX + 180, moduleY + 12,
                    module.isEnabled() ? 0xFF00AA00 : 0xFF333333);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(module.isEnabled() ? "ON" : "OFF"),
                    moduleX + 165, moduleY, 0xFFFFFF);
            moduleY += 20;
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Module> modules = MiracleClient.getInstance().getModuleManager().getByCategory(selectedCategory);
        int moduleY = menuY + 30;
        int moduleX = menuX + 100;

        for (Module module : modules) {
            if (mouseX >= moduleX + 150 && mouseX <= moduleX + 180 &&
                    mouseY >= moduleY - 2 && mouseY <= moduleY + 12) {
                module.toggle();
                return true;
            }
            moduleY += 20;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() { return false; }
}
```

### integration/ModMenuIntegration.java
```java
package gg.miracle.integration;

import gg.miracle.gui.MiracleMainMenu;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MiracleMainMenu::new;
    }
}
```

### modules/combat/AutoSprint.java
```java
package gg.miracle.modules.combat;

import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;

public class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", "Automatically sprints when moving forward", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.input.movementForward > 0 && !mc.player.isSprinting() && !mc.player.isUsingItem()) {
            mc.player.setSprinting(true);
        }
    }
}
```

### modules/render/Fullbright.java
```java
package gg.miracle.modules.render;

import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;

public class Fullbright extends Module {
    private double previousGamma;

    public Fullbright() {
        super("Fullbright", "Makes everything fully bright", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        previousGamma = mc.options.getGamma().getValue();
        mc.options.getGamma().setValue(16.0);
    }

    @Override
    protected void onDisable() {
        mc.options.getGamma().setValue(previousGamma);
    }
}
```

### modules/render/NoHurtCam.java
```java
package gg.miracle.modules.render;

import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;

public class NoHurtCam extends Module {
    public NoHurtCam() {
        super("NoHurtCam", "Disables the screen shake when taking damage", Category.RENDER);
    }
    // Implementation is in GameRendererMixin
}
```

### modules/hud/FPSDisplay.java
```java
package gg.miracle.modules.hud;

import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class FPSDisplay extends Module {
    private final Setting<Integer> x = register(Setting.ofInt("X", "X position", 5, 0, 1920));
    private final Setting<Integer> y = register(Setting.ofInt("Y", "Y position", 5, 0, 1080));
    private final Setting<Integer> color = register(Setting.of("Color", "Text color", 0xFFFFFF));

    public FPSDisplay() {
        super("FPS", "Displays current FPS", Category.HUD);
    }

    public void render(DrawContext context) {
        if (!isEnabled() || mc.player == null) return;
        String fpsText = "FPS: " + mc.getCurrentFps();
        context.drawTextWithShadow(mc.textRenderer, fpsText, x.get(), y.get(), color.get());
    }
}
```

### modules/hud/CoordsDisplay.java
```java
package gg.miracle.modules.hud;

import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class CoordsDisplay extends Module {
    private final Setting<Integer> x = register(Setting.ofInt("X", "X position", 5, 0, 1920));
    private final Setting<Integer> y = register(Setting.ofInt("Y", "Y position", 20, 0, 1080));
    private final Setting<Integer> color = register(Setting.of("Color", "Text color", 0xFFFFFF));

    public CoordsDisplay() {
        super("Coords", "Displays current coordinates", Category.HUD);
    }

    public void render(DrawContext context) {
        if (!isEnabled() || mc.player == null) return;
        String coords = String.format("XYZ: %.1f / %.1f / %.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
        context.drawTextWithShadow(mc.textRenderer, coords, x.get(), y.get(), color.get());
    }
}
```

### modules/hud/ArmorDisplay.java
```java
package gg.miracle.modules.hud;

import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class ArmorDisplay extends Module {
    private final Setting<Integer> x = register(Setting.ofInt("X", "X position", 5, 0, 1920));
    private final Setting<Integer> y = register(Setting.ofInt("Y", "Y position", 50, 0, 1080));

    public ArmorDisplay() {
        super("Armor", "Displays armor durability", Category.HUD);
    }

    public void render(DrawContext context) {
        if (!isEnabled() || mc.player == null) return;

        int yOffset = 0;
        for (ItemStack stack : mc.player.getArmorItems()) {
            if (!stack.isEmpty()) {
                context.drawItem(stack, x.get(), y.get() + yOffset);
                if (stack.isDamageable()) {
                    int durability = stack.getMaxDamage() - stack.getDamage();
                    int maxDurability = stack.getMaxDamage();
                    float percent = (float) durability / maxDurability;
                    int color = percent > 0.5f ? 0x00FF00 : percent > 0.25f ? 0xFFFF00 : 0xFF0000;
                    context.drawTextWithShadow(mc.textRenderer,
                            durability + "/" + maxDurability,
                            x.get() + 20, y.get() + yOffset + 4, color);
                }
                yOffset += 18;
            }
        }
    }
}
```

### modules/misc/DiscordRPC.java
```java
package gg.miracle.modules.misc;

import gg.miracle.MiracleClient;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;

public class DiscordRPC extends Module {
    public DiscordRPC() {
        super("DiscordRPC", "Shows Miracle Client in Discord", Category.MISC);
    }

    @Override
    protected void onEnable() {
        MiracleClient.LOGGER.info("Discord RPC enabled");
    }

    @Override
    protected void onDisable() {
        MiracleClient.LOGGER.info("Discord RPC disabled");
    }
}
```

### mixin/MinecraftClientMixin.java
```java
package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        MiracleClient.getInstance().shutdown();
    }
}
```

### mixin/KeyboardMixin.java
```java
package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import gg.miracle.api.modules.Module;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action != 1 || client.currentScreen != null) return;
        for (Module module : MiracleClient.getInstance().getModuleManager().getAll()) {
            if (module.getKeyBind() == key) module.toggle();
        }
    }
}
```

### mixin/InGameHudMixin.java
```java
package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import gg.miracle.modules.hud.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        var moduleManager = MiracleClient.getInstance().getModuleManager();

        FPSDisplay fpsDisplay = moduleManager.get(FPSDisplay.class);
        if (fpsDisplay != null) fpsDisplay.render(context);

        CoordsDisplay coordsDisplay = moduleManager.get(CoordsDisplay.class);
        if (coordsDisplay != null) coordsDisplay.render(context);

        ArmorDisplay armorDisplay = moduleManager.get(ArmorDisplay.class);
        if (armorDisplay != null) armorDisplay.render(context);
    }
}
```

### mixin/GameRendererMixin.java
```java
package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import gg.miracle.modules.render.NoHurtCam;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoHurtCam noHurtCam = MiracleClient.getInstance().getModuleManager().get(NoHurtCam.class);
        if (noHurtCam != null && noHurtCam.isEnabled()) ci.cancel();
    }
}
```

### mixin/TitleScreenMixin.java
```java
package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // TODO: Fix for 1.21.8 - drawText signature may have changed
    }
}
```

### mixin/WindowTitleMixin.java
```java
package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowTitleMixin {
    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void onSetTitle(String title, CallbackInfo ci) {
        ci.cancel();
        Window window = (Window) (Object) this;
        long handle = window.getHandle();
        org.lwjgl.glfw.GLFW.glfwSetWindowTitle(handle, MiracleClient.MOD_NAME);
    }
}
```

---

## Resource Files

### fabric.mod.json
```json
{
  "schemaVersion": 1,
  "id": "miracle",
  "version": "${version}",
  "name": "Miracle Client",
  "description": "A modern Minecraft client with performance optimizations and quality of life features.",
  "authors": ["Miracle Team"],
  "contact": {
    "homepage": "https://miracle.gg",
    "sources": "https://github.com/miracleclient/miracle"
  },
  "license": "All Rights Reserved",
  "icon": "assets/miracle/icon.png",
  "environment": "client",
  "entrypoints": {
    "client": ["gg.miracle.MiracleClient"],
    "modmenu": ["gg.miracle.integration.ModMenuIntegration"]
  },
  "mixins": ["miracle.mixins.json"],
  "accessWidener": "miracle.accesswidener",
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": "${minecraft_version}",
    "java": ">=21",
    "fabric-api": "*"
  },
  "recommends": {
    "sodium": "*",
    "iris": "*"
  },
  "custom": {
    "modmenu": {
      "links": {
        "modmenu.discord": "https://discord.gg/miracle"
      }
    }
  }
}
```

### miracle.mixins.json
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "gg.miracle.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": [
    "MinecraftClientMixin",
    "KeyboardMixin",
    "InGameHudMixin",
    "GameRendererMixin",
    "TitleScreenMixin",
    "WindowTitleMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

### miracle.accesswidener
```
accessWidener v2 named

# Minecraft Client access
accessible field net/minecraft/client/MinecraftClient itemUseCooldown I
accessible field net/minecraft/client/MinecraftClient attackCooldown I
accessible method net/minecraft/client/MinecraftClient doItemUse ()V
accessible method net/minecraft/client/MinecraftClient doAttack ()Z

# GUI access
accessible field net/minecraft/client/gui/screen/Screen width I
accessible field net/minecraft/client/gui/screen/Screen height I

# Render access
accessible field net/minecraft/client/render/GameRenderer zoom F
accessible field net/minecraft/client/render/GameRenderer zoomX F
accessible field net/minecraft/client/render/GameRenderer zoomY F
```
