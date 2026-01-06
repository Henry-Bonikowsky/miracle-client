package gg.miracle.mixin;

import gg.miracle.MiracleClient;
import gg.miracle.replay.detection.HighlightDetector;
import gg.miracle.replay.detection.HighlightEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    
    @Unique
    private double miracle$previousFallDistance = 0;
    
    /**
     * Detect player death.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        HighlightDetector detector = MiracleClient.getInstance().getReplayBufferManager().getHighlightDetector();
        
        if (detector == null) return;
        
        // Check for clutch survival (low health)
        if (player.getHealth() > 0 && player.getHealth() <= 6.0f) {
            // Player is below 3 hearts but alive
            // Check if they just took damage (fell, got hit, etc.)
            if (player.hurtTime > 0) {
                detector.registerEvent(HighlightEvent.CLUTCH_SURVIVAL);
            }
        }
        
        // Check for long fall survival
        if (player.isOnGround() && miracle$previousFallDistance > 10.0) {
            // Player fell more than 10 blocks and survived
            detector.registerEvent(HighlightEvent.LONG_FALL_SURVIVAL);
        }
        
        // Update previous fall distance
        miracle$previousFallDistance = player.fallDistance;
    }
    
    // Note: Death detection removed - onDeath doesn't exist in ClientPlayerEntity
    // Can detect death by checking if health <= 0 in tick() instead if needed
}
