package gg.miracle.gui.menu.tabs;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.miracle.MiracleClient;
import gg.miracle.gui.menu.TabContent;
import gg.miracle.gui.render.GradientRenderer;
import gg.miracle.gui.theme.MiracleTheme;
import gg.miracle.gui.widget.MiracleButton;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Accounts tab content showing current account and account switching/adding.
 */
public class AccountsTabContent implements TabContent {

    private static final int SECTION_SPACING = 20;
    private static final int AVATAR_SIZE = 48;
    private static final int ACCOUNT_ITEM_HEIGHT = 40;

    private final MiracleButton switchButton = new MiracleButton()
        .label("Switch Account")
        .onClick(this::toggleAccountPicker);

    private final MiracleButton addAccountButton = new MiracleButton()
        .label("Add Account")
        .onClick(this::startAddAccount);

    private final MiracleButton openBrowserButton = new MiracleButton()
        .label("Open Browser")
        .onClick(this::openVerificationUrl);

    private final MiracleButton cancelAddButton = new MiracleButton()
        .label("Cancel")
        .onClick(this::cancelAddAccount);

    private final MiracleButton copyCodeButton = new MiracleButton()
        .label("Copy Code")
        .onClick(this::copyCodeToClipboard);

    // Copy feedback state
    private boolean codeCopied = false;
    private long codeCopiedTime = 0;
    private static final long COPIED_DISPLAY_TIME = 2000;

    // Account picker state
    private boolean showingPicker = false;
    private List<AccountInfo> availableAccounts = new ArrayList<>();
    private int hoveredAccountIndex = -1;

    // Add account state
    private boolean addingAccount = false;
    private String deviceCode = null;
    private String userCode = null;
    private String verificationUrl = null;
    private String authStatus = null;
    private long lastPollTime = 0;
    private static final long POLL_INTERVAL = 1000; // 1 second

    // Account info from launcher
    private record AccountInfo(String id, String name, boolean isActive) {}

    @Override
    public void init() {
        loadAccountsFromFile();
    }

    @Override
    public void tick() {
        // Poll for auth response when adding account
        if (addingAccount && System.currentTimeMillis() - lastPollTime > POLL_INTERVAL) {
            lastPollTime = System.currentTimeMillis();
            pollAuthResponse();
        }

        // Reset copied state after display time
        if (codeCopied && System.currentTimeMillis() - codeCopiedTime > COPIED_DISPLAY_TIME) {
            codeCopied = false;
        }
    }

    private void loadAccountsFromFile() {
        availableAccounts.clear();
        Path accountsFile = getConfigDir().resolve("accounts.json");

        if (!Files.exists(accountsFile)) {
            MiracleClient.LOGGER.info("[Accounts] No accounts file found");
            return;
        }

        try {
            String json = Files.readString(accountsFile);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (var elem : arr) {
                JsonObject obj = elem.getAsJsonObject();
                availableAccounts.add(new AccountInfo(
                    obj.get("id").getAsString(),
                    obj.get("name").getAsString(),
                    obj.has("is_active") && obj.get("is_active").getAsBoolean()
                ));
            }
            MiracleClient.LOGGER.info("[Accounts] Loaded {} accounts", availableAccounts.size());
        } catch (Exception e) {
            MiracleClient.LOGGER.error("[Accounts] Failed to load accounts", e);
        }
    }

    private Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("miracle");
    }

    private void toggleAccountPicker() {
        showingPicker = !showingPicker;
        if (showingPicker) {
            loadAccountsFromFile();
        }
    }

    private void startAddAccount() {
        addingAccount = true;
        authStatus = "Requesting...";
        userCode = null;
        verificationUrl = null;

        // Write auth request for launcher
        Path requestFile = getConfigDir().resolve("auth_request.json");
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("action", "start_device_flow");
            Files.writeString(requestFile, new Gson().toJson(obj));
            MiracleClient.LOGGER.info("[Accounts] Auth request written");
        } catch (IOException e) {
            MiracleClient.LOGGER.error("[Accounts] Failed to write auth request", e);
            authStatus = "Error: " + e.getMessage();
        }
    }

    private void pollAuthResponse() {
        Path responseFile = getConfigDir().resolve("auth_response.json");
        if (!Files.exists(responseFile)) {
            return;
        }

        try {
            String json = Files.readString(responseFile);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String status = obj.get("status").getAsString();

            if ("pending".equals(status)) {
                userCode = obj.get("user_code").getAsString();
                verificationUrl = obj.get("verification_uri").getAsString();
                authStatus = "Enter code in browser";
            } else if ("success".equals(status)) {
                // Auth completed!
                JsonObject account = obj.getAsJsonObject("account");
                String name = account.get("name").getAsString();
                authStatus = "Added: " + name;
                addingAccount = false;

                // Delete response file
                Files.deleteIfExists(responseFile);

                // Reload accounts (launcher should have updated accounts.json)
                // Give it a moment to write
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        loadAccountsFromFile();
                    } catch (InterruptedException ignored) {}
                }).start();
            } else if ("error".equals(status)) {
                authStatus = "Error: " + obj.get("error").getAsString();
                addingAccount = false;
                Files.deleteIfExists(responseFile);
            }
        } catch (Exception e) {
            MiracleClient.LOGGER.error("[Accounts] Failed to read auth response", e);
        }
    }

    private void openVerificationUrl() {
        if (verificationUrl != null) {
            try {
                Util.getOperatingSystem().open(URI.create(verificationUrl));
            } catch (Exception e) {
                MiracleClient.LOGGER.error("[Accounts] Failed to open URL", e);
            }
        }
    }

    private void copyCodeToClipboard() {
        if (userCode != null) {
            MinecraftClient.getInstance().keyboard.setClipboard(userCode);
            codeCopied = true;
            codeCopiedTime = System.currentTimeMillis();
            MiracleClient.LOGGER.info("[Accounts] Code copied to clipboard: {}", userCode);
        }
    }

    private void cancelAddAccount() {
        addingAccount = false;
        authStatus = null;
        userCode = null;
        verificationUrl = null;

        // Clean up files
        try {
            Files.deleteIfExists(getConfigDir().resolve("auth_request.json"));
            Files.deleteIfExists(getConfigDir().resolve("auth_response.json"));
        } catch (IOException ignored) {}
    }

    private void switchToAccount(AccountInfo account) {
        if (account.isActive) {
            toggleAccountPicker();
            return;
        }

        Path switchFile = getConfigDir().resolve("switch_account.json");
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("account_id", account.id);
            Files.writeString(switchFile, new Gson().toJson(obj));
            MiracleClient.LOGGER.info("[Accounts] Switch requested to: {}", account.name);
            MinecraftClient.getInstance().scheduleStop();
        } catch (IOException e) {
            MiracleClient.LOGGER.error("[Accounts] Failed to write switch request", e);
        }
    }

    @Override
    public void render(DrawContext context, int x, int y, int width, int height,
                      int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = MiracleTheme.current();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        MinecraftClient client = MinecraftClient.getInstance();

        int currentY = y;

        // === Current Account Section ===
        context.drawText(textRenderer, "Current Account", x, currentY, theme.accent400, false);
        currentY += 16;

        int cardHeight = AVATAR_SIZE + 16;
        GradientRenderer.fillGlassPanelRounded(context, x, currentY, width, cardHeight, 8);

        int avatarX = x + 8;
        int avatarY = currentY + 8;
        drawAvatarPlaceholder(context, avatarX, avatarY, theme);

        int textX = avatarX + AVATAR_SIZE + 12;
        int textY = currentY + 16;

        String username = client.getSession().getUsername();
        context.drawText(textRenderer, username, textX, textY, theme.textPrimary, false);

        String accountType = getAccountType(client);
        int badgeY = textY + 16;
        int badgeColor = accountType.equals("Microsoft") ? 0xFF00A4EF : theme.textMuted;
        context.drawText(textRenderer, accountType, textX, badgeY, badgeColor, false);

        currentY += cardHeight + SECTION_SPACING;

        // === Add Account Flow ===
        if (addingAccount) {
            context.drawText(textRenderer, "Add Account", x, currentY, theme.accent400, false);
            currentY += 16;

            // Show device code info
            GradientRenderer.fillGlassPanelRounded(context, x, currentY, width, 80, 8);

            if (userCode != null) {
                // Show code prominently
                context.drawText(textRenderer, "Go to:", x + 12, currentY + 12, theme.textSecondary, false);
                context.drawText(textRenderer, verificationUrl, x + 12, currentY + 26, theme.accent400, false);

                context.drawText(textRenderer, "Enter code:", x + 12, currentY + 46, theme.textSecondary, false);
                // Draw code larger/highlighted
                context.drawText(textRenderer, userCode, x + 80, currentY + 46, theme.textPrimary, false);

                // Show copied feedback
                if (codeCopied) {
                    context.drawText(textRenderer, "Copied!", x + 12, currentY + 62, 0xFF00FF00, false);
                }
            } else {
                context.drawText(textRenderer, authStatus != null ? authStatus : "Waiting...",
                    x + 12, currentY + 35, theme.textSecondary, false);
            }

            currentY += 88;

            // Copy code button (show before browser button when code is available)
            if (userCode != null) {
                copyCodeButton.label(codeCopied ? "Copied!" : "Copy Code");
                copyCodeButton.setBounds(x, currentY, width, 32);
                copyCodeButton.render(context, mouseX, mouseY, delta);
                currentY += 40;
            }

            // Open browser button
            if (verificationUrl != null) {
                openBrowserButton.setBounds(x, currentY, width, 32);
                openBrowserButton.render(context, mouseX, mouseY, delta);
                currentY += 40;
            }

            // Cancel button
            cancelAddButton.setBounds(x, currentY, width, 32);
            cancelAddButton.render(context, mouseX, mouseY, delta);
            return;
        }

        // === Account Picker ===
        if (showingPicker && availableAccounts.size() > 1) {
            context.drawText(textRenderer, "Select Account", x, currentY, theme.accent400, false);
            currentY += 16;

            hoveredAccountIndex = -1;
            int accountIndex = 0;
            for (AccountInfo account : availableAccounts) {
                int itemY = currentY + (accountIndex * (ACCOUNT_ITEM_HEIGHT + 4));

                boolean hovered = mouseX >= x && mouseX < x + width &&
                                 mouseY >= itemY && mouseY < itemY + ACCOUNT_ITEM_HEIGHT;
                if (hovered) {
                    hoveredAccountIndex = accountIndex;
                }

                int bgColor = account.isActive ? MiracleTheme.withAlpha(theme.accent500, 0x40) :
                             hovered ? MiracleTheme.withAlpha(theme.surfaceSecondary, 0x80) :
                             MiracleTheme.withAlpha(theme.surfaceSecondary, 0x40);
                context.fill(x, itemY, x + width, itemY + ACCOUNT_ITEM_HEIGHT, bgColor);

                if (account.isActive) {
                    context.fill(x, itemY + 4, x + 3, itemY + ACCOUNT_ITEM_HEIGHT - 4, theme.accent500);
                }

                int nameColor = account.isActive ? theme.accent400 : theme.textPrimary;
                context.drawText(textRenderer, account.name, x + 12, itemY + 8, nameColor, false);

                String status = account.isActive ? "Currently playing" : "Click to switch";
                int statusColor = account.isActive ? theme.textSecondary : theme.textMuted;
                context.drawText(textRenderer, status, x + 12, itemY + 22, statusColor, false);

                accountIndex++;
            }

            currentY += (availableAccounts.size() * (ACCOUNT_ITEM_HEIGHT + 4)) + 8;

            // Cancel button when picking
            switchButton.label("Cancel");
            switchButton.setBounds(x, currentY, width, 32);
            switchButton.render(context, mouseX, mouseY, delta);
            return;
        }

        // === Normal Actions ===
        context.drawText(textRenderer, "Actions", x, currentY, theme.accent400, false);
        currentY += 16;

        // Switch button (only if multiple accounts)
        if (availableAccounts.size() > 1) {
            switchButton.label("Switch Account");
            switchButton.setBounds(x, currentY, width, 32);
            switchButton.render(context, mouseX, mouseY, delta);
            currentY += 40;
        }

        // Add account button
        addAccountButton.setBounds(x, currentY, width, 32);
        addAccountButton.render(context, mouseX, mouseY, delta);
        currentY += 40;

        // Info
        String info = availableAccounts.size() + " account" + (availableAccounts.size() != 1 ? "s" : "") + " available";
        context.drawText(textRenderer, info, x, currentY, theme.textMuted, false);
    }

    private void drawAvatarPlaceholder(DrawContext context, int x, int y, MiracleTheme.Theme theme) {
        context.fill(x, y, x + AVATAR_SIZE, y + AVATAR_SIZE, theme.surfaceSecondary);
        context.fill(x + 1, y + 1, x + AVATAR_SIZE - 1, y + AVATAR_SIZE - 1, theme.glassBg);

        int faceSize = AVATAR_SIZE - 8;
        int faceX = x + 4;
        int faceY = y + 4;
        context.fill(faceX, faceY, faceX + faceSize, faceY + faceSize, theme.accent800);
    }

    private String getAccountType(MinecraftClient client) {
        var session = client.getSession();
        if (session.getUuidOrNull() != null) {
            return "Microsoft";
        }
        return "Offline";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (addingAccount) {
            if (userCode != null && copyCodeButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (verificationUrl != null && openBrowserButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (cancelAddButton.mouseClicked(mouseX, mouseY, button)) return true;
            return false;
        }

        if (showingPicker && hoveredAccountIndex >= 0 && hoveredAccountIndex < availableAccounts.size()) {
            switchToAccount(availableAccounts.get(hoveredAccountIndex));
            return true;
        }

        if (showingPicker) {
            if (switchButton.mouseClicked(mouseX, mouseY, button)) return true;
        } else {
            if (availableAccounts.size() > 1 && switchButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (addAccountButton.mouseClicked(mouseX, mouseY, button)) return true;
        }

        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean down) {
        if (button != 0) return false;

        if (addingAccount) {
            if (userCode != null && copyCodeButton.mouseClicked(mouseX, mouseY, button, down)) return true;
            if (verificationUrl != null && openBrowserButton.mouseClicked(mouseX, mouseY, button, down)) return true;
            if (cancelAddButton.mouseClicked(mouseX, mouseY, button, down)) return true;
            return false;
        }

        if (!down && showingPicker && hoveredAccountIndex >= 0 && hoveredAccountIndex < availableAccounts.size()) {
            switchToAccount(availableAccounts.get(hoveredAccountIndex));
            return true;
        }

        if (showingPicker) {
            if (switchButton.mouseClicked(mouseX, mouseY, button, down)) return true;
        } else {
            if (availableAccounts.size() > 1 && switchButton.mouseClicked(mouseX, mouseY, button, down)) return true;
            if (addAccountButton.mouseClicked(mouseX, mouseY, button, down)) return true;
        }

        return false;
    }
}
