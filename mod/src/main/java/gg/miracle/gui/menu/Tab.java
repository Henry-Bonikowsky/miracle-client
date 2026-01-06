package gg.miracle.gui.menu;

import net.minecraft.util.Identifier;

/**
 * Enum representing the tabs in the Miracle Client main menu.
 */
public enum Tab {
    HOME("Home", "home"),
    SINGLEPLAYER("Singleplayer", "singleplayer"),
    MULTIPLAYER("Multiplayer", "multiplayer"),
    MODS("Mods", "mods"),
    VERSIONS("Versions", "versions"),
    CLIPS("Clips", "clips"),
    FRIENDS("Friends", "friends"),
    ACCOUNTS("Accounts", "account"),
    SETTINGS("Settings", "settings"),
    QUIT("Quit", "quit");

    private final String displayName;
    private final Identifier iconTexture;

    Tab(String displayName, String iconName) {
        this.displayName = displayName;
        this.iconTexture = Identifier.of("miracle", "textures/gui/icons/" + iconName + ".png");
    }

    public String getDisplayName() {
        return displayName;
    }

    public Identifier getIconTexture() {
        return iconTexture;
    }
}
