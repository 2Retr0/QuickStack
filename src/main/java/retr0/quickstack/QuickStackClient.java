package retr0.quickstack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import retr0.quickstack.network.PacketRegistry;
import retr0.quickstack.util.OutlineRenderManager;

public class QuickStackClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PacketRegistry.registerS2CPackets();

        ClientTickEvents.START_WORLD_TICK.register(clientWorld -> OutlineRenderManager.INSTANCE.tick());
    }
}
