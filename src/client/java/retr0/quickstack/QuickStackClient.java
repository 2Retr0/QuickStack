package retr0.quickstack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import retr0.quickstack.config.QuickStackConfig;
import retr0.quickstack.network.client.S2CPacketDepositResult;
import retr0.quickstack.network.client.S2CPacketToastResult;
import retr0.quickstack.util.OutlineColorManager;

import static retr0.quickstack.QuickStack.MOD_ID;
import static retr0.quickstack.network.PacketIdentifiers.DEPOSIT_RESULT_ID;
import static retr0.quickstack.network.PacketIdentifiers.TOAST_RESULT_ID;

public class QuickStackClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        QuickStackConfig.init(MOD_ID, QuickStackConfig.class);

        ClientPlayNetworking.registerGlobalReceiver(TOAST_RESULT_ID, S2CPacketToastResult::receive);
        ClientPlayNetworking.registerGlobalReceiver(DEPOSIT_RESULT_ID, S2CPacketDepositResult::receive);

        OutlineColorManager.register();
    }
}
