package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import gg.miracle.modules.render.Freelook;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        Freelook freelook = MiracleClient.getInstance().getModuleManager().get(Freelook.class);
        if (freelook != null && freelook.isFreelookActive()) {
            // Update camera rotation instead of player rotation
            float sensitivity = 0.15f;
            freelook.setCameraYaw(freelook.getCameraYaw() + (float)(cursorDeltaX * sensitivity));
            float newPitch = freelook.getCameraPitch() + (float)(cursorDeltaY * sensitivity);
            freelook.setCameraPitch(MathHelper.clamp(newPitch, -90.0f, 90.0f));

            // Cancel the original method so player doesn't rotate
            ci.cancel();
        }
    }
}
