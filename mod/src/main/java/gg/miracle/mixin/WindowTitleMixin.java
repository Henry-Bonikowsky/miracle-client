package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowTitleMixin {
    
    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void onSetTitle(String title, CallbackInfo ci) {
        // Cancel the original setTitle and set our own
        ci.cancel();
        
        Window window = (Window) (Object) this;
        long handle = window.getHandle();
        org.lwjgl.glfw.GLFW.glfwSetWindowTitle(handle, MiracleClient.MOD_NAME);
    }
}
