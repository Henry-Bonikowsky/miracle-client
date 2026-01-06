package gg.miracle.modules.hud;

import gg.miracle.api.config.Setting;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import net.minecraft.client.gui.DrawContext;
//? if MC_1_21_5 || MC_1_21_8 || MC_1_21_11 {
import net.minecraft.entity.EquipmentSlot;
//?}
import net.minecraft.item.ItemStack;

public class ArmorDisplay extends Module {
    private final Setting<Integer> x = register(Setting.ofInt("X", "X position", 5, 0, 1920));
    private final Setting<Integer> y = register(Setting.ofInt("Y", "Y position", 50, 0, 1080));

    //? if MC_1_21_5 || MC_1_21_8 || MC_1_21_11 {
    // Armor slots in display order (head to feet)
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    //?}

    public ArmorDisplay() {
        super("Armor", "Displays armor durability", Category.HUD);
    }

    public void render(DrawContext context) {
        if (!isEnabled() || mc.player == null) return;

        int yOffset = 0;
        //? if MC_1_21_5 || MC_1_21_8 || MC_1_21_11 {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = mc.player.getEquippedStack(slot);
        //?} else {
        /*for (ItemStack stack : mc.player.getArmorItems()) {*/
        //?}
            if (!stack.isEmpty()) {
                context.drawItem(stack, x.get(), y.get() + yOffset);

                if (stack.isDamageable()) {
                    int durability = stack.getMaxDamage() - stack.getDamage();
                    int maxDurability = stack.getMaxDamage();
                    float percent = (float) durability / maxDurability;

                    int color = percent > 0.5f ? 0x00FF00 : percent > 0.25f ? 0xFFFF00 : 0xFF0000;

                    context.drawTextWithShadow(mc.textRenderer,
                            durability + "/" + maxDurability,
                            x.get() + 20, y.get() + yOffset + 4, color);
                }

                yOffset += 18;
            }
        }
    }
}
