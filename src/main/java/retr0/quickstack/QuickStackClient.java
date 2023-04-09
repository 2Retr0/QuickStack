package retr0.quickstack;

import net.fabricmc.api.ClientModInitializer;
import retr0.quickstack.config.QuickStackConfig;
import retr0.quickstack.network.PacketRegistry;
import retr0.quickstack.util.OutlineColorManager;

import static retr0.quickstack.QuickStack.MOD_ID;

public class QuickStackClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        QuickStackConfig.init(MOD_ID, QuickStackConfig.class);

        PacketRegistry.registerS2CPackets();
        OutlineColorManager.register();
    }
}
