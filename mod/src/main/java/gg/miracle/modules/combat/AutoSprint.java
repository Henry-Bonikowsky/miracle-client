package gg.miracle.modules.combat;

import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;

public class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", "Automatically sprints when moving forward", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        //? if MC_1_21_5 || MC_1_21_8 || MC_1_21_11 {
        if (mc.player.input.hasForwardMovement() && !mc.player.isSprinting() && !mc.player.isUsingItem()) {
        //?} else {
        /*if (mc.player.input.movementForward > 0 && !mc.player.isSprinting() && !mc.player.isUsingItem()) {*/
        //?}
            mc.player.setSprinting(true);
        }
    }
}
