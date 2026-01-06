package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import gg.miracle.modules.render.Freelook;
import gg.miracle.replay.render.VirtualCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
//? if >=1.21.11
import net.minecraft.world.World;
//? if <1.21.11
/*import net.minecraft.world.BlockView;*/
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);
    
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    //? if >=1.21.11 {
    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", shift = At.Shift.AFTER))
    private void onUpdateAfterRotation(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        // Virtual camera takes priority (for replay rendering)
        VirtualCamera virtualCamera = VirtualCamera.getInstance();
        if (virtualCamera != null && virtualCamera.isActive()) {
            setPos(virtualCamera.getX(), virtualCamera.getY(), virtualCamera.getZ());
            setRotation(virtualCamera.getYaw(), virtualCamera.getPitch());
            return;
        }
        
        // Freelook camera
        Freelook freelook = MiracleClient.getInstance().getModuleManager().get(Freelook.class);
        if (freelook != null && freelook.isFreelookActive()) {
            setRotation(freelook.getCameraYaw(), freelook.getCameraPitch());
        }
    }
    //?} else {
    /*@Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", shift = At.Shift.AFTER))
    private void onUpdateAfterRotation(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        // Virtual camera takes priority (for replay rendering)
        VirtualCamera virtualCamera = VirtualCamera.getInstance();
        if (virtualCamera != null && virtualCamera.isActive()) {
            setPos(virtualCamera.getX(), virtualCamera.getY(), virtualCamera.getZ());
            setRotation(virtualCamera.getYaw(), virtualCamera.getPitch());
            return;
        }
        
        // Freelook camera
        Freelook freelook = MiracleClient.getInstance().getModuleManager().get(Freelook.class);
        if (freelook != null && freelook.isFreelookActive()) {
            setRotation(freelook.getCameraYaw(), freelook.getCameraPitch());
        }
    }
    *///?}
}
