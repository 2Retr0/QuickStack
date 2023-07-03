package retr0.quickstack;

import net.fabricmc.api.ClientModInitializer;
import retr0.quickstack.network.client.PacketRegistry;
import retr0.quickstack.util.OutlineColorManager;

public class QuickStackClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PacketRegistry.registerS2CPackets();
        OutlineColorManager.register();
    }
}
