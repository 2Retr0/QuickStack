package retr0.quickstack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retr0.quickstack.network.PacketRegistry;
import retr0.quickstack.util.OutlineRenderManager;
import retr0.quickstack.util.QuickStackManager;

public class QuickStack implements ModInitializer {
    public static final String MOD_ID = "quickstack";
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Initialized QuickStack!");

        PacketRegistry.registerC2SPackets();

        ServerTickEvents.START_WORLD_TICK.register(clientWorld -> QuickStackManager.INSTANCE.tick());
    }
}
