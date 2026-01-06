package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import gg.miracle.api.modules.Module;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
//? if >=1.21.9
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    //? if >=1.21.9 {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        if (action != 1) return;
        if (client.currentScreen != null) return;

        int key = input.key();
        for (Module module : MiracleClient.getInstance().getModuleManager().getAll()) {
            if (module.getKeyBind() == key) {
                module.toggle();
            }
        }
    }
    //?} else {
    /*@Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action != 1) return;
        if (client.currentScreen != null) return;

        for (Module module : MiracleClient.getInstance().getModuleManager().getAll()) {
            if (module.getKeyBind() == key) {
                module.toggle();
            }
        }
    }
    *///?}
}
