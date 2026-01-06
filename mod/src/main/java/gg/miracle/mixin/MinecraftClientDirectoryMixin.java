package gg.miracle.mixin;

import gg.miracle.api.DirectoryManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

/**
 * Mixin to override Minecraft's resource pack and shader pack directories
 * with profile-specific paths when set by the launcher.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientDirectoryMixin {

    /**
     * Inject into getResourcePackDir() to return custom directory if set.
     * This makes Minecraft look for resource packs in the profile-specific folder.
     */
    @Inject(method = "getResourcePackDir", at = @At("HEAD"), cancellable = true)
    private void getCustomResourcePackDir(CallbackInfoReturnable<Path> cir) {
        if (DirectoryManager.hasCustomResourcePacksDir()) {
            cir.setReturnValue(DirectoryManager.getCustomResourcePacksDir());
        }
    }
}
