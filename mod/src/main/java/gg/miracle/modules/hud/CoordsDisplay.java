package gg.miracle.modules.hud;

import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import net.minecraft.client.gui.DrawContext;

public class CoordsDisplay extends Module {
    private final Setting<Integer> x = register(Setting.ofInt("X", "X position", 5, 0, 1920));
    private final Setting<Integer> y = register(Setting.ofInt("Y", "Y position", 20, 0, 1080));
    private final Setting<Integer> color = register(Setting.of("Color", "Text color", 0xFFFFFF));

    public CoordsDisplay() {
        super("Coords", "Displays current coordinates", Category.HUD);
    }

    public void render(DrawContext context) {
        if (!isEnabled() || mc.player == null) return;

        String coords = String.format("XYZ: %.1f / %.1f / %.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ());
        context.drawTextWithShadow(mc.textRenderer, coords, x.get(), y.get(), color.get());
    }
}
