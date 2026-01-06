package gg.miracle.modules.misc;

import gg.miracle.MiracleClient;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;
import gg.miracle.replay.AutoClipSystem;

/**
 * Automatically generates cinematic background clips every 10 minutes.
 * Clips are saved to AppData/MiracleClient/background-videos/
 */
public class AutoClip extends Module {
    
    private AutoClipSystem autoClipSystem;
    
    public AutoClip() {
        super("Auto Clip", "Automatically generate cinematic background clips", Category.MISC);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        if (autoClipSystem == null) {
            autoClipSystem = MiracleClient.getInstance().getAutoClipSystem();
        }
        
        if (autoClipSystem != null) {
            autoClipSystem.enable();
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (autoClipSystem != null) {
            autoClipSystem.disable();
        }
    }
    
    /**
     * Get the auto-clip system instance.
     */
    public AutoClipSystem getAutoClipSystem() {
        return autoClipSystem;
    }
}
