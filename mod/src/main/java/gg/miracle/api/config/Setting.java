package gg.miracle.api.config;

import java.util.function.Consumer;

public class Setting<T> {
    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;
    private Consumer<T> onChange;

    // For number settings
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

    public static Setting<Boolean> ofBool(String name, String description, boolean defaultValue) {
        return new Setting<>(name, description, defaultValue);
    }

    public static Setting<String> ofString(String name, String description, String defaultValue) {
        return new Setting<>(name, description, defaultValue);
    }

    public Setting<T> onChange(Consumer<T> onChange) {
        this.onChange = onChange;
        return this;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
        if (onChange != null) {
            onChange.accept(value);
        }
    }

    public void reset() {
        set(defaultValue);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }

    public boolean hasRange() {
        return min != null && max != null;
    }
}
