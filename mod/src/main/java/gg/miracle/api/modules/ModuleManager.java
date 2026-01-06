package gg.miracle.api.modules;

import gg.miracle.MiracleClient;
import gg.miracle.modules.combat.*;
import gg.miracle.modules.hud.*;
import gg.miracle.modules.misc.*;
import gg.miracle.modules.movement.*;
import gg.miracle.modules.render.*;

import java.util.*;

public class ModuleManager {
    private final Map<Class<? extends Module>, Module> modules = new LinkedHashMap<>();

    public ModuleManager() {
        // Register modules
        registerModules();

        MiracleClient.LOGGER.info("Registered {} modules", modules.size());
    }

    private void registerModules() {
        // Combat
        register(new AutoSprint());

        // Movement
        register(new ToggleSprint());
        register(new ToggleSneak());

        // Render
        register(new Fullbright());
        register(new NoHurtCam());
        register(new Freelook());

        // HUD
        register(new FPSDisplay());
        register(new CoordsDisplay());
        register(new ArmorDisplay());
        register(new Keystrokes());
        register(new ReachDisplay());

        // Misc
        register(new DiscordRPC());
        register(new CommandKeybinds());
        register(new AutoClip());
    }

    private void register(Module module) {
        modules.put(module.getClass(), module);
    }

    public void onTick() {
        for (Module module : modules.values()) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> clazz) {
        return (T) modules.get(clazz);
    }

    public Module getByName(String name) {
        return modules.values().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Collection<Module> getAll() {
        return modules.values();
    }

    public List<Module> getByCategory(Category category) {
        return modules.values().stream()
                .filter(m -> m.getCategory() == category)
                .toList();
    }
}
