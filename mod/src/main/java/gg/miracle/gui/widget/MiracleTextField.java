package gg.miracle.gui.widget;

import gg.miracle.gui.UISounds;
import gg.miracle.gui.render.GradientRenderer;
import gg.miracle.gui.theme.MiracleTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Text input field with placeholder, validation, and character limit.
 */
public class MiracleTextField extends MiracleWidget {

    private String label = "";
    private String text = "";
    private String placeholder = "";
    private int maxLength = 32;
    private boolean focused = false;
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private long lastCursorBlink = 0;
    private boolean password = false;

    private Predicate<String> validator = null;
    private Consumer<String> onChange = null;
    private Runnable onEnter = null;

    private static final int PADDING = 8;
    private static final int CURSOR_BLINK_MS = 530;

    public MiracleTextField() {
        super();
    }

    // Builder methods
    public MiracleTextField label(String label) {
        this.label = label != null ? label : "";
        return this;
    }

    public MiracleTextField text(String text) {
        this.text = text != null ? text : "";
        this.cursorPosition = Math.min(cursorPosition, this.text.length());
        return this;
    }

    public MiracleTextField placeholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        return this;
    }

    public MiracleTextField maxLength(int maxLength) {
        this.maxLength = Math.max(1, maxLength);
        return this;
    }

    public MiracleTextField password(boolean password) {
        this.password = password;
        return this;
    }

    public MiracleTextField validator(Predicate<String> validator) {
        this.validator = validator;
        return this;
    }

    public MiracleTextField onChange(Consumer<String> onChange) {
        this.onChange = onChange;
        return this;
    }

    public MiracleTextField onEnter(Runnable onEnter) {
        this.onEnter = onEnter;
        return this;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (text == null) text = "";
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
        }
        this.text = text;
        this.cursorPosition = Math.min(cursorPosition, text.length());
        if (onChange != null) {
            onChange.accept(this.text);
        }
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        if (this.focused != focused) {
            this.focused = focused;
            if (focused) {
                lastCursorBlink = System.currentTimeMillis();
            }
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MiracleTheme.Theme theme = theme();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float hover = getHoverProgress();

        // Calculate field area
        int fieldWidth = 200;
        int fieldX = x + width - fieldWidth;
        int fieldY = y;

        // Draw label on left
        if (!label.isEmpty()) {
            int labelY = y + (height - textRenderer.fontHeight) / 2;
            int labelColor = MiracleTheme.lerpColor(theme.textSecondary, theme.textPrimary, hover);
            context.drawText(textRenderer, label, x, labelY, labelColor, false);
        }

        // Validate current text
        boolean isValid = validator == null || validator.test(text);

        // Background with focus/validation states
        int bgColor = focused
            ? MiracleTheme.withAlpha(theme.accent500, 0x20)
            : MiracleTheme.withAlpha(theme.glassBg, 0xFF);

        GradientRenderer.fillRoundedRect(context, fieldX, fieldY, fieldWidth, height, bgColor, MiracleTheme.RADIUS_MEDIUM);

        // Border - changes color based on focus/validation
        float press = getPressProgress();
        int borderColor;
        int borderWidth = focused ? 2 : 1;

        if (!isValid && !text.isEmpty()) {
            borderColor = 0xFFDC2626; // Red for invalid
        } else if (focused) {
            borderColor = theme.accent500; // Accent when focused
        } else {
            borderColor = MiracleTheme.lerpColor(theme.glassBorder, theme.accent500, Math.max(hover, press));
        }

        // Subtle press shadow on border
        if (press > 0 && !focused) {
            int pressShadow = MiracleTheme.withAlpha(theme.accent500, (int)(0x30 * press));
            GradientRenderer.drawBorder(context, fieldX - 1, fieldY - 1, fieldWidth + 2, height + 2, pressShadow, 1);
        }

        GradientRenderer.drawBorder(context, fieldX, fieldY, fieldWidth, height, borderColor, borderWidth);

        // Render text content with clipping
        int textX = fieldX + PADDING;
        int textY = fieldY + (height - textRenderer.fontHeight) / 2;
        int textAreaWidth = fieldWidth - PADDING * 2;

        context.enableScissor(fieldX, fieldY, fieldX + fieldWidth, fieldY + height);

        if (text.isEmpty() && !focused) {
            // Placeholder text
            context.drawText(textRenderer, placeholder, textX, textY, theme.textMuted, false);
        } else {
            // Actual text (masked if password)
            String displayText = password ? "*".repeat(text.length()) : text;

            // Selection highlight
            if (focused && selectionStart >= 0 && selectionEnd > selectionStart) {
                int selStart = Math.min(selectionStart, selectionEnd);
                int selEnd = Math.max(selectionStart, selectionEnd);
                String beforeSel = displayText.substring(0, selStart);
                String selection = displayText.substring(selStart, selEnd);

                int selX = textX + textRenderer.getWidth(beforeSel);
                int selWidth = textRenderer.getWidth(selection);
                context.fill(selX, textY - 1, selX + selWidth, textY + textRenderer.fontHeight + 1,
                           MiracleTheme.withAlpha(theme.accent500, 0x60));
            }

            // Text
            context.drawText(textRenderer, displayText, textX, textY, theme.textPrimary, false);

            // Cursor (blinking when focused)
            if (focused) {
                long now = System.currentTimeMillis();
                boolean showCursor = ((now - lastCursorBlink) % (CURSOR_BLINK_MS * 2)) < CURSOR_BLINK_MS;

                if (showCursor) {
                    String beforeCursor = displayText.substring(0, Math.min(cursorPosition, displayText.length()));
                    int cursorX = textX + textRenderer.getWidth(beforeCursor);
                    context.fill(cursorX, textY - 1, cursorX + 1, textY + textRenderer.fontHeight + 1, theme.accent400);
                }
            }
        }

        context.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, boolean down) {
        if (!visible || !enabled) return false;
        if (button != 0) return false;

        int fieldWidth = 200;
        int fieldX = x + width - fieldWidth;

        if (!down) {
            // Click inside field
            if (mouseX >= fieldX && mouseX <= fieldX + fieldWidth &&
                mouseY >= y && mouseY <= y + height) {

                if (!focused) {
                    focused = true;
                    lastCursorBlink = System.currentTimeMillis();
                    UISounds.click();
                }

                // Calculate cursor position from click
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                String displayText = password ? "*".repeat(text.length()) : text;
                int textX = fieldX + PADDING;
                int clickOffset = (int)(mouseX - textX);

                cursorPosition = 0;
                for (int i = 0; i <= displayText.length(); i++) {
                    String substr = displayText.substring(0, i);
                    int width = textRenderer.getWidth(substr);
                    if (width > clickOffset) {
                        cursorPosition = Math.max(0, i - 1);
                        break;
                    }
                    cursorPosition = i;
                }

                // Reset selection
                selectionStart = -1;
                selectionEnd = -1;

                return true;
            } else {
                // Click outside - unfocus
                if (focused) {
                    focused = false;
                    return true;
                }
            }
        }

        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        // Reset cursor blink on any key
        lastCursorBlink = System.currentTimeMillis();

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (onEnter != null) {
                    onEnter.run();
                }
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition > 0) {
                    text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                    cursorPosition--;
                    notifyChange();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition < text.length()) {
                    text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
                    notifyChange();
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (cursorPosition > 0) {
                    if (shift) {
                        if (!hasSelection()) selectionStart = cursorPosition;
                        cursorPosition--;
                        selectionEnd = cursorPosition;
                    } else {
                        cursorPosition--;
                        selectionStart = -1;
                        selectionEnd = -1;
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (cursorPosition < text.length()) {
                    if (shift) {
                        if (!hasSelection()) selectionStart = cursorPosition;
                        cursorPosition++;
                        selectionEnd = cursorPosition;
                    } else {
                        cursorPosition++;
                        selectionStart = -1;
                        selectionEnd = -1;
                    }
                }
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                cursorPosition = 0;
                if (!shift) {
                    selectionStart = -1;
                    selectionEnd = -1;
                }
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                cursorPosition = text.length();
                if (!shift) {
                    selectionStart = -1;
                    selectionEnd = -1;
                }
                return true;
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) {
                    // Select all
                    selectionStart = 0;
                    selectionEnd = text.length();
                    cursorPosition = text.length();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_C -> {
                if (ctrl && hasSelection()) {
                    // Copy
                    String selected = getSelectedText();
                    MinecraftClient.getInstance().keyboard.setClipboard(selected);
                    return true;
                }
            }
            case GLFW.GLFW_KEY_X -> {
                if (ctrl && hasSelection()) {
                    // Cut
                    String selected = getSelectedText();
                    MinecraftClient.getInstance().keyboard.setClipboard(selected);
                    deleteSelection();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) {
                    // Paste
                    String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
                    if (clipboard != null && !clipboard.isEmpty()) {
                        insertText(clipboard);
                    }
                    return true;
                }
            }
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;

        // Filter invalid characters
        if (chr < 32 || chr == 127) return false;

        insertText(String.valueOf(chr));
        return true;
    }

    private void insertText(String insert) {
        if (hasSelection()) {
            deleteSelection();
        }

        String newText = text.substring(0, cursorPosition) + insert + text.substring(cursorPosition);
        if (newText.length() <= maxLength) {
            text = newText;
            cursorPosition += insert.length();
            notifyChange();
        }
    }

    private boolean hasSelection() {
        return selectionStart >= 0 && selectionEnd > selectionStart;
    }

    private String getSelectedText() {
        if (!hasSelection()) return "";
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        return text.substring(start, end);
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        text = text.substring(0, start) + text.substring(end);
        cursorPosition = start;
        selectionStart = -1;
        selectionEnd = -1;
        notifyChange();
    }

    private void notifyChange() {
        if (onChange != null) {
            onChange.accept(text);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width &&
               mouseY >= y && mouseY < y + height;
    }
}
