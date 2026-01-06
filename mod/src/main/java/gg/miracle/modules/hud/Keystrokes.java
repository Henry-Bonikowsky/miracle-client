package gg.miracle.modules.hud;

import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class Keystrokes extends Module {
    private final Setting<Integer> x = register(Setting.ofInt("X", "X position", 10, 0, 1920));
    private final Setting<Integer> y = register(Setting.ofInt("Y", "Y position", 100, 0, 1080));
    private final Setting<Float> scale = register(Setting.ofFloat("Scale", "Size multiplier", 1.0f, 0.5f, 2.0f));
    private final Setting<Boolean> showCPS = register(Setting.ofBool("Show CPS", "Show clicks per second", true));
    private final Setting<Boolean> showSpacebar = register(Setting.ofBool("Show Spacebar", "Show spacebar key", true));
    private final Setting<Boolean> showMouseButtons = register(Setting.ofBool("Show Mouse", "Show mouse buttons", true));

    // CPS tracking
    private long[] leftClicks = new long[20];
    private long[] rightClicks = new long[20];
    private int leftClickIndex = 0;
    private int rightClickIndex = 0;
    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;

    private static final int KEY_SIZE = 22;
    private static final int KEY_GAP = 2;
    private static final int BG_COLOR = 0x80000000;
    private static final int BG_PRESSED = 0x80FFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_PRESSED = 0xFF000000;

    public Keystrokes() {
        super("Keystrokes", "Displays WASD, space, and mouse clicks", Category.HUD);
    }

    public void render(DrawContext context) {
        if (!isEnabled() || mc.player == null) return;

        int baseX = x.get();
        int baseY = y.get();
        float s = scale.get();
        int keySize = (int)(KEY_SIZE * s);
        int gap = (int)(KEY_GAP * s);

        // Track clicks for CPS
        updateCPS();

        // W key (center top)
        drawKey(context, baseX + keySize + gap, baseY, keySize, "W", mc.options.forwardKey.isPressed());

        // A, S, D keys (row below)
        int row2Y = baseY + keySize + gap;
        drawKey(context, baseX, row2Y, keySize, "A", mc.options.leftKey.isPressed());
        drawKey(context, baseX + keySize + gap, row2Y, keySize, "S", mc.options.backKey.isPressed());
        drawKey(context, baseX + (keySize + gap) * 2, row2Y, keySize, "D", mc.options.rightKey.isPressed());

        int currentY = row2Y + keySize + gap;

        // Spacebar
        if (showSpacebar.get()) {
            int spaceWidth = keySize * 3 + gap * 2;
            drawKey(context, baseX, currentY, spaceWidth, keySize / 2, "---", mc.options.jumpKey.isPressed());
            currentY += keySize / 2 + gap;
        }

        // Mouse buttons
        if (showMouseButtons.get()) {
            int mouseWidth = (keySize * 3 + gap * 2 - gap) / 2;
            boolean lmbPressed = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rmbPressed = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

            String lmbText = showCPS.get() ? "LMB " + getLeftCPS() : "LMB";
            String rmbText = showCPS.get() ? "RMB " + getRightCPS() : "RMB";

            drawKey(context, baseX, currentY, mouseWidth, keySize, lmbText, lmbPressed);
            drawKey(context, baseX + mouseWidth + gap, currentY, mouseWidth, keySize, rmbText, rmbPressed);
        }
    }

    private void drawKey(DrawContext context, int x, int y, int size, String text, boolean pressed) {
        drawKey(context, x, y, size, size, text, pressed);
    }

    private void drawKey(DrawContext context, int x, int y, int width, int height, String text, boolean pressed) {
        // Background
        int bgColor = pressed ? BG_PRESSED : BG_COLOR;
        context.fill(x, y, x + width, y + height, bgColor);

        // Text
        int textColor = pressed ? TEXT_PRESSED : TEXT_COLOR;
        int textWidth = mc.textRenderer.getWidth(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        context.drawText(mc.textRenderer, text, textX, textY, textColor, false);
    }

    private void updateCPS() {
        long now = System.currentTimeMillis();
        boolean lmbPressed = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rmbPressed = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        // Track left clicks
        if (lmbPressed && !wasLeftPressed) {
            leftClicks[leftClickIndex] = now;
            leftClickIndex = (leftClickIndex + 1) % leftClicks.length;
        }
        wasLeftPressed = lmbPressed;

        // Track right clicks
        if (rmbPressed && !wasRightPressed) {
            rightClicks[rightClickIndex] = now;
            rightClickIndex = (rightClickIndex + 1) % rightClicks.length;
        }
        wasRightPressed = rmbPressed;
    }

    private int getLeftCPS() {
        return countRecentClicks(leftClicks);
    }

    private int getRightCPS() {
        return countRecentClicks(rightClicks);
    }

    private int countRecentClicks(long[] clicks) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (long click : clicks) {
            if (now - click < 1000) {
                count++;
            }
        }
        return count;
    }
}
