package gg.miracle.gui;

import gg.miracle.MiracleClient;
import gg.miracle.gui.menu.Ribbon;
import gg.miracle.gui.menu.Tab;
import gg.miracle.gui.menu.TabBar;
import gg.miracle.gui.menu.TabContent;
import gg.miracle.gui.menu.tabs.AccountsTabContent;
import gg.miracle.gui.menu.tabs.HomeTabContent;
import gg.miracle.gui.menu.tabs.ClipsTabContent;
import gg.miracle.gui.menu.tabs.SettingsTabContent;
import gg.miracle.gui.render.MenuBackground;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
/*? if MC_1_21_11 {*/
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
/*?}*/

/**
 * Custom title screen with tab-based sidebar layout.
 * Layout: TabBar (40px) | Ribbon (250px) | Background
 */
public class MiracleTitleScreen extends Screen {

    private final MenuBackground background = new MenuBackground();
    private final TabBar tabBar = new TabBar();
    private final Ribbon ribbon = new Ribbon();

    // Content cache for each tab
    private final TabContent[] tabContents = new TabContent[Tab.values().length];

    public MiracleTitleScreen() {
        super(Text.literal("Miracle Client"));
    }

    @Override
    protected void init() {
        // Calculate panel height (full screen height with margin)
        int panelHeight = this.height;

        // Position tab bar on the left
        tabBar.setBounds(0, 0, panelHeight);

        // Position ribbon next to tab bar
        ribbon.setBounds(TabBar.WIDTH, 0, panelHeight);

        // Initialize with home tab
        switchToTab(Tab.HOME);
    }

    /**
     * Switch to a different tab.
     */
    private void switchToTab(Tab tab) {
        // Handle special tabs
        if (tab == Tab.QUIT) {
            this.client.scheduleStop();
            return;
        }

        if (tab == Tab.SINGLEPLAYER) {
            this.client.setScreen(new SelectWorldScreen(this));
            return;
        }

        if (tab == Tab.MULTIPLAYER) {
            this.client.setScreen(new MultiplayerScreen(this));
            return;
        }

        // Update tab bar selection
        tabBar.setSelectedTab(tab);

        // Get or create content for this tab
        TabContent content = getOrCreateContent(tab);
        ribbon.setTab(tab, content);
    }

    /**
     * Get existing content or create new content for a tab.
     */
    private TabContent getOrCreateContent(Tab tab) {
        int index = tab.ordinal();
        if (tabContents[index] == null) {
            tabContents[index] = createContentForTab(tab);
        }
        return tabContents[index];
    }

    /**
     * Factory method to create content for each tab type.
     */
    private TabContent createContentForTab(Tab tab) {
        return switch (tab) {
            case HOME -> new HomeTabContent();
            case SINGLEPLAYER -> new PlaceholderContent("Singleplayer", "Your worlds");
            case MULTIPLAYER -> new PlaceholderContent("Multiplayer", "Server browser");
            case MODS -> new PlaceholderContent("Mods", "Manage your mods");
            case VERSIONS -> new PlaceholderContent("Versions", "Switch game versions");
            case CLIPS -> new ClipsTabContent();
            case FRIENDS -> new PlaceholderContent("Friends", "Online friends");
            case ACCOUNTS -> new AccountsTabContent();
            case SETTINGS -> new SettingsTabContent();
            case QUIT -> new PlaceholderContent("Quit", "Exit game");
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render background (clips or fallback gradient/orbs)
        background.render(context, this.width, this.height);

        // Render tab bar
        tabBar.render(context, mouseX, mouseY, delta);

        // Render ribbon
        ribbon.render(context, mouseX, mouseY, delta);
    }

    /*? if MC_1_21_11 {*/
    @Override
    public boolean mouseClicked(Click click, boolean down) {
        // Pass both press and release to our handler
        if (handleMouseClick(click.x(), click.y(), click.button(), down)) return true;
        return super.mouseClicked(click, down);
    }
    /*?} else {*/
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleMouseClick(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }*/
    /*?}*/

    private boolean handleMouseClick(double mouseX, double mouseY, int button) {
        return handleMouseClick(mouseX, mouseY, button, false);
    }

    private boolean handleMouseClick(double mouseX, double mouseY, int button, boolean down) {
        // Check tab bar first (only on release for navigation)
        if (!down) {
            Tab clickedTab = tabBar.handleClick(mouseX, mouseY, button);
            if (clickedTab != null) {
                switchToTab(clickedTab);
                return true;
            }
        }

        // Pass to ribbon (handles both press and release for widgets)
        if (ribbon.handleClick(mouseX, mouseY, button, down)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (ribbon.handleScroll(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /*? if MC_1_21_11 {*/
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (ribbon.handleDrag(click.x(), click.y(), click.button(), deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }
    /*?} else {*/
    /*@Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (ribbon.handleDrag(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }*/
    /*?}*/

    /*? if MC_1_21_11 {*/
    @Override
    public boolean mouseReleased(Click click) {
        if (ribbon.handleRelease(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseReleased(click);
    }
    /*?} else {*/
    /*@Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (ribbon.handleRelease(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }*/
    /*?}*/

    /*? if MC_1_21_11 {*/
    @Override
    public boolean keyPressed(KeyInput input) {
        if (handleKeyPress(input.key())) return true;
        return super.keyPressed(input);
    }
    /*?} else {*/
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (handleKeyPress(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }*/
    /*?}*/

    private boolean handleKeyPress(int keyCode) {
        // Delegate to ribbon (passes scanCode=0, modifiers=0 - not used by tab contents)
        return ribbon.handleKeyPress(keyCode, 0, 0);
    }

    @Override
    public void tick() {
        super.tick();
        ribbon.tick();
    }

    @Override
    public void close() {
        background.close();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /**
     * Temporary placeholder content until real tab contents are implemented.
     */
    private static class PlaceholderContent implements TabContent {
        private final String title;
        private final String description;

        PlaceholderContent(String title, String description) {
            this.title = title;
            this.description = description;
        }

        @Override
        public void render(DrawContext context, int x, int y, int width, int height,
                          int mouseX, int mouseY, float delta) {
            var theme = MiracleTheme.current();
            var textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;

            // Draw title
            context.drawText(textRenderer, title, x, y, theme.textPrimary, false);

            // Draw description
            context.drawText(textRenderer, description, x, y + 14, theme.textSecondary, false);

            // Draw placeholder message
            context.drawText(textRenderer, "Coming soon...", x, y + 40, theme.textMuted, false);
        }
    }
}
