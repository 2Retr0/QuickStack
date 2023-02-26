package retr0.quickstack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import retr0.quickstack.network.PacketRegistry;
import retr0.quickstack.util.OutlineRenderManager;

import static net.fabricmc.fabric.api.resource.ResourcePackActivationType.DEFAULT_ENABLED;
import static retr0.quickstack.QuickStack.MOD_ID;

public class QuickStackClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PacketRegistry.registerS2CPackets();

        ClientTickEvents.START_WORLD_TICK.register(clientWorld -> OutlineRenderManager.INSTANCE.tick());
    }
}
