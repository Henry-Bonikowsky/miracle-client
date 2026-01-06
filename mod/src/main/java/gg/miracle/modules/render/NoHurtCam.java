package gg.miracle.modules.render;

import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;

public class NoHurtCam extends Module {
    public NoHurtCam() {
        super("NoHurtCam", "Disables the screen shake when taking damage", Category.RENDER);
    }

    // The actual implementation is in GameRendererMixin
}
