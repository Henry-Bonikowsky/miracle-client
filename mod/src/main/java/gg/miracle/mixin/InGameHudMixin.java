package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import gg.miracle.modules.hud.ArmorDisplay;
import gg.miracle.modules.hud.CoordsDisplay;
import gg.miracle.modules.hud.FPSDisplay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // Render HUD modules
        var moduleManager = MiracleClient.getInstance().getModuleManager();

        FPSDisplay fpsDisplay = moduleManager.get(FPSDisplay.class);
        if (fpsDisplay != null) {
            fpsDisplay.render(context);
        }

        CoordsDisplay coordsDisplay = moduleManager.get(CoordsDisplay.class);
        if (coordsDisplay != null) {
            coordsDisplay.render(context);
        }

        ArmorDisplay armorDisplay = moduleManager.get(ArmorDisplay.class);
        if (armorDisplay != null) {
            armorDisplay.render(context);
        }
    }
}
