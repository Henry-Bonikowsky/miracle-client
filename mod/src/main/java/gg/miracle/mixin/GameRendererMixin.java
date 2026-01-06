package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import gg.miracle.modules.render.NoHurtCam;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoHurtCam noHurtCam = MiracleClient.getInstance().getModuleManager().get(NoHurtCam.class);
        if (noHurtCam != null && noHurtCam.isEnabled()) {
            ci.cancel();
        }
    }
}
