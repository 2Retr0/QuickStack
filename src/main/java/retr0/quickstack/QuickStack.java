package retr0.quickstack;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retr0.quickstack.network.PacketRegistry;

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

        // TODO: BLOCK AND INVENTORY HIGHLIGHTING
        // TODO: PATH FINDING FOR VALID QUICKSTACK
        // TODO: ITEM FAVORITEING
        LOGGER.info("Hello Fabric world!");

        PacketRegistry.registerC2SPackets();
        PacketRegistry.registerS2CPackets();

        // CONFIG IDEAS:
        // * ALLOW HOTBAR QUICKSTACK
        // * REQUIRE EXACT NBT
        // * SEARCH RADIUS
    }
}
