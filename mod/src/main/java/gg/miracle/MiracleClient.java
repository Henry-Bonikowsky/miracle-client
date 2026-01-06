package gg.miracle;

import gg.miracle.api.DirectoryManager;
import gg.miracle.api.config.ConfigManager;
import gg.miracle.api.events.EventBus;
import gg.miracle.api.modules.Module;
import gg.miracle.api.modules.ModuleManager;
import gg.miracle.replay.ReplayBufferManager;
import gg.miracle.replay.AutoClipSystem;
import gg.miracle.replay.detection.HighlightDetector;
import gg.miracle.replay.detection.HighlightEvent;
import gg.miracle.gui.MiracleMainMenu;
import gg.miracle.sound.MiracleSounds;
import gg.miracle.modules.movement.ToggleSprint;
import gg.miracle.modules.movement.ToggleSneak;
import gg.miracle.modules.render.Fullbright;
import gg.miracle.modules.render.Freelook;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
/*? if MC_1_21_11 {*/
import net.minecraft.util.Identifier;
/*?}*/
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

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
    private ReplayBufferManager replayBufferManager;
    private AutoClipSystem autoClipSystem;

    /*? if MC_1_21_11 {*/
    private static final KeyBinding.Category MIRACLE_CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
    /*?}*/

    private KeyBinding openMenuKey;
    private KeyBinding freelookKey;
    private final Map<KeyBinding, Class<? extends Module>> moduleKeybinds = new HashMap<>();
    private boolean menuKeyWasDown = false;
    private int menuCooldown = 0;  // Prevent immediate reopen

    @Override
    public void onInitializeClient() {
        instance = this;
        mc = MinecraftClient.getInstance();

        LOGGER.info("Initializing {} v{}", MOD_NAME, MOD_VERSION);

        // Initialize sounds
        MiracleSounds.init();

        // Initialize directory manager
        DirectoryManager.init();

        // Initialize core systems
        this.eventBus = new EventBus();
        this.configManager = new ConfigManager();
        this.moduleManager = new ModuleManager();
        this.replayBufferManager = new ReplayBufferManager();

        // Load configuration
        configManager.load();

        // Register keybindings
        registerKeybindings();

        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        // Register kill detection
        registerKillDetection();
        
        // Start replay buffer recording
        replayBufferManager.startRecording();
        
        // Initialize auto-clip system
        this.autoClipSystem = new AutoClipSystem(replayBufferManager);

        // Set window title
        setWindowTitle();

        LOGGER.info("{} initialized successfully!", MOD_NAME);
    }

    private void registerKeybindings() {
        // Menu keybind
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.miracle.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                /*? if MC_1_21_11 {*/
                MIRACLE_CATEGORY
                /*?} else {*/
                /*"category.miracle"*/
                /*?}*/
        ));

        // Freelook keybind (hold-to-activate, handled separately)
        freelookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.miracle.freelook",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                /*? if MC_1_21_11 {*/
                MIRACLE_CATEGORY
                /*?} else {*/
                /*"category.miracle"*/
                /*?}*/
        ));

        // Module keybinds (toggle on press)
        registerModuleKeybind("key.miracle.toggle_sprint", GLFW.GLFW_KEY_UNKNOWN, ToggleSprint.class);
        registerModuleKeybind("key.miracle.toggle_sneak", GLFW.GLFW_KEY_UNKNOWN, ToggleSneak.class);
        registerModuleKeybind("key.miracle.fullbright", GLFW.GLFW_KEY_UNKNOWN, Fullbright.class);
    }

    private void registerModuleKeybind(String translationKey, int defaultKey, Class<? extends Module> moduleClass) {
        KeyBinding keybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey,
                InputUtil.Type.KEYSYM,
                defaultKey,
                /*? if MC_1_21_11 {*/
                MIRACLE_CATEGORY
                /*?} else {*/
                /*"category.miracle"*/
                /*?}*/
        ));
        moduleKeybinds.put(keybind, moduleClass);
    }
    
    private void registerKillDetection() {
        // Hook into entity attacks to detect kills
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player == mc.player && entity instanceof LivingEntity living) {
                // Check if this attack killed the entity
                if (living.getHealth() <= 0 || living.isDead()) {
                    HighlightDetector detector = replayBufferManager.getHighlightDetector();
                    if (detector != null) {
                        // Determine if PvP or PvE
                        if (entity instanceof PlayerEntity) {
                            detector.registerEvent(HighlightEvent.PVP_KILL);
                        } else {
                            detector.registerEvent(HighlightEvent.KILL);
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    private void setWindowTitle() {
        // Set the window title to "Miracle Client"
        try {
            long window = mc.getWindow().getHandle();
            GLFW.glfwSetWindowTitle(window, MOD_NAME);
            LOGGER.info("Window title set to: {}", MOD_NAME);
        } catch (Exception e) {
            LOGGER.error("Failed to set window title", e);
        }
    }

    private int maximizeDelay = 20;  // Wait 20 ticks (1 second) before maximizing

    private void maximizeWindowOnce() {
        if (maximizeDelay > 0) {
            maximizeDelay--;
            return;
        }
        if (maximizeDelay == 0) {
            maximizeDelay = -1;  // Only try once
            try {
                long window = mc.getWindow().getHandle();
                GLFW.glfwMaximizeWindow(window);
                LOGGER.info("Window maximized");
            } catch (Exception e) {
                LOGGER.error("Failed to maximize window", e);
            }
        }
    }

    private void onClientTick(MinecraftClient client) {
        // Maximize window on first tick (after window is fully ready)
        maximizeWindowOnce();

        // Decrement cooldown
        if (menuCooldown > 0) {
            menuCooldown--;
        }

        // Handle menu keybinding - trigger on rising edge only, with cooldown
        boolean menuKeyDown = openMenuKey.isPressed();
        if (menuKeyDown && !menuKeyWasDown && menuCooldown == 0) {
            if (client.currentScreen == null) {
                client.setScreen(new MiracleMainMenu());
                menuCooldown = 10;  // Prevent rapid toggling
            } else if (client.currentScreen instanceof MiracleMainMenu) {
                client.setScreen(null);
                menuCooldown = 10;  // Prevent immediate reopen
            }
        }
        menuKeyWasDown = menuKeyDown;

        // Consume any pending wasPressed
        while (openMenuKey.wasPressed()) {}

        // Handle freelook (hold-to-activate)
        Freelook freelook = moduleManager.get(Freelook.class);
        if (freelook != null && client.player != null) {
            boolean freelookKeyDown = freelookKey.isPressed();
            freelook.updateHoldState(freelookKeyDown);
        }

        // Handle module keybindings (toggle on press)
        for (Map.Entry<KeyBinding, Class<? extends Module>> entry : moduleKeybinds.entrySet()) {
            while (entry.getKey().wasPressed()) {
                Module module = moduleManager.get(entry.getValue());
                if (module != null) {
                    module.toggle();
                }
            }
        }

        // Capture replay frame
        replayBufferManager.captureFrame();
        
        // Tick auto-clip system
        if (autoClipSystem != null) {
            autoClipSystem.tick();
        }
        
        // Tick modules
        if (client.player != null && client.world != null) {
            moduleManager.onTick();
        }
    }

    private void toggleMenu(MinecraftClient client) {
        if (client.currentScreen == null) {
            client.setScreen(new MiracleMainMenu());
        } else if (client.currentScreen instanceof MiracleMainMenu) {
            client.setScreen(null);
        }
    }

    public void shutdown() {
        LOGGER.info("Shutting down {}...", MOD_NAME);
        replayBufferManager.stopRecording();
        configManager.save();
    }

    // Getters
    public static MiracleClient getInstance() {
        return instance;
    }

    public static MinecraftClient getMinecraft() {
        return mc;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ReplayBufferManager getReplayBufferManager() {
        return replayBufferManager;
    }
    
    public AutoClipSystem getAutoClipSystem() {
        return autoClipSystem;
    }
}
