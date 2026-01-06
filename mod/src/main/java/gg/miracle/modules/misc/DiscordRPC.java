package gg.miracle.modules.misc;

import gg.miracle.MiracleClient;
import gg.miracle.api.modules.Category;
import gg.miracle.api.modules.Module;

public class DiscordRPC extends Module {
    public DiscordRPC() {
        super("DiscordRPC", "Shows Miracle Client in Discord", Category.MISC);
    }

    @Override
    protected void onEnable() {
        // Discord RPC initialization would go here
        MiracleClient.LOGGER.info("Discord RPC enabled");
    }

    @Override
    protected void onDisable() {
        // Discord RPC cleanup would go here
        MiracleClient.LOGGER.info("Discord RPC disabled");
    }
}
