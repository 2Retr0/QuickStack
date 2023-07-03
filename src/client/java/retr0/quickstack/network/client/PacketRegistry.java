package retr0.quickstack.network.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import static retr0.quickstack.network.PacketIdentifiers.*;

public class PacketRegistry {
    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(TOAST_RESULT_ID, S2CPacketToastResult::receive);
        ClientPlayNetworking.registerGlobalReceiver(DEPOSIT_RESULT_ID, S2CPacketDepositResult::receive);
    }
}
