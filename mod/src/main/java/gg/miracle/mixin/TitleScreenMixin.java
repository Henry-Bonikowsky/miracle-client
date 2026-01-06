package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import gg.miracle.gui.MiracleTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void replaceTitleScreen(CallbackInfo ci) {
        MiracleClient.LOGGER.info("[TitleScreenMixin] init() called - replacing with MiracleTitleScreen");
        MinecraftClient client = MinecraftClient.getInstance();
        // Replace with our custom title screen
        client.setScreen(new MiracleTitleScreen());
        ci.cancel();
    }
}
