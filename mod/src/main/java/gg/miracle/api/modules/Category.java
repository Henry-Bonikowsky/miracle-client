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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
