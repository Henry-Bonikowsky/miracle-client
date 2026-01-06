package gg.miracle.modules.hud;

import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public class ReachDisplay extends Module {
    private final Setting<Integer> x = register(Setting.ofInt("X", "X position", 10, 0, 1920));
    private final Setting<Integer> y = register(Setting.ofInt("Y", "Y position", 200, 0, 1080));
    private final Setting<Integer> decimals = register(Setting.ofInt("Decimals", "Decimal places", 2, 0, 4));
    private final Setting<Integer> displayTime = register(Setting.ofInt("Display Time", "Seconds to show reach", 3, 1, 10));
    private final Setting<Integer> textColor = register(Setting.ofInt("Color", "Text color (hex)", 0x55FFFF, 0, 0xFFFFFF));

    private double lastReach = 0;
    private long lastHitTime = 0;
    private boolean wasAttacking = false;

    public ReachDisplay() {
        super("ReachDisplay", "Shows distance to hit target", Category.HUD);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Check for attack
        boolean isAttacking = mc.options.attackKey.isPressed();

        if (isAttacking && !wasAttacking) {
            // Player just started attacking
            HitResult hitResult = mc.crosshairTarget;

            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hitResult;
                Entity target = entityHit.getEntity();

                // Calculate distance from player eyes to target center
                // Use individual coordinate methods for version compatibility
                Vec3d targetCenter = new Vec3d(target.getX(), target.getY() + target.getHeight() / 2, target.getZ());
                double distance = mc.player.getEyePos().distanceTo(targetCenter);
                lastReach = distance;
                lastHitTime = System.currentTimeMillis();
            }
        }

        wasAttacking = isAttacking;
    }

    public void render(DrawContext context) {
        if (!isEnabled() || mc.player == null) return;

        long now = System.currentTimeMillis();
        long elapsed = now - lastHitTime;
        int displayTimeMs = displayTime.get() * 1000;

        if (elapsed < displayTimeMs && lastReach > 0) {
            // Format reach with configured decimals
            String format = "%." + decimals.get() + "f";
            String reachText = String.format(format + " blocks", lastReach);

            // Fade out effect
            float alpha = 1.0f;
            if (elapsed > displayTimeMs - 500) {
                alpha = (displayTimeMs - elapsed) / 500f;
            }

            int color = textColor.get() | ((int)(alpha * 255) << 24);
            context.drawTextWithShadow(mc.textRenderer, reachText, x.get(), y.get(), color);
        }
    }

    @Override
    protected void onDisable() {
        lastReach = 0;
        lastHitTime = 0;
    }
}
