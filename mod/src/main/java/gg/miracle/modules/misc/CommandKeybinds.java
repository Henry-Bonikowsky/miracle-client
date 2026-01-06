package gg.miracle.modules.misc;

import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class CommandKeybinds extends Module {
    // Pre-configured keybinds (can be expanded with a proper settings UI later)
    private final Setting<String> bind1Key = register(Setting.ofString("Bind 1 Key", "Key name (F4, G, etc.)", ""));
    private final Setting<String> bind1Cmd = register(Setting.ofString("Bind 1 Command", "Command to run", ""));
    private final Setting<String> bind2Key = register(Setting.ofString("Bind 2 Key", "Key name", ""));
    private final Setting<String> bind2Cmd = register(Setting.ofString("Bind 2 Command", "Command to run", ""));
    private final Setting<String> bind3Key = register(Setting.ofString("Bind 3 Key", "Key name", ""));
    private final Setting<String> bind3Cmd = register(Setting.ofString("Bind 3 Command", "Command to run", ""));
    private final Setting<String> bind4Key = register(Setting.ofString("Bind 4 Key", "Key name", ""));
    private final Setting<String> bind4Cmd = register(Setting.ofString("Bind 4 Command", "Command to run", ""));

    private final Map<Integer, Boolean> keyStates = new HashMap<>();

    public CommandKeybinds() {
        super("CommandKeybinds", "Bind hotkeys to chat commands", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.currentScreen != null) return;

        // Check each configured bind
        checkBind(bind1Key.get(), bind1Cmd.get());
        checkBind(bind2Key.get(), bind2Cmd.get());
        checkBind(bind3Key.get(), bind3Cmd.get());
        checkBind(bind4Key.get(), bind4Cmd.get());
    }

    private void checkBind(String keyName, String command) {
        if (keyName == null || keyName.isEmpty() || command == null || command.isEmpty()) {
            return;
        }

        int keyCode = getKeyCode(keyName);
        if (keyCode == -1) return;

        boolean isPressed = GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        boolean wasPressed = keyStates.getOrDefault(keyCode, false);

        if (isPressed && !wasPressed) {
            // Key was just pressed - execute command
            executeCommand(command);
        }

        keyStates.put(keyCode, isPressed);
    }

    private void executeCommand(String command) {
        if (mc.player == null) return;

        // Send as chat message (commands start with /)
        if (command.startsWith("/")) {
            mc.player.networkHandler.sendChatCommand(command.substring(1));
        } else {
            mc.player.networkHandler.sendChatMessage(command);
        }
    }

    private int getKeyCode(String keyName) {
        if (keyName == null || keyName.isEmpty()) return -1;

        String key = keyName.toUpperCase().trim();

        // Function keys
        if (key.startsWith("F") && key.length() > 1) {
            try {
                int num = Integer.parseInt(key.substring(1));
                if (num >= 1 && num <= 25) {
                    return GLFW.GLFW_KEY_F1 + num - 1;
                }
            } catch (NumberFormatException ignored) {}
        }

        // Letter keys
        if (key.length() == 1) {
            char c = key.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return GLFW.GLFW_KEY_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return GLFW.GLFW_KEY_0 + (c - '0');
            }
        }

        // Special keys
        return switch (key) {
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "ENTER", "RETURN" -> GLFW.GLFW_KEY_ENTER;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "INSERT" -> GLFW.GLFW_KEY_INSERT;
            case "DELETE" -> GLFW.GLFW_KEY_DELETE;
            case "HOME" -> GLFW.GLFW_KEY_HOME;
            case "END" -> GLFW.GLFW_KEY_END;
            case "PAGEUP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGEDOWN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            case "LSHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LCTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            default -> -1;
        };
    }

    @Override
    protected void onDisable() {
        keyStates.clear();
    }
}
