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

    public void toggle() {
        setEnabled(!enabled);
    }

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
        
        // Save config immediately when module state changes
        MiracleClient.getInstance().getConfigManager().save();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}

    // Settings
    protected <T> Setting<T> register(Setting<T> setting) {
        settings.add(setting);
        return setting;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }
}
