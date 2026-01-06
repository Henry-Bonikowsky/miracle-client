package gg.miracle.modules.render;

import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;

public class Fullbright extends Module {
    private double previousGamma;

    public Fullbright() {
        super("Fullbright", "Makes everything fully bright", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        previousGamma = mc.options.getGamma().getValue();
        mc.options.getGamma().setValue(16.0);
    }

    @Override
    protected void onDisable() {
        mc.options.getGamma().setValue(previousGamma);
    }
}
