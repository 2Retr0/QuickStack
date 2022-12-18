package retr0.quickstack;

import net.fabricmc.api.ClientModInitializer;
import retr0.quickstack.network.PacketRegistry;

public class QuickStackClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PacketRegistry.registerS2CPackets();
    }
}
