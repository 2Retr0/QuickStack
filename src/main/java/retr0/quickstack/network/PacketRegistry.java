package retr0.quickstack.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import static retr0.quickstack.network.C2SPacketDepositRequest.DEPOSIT_REQUEST_ID;
import static retr0.quickstack.network.S2CPacketDepositResult.DEPOSIT_RESULT_ID;
import static retr0.quickstack.network.S2CPacketToastResult.TOAST_RESULT_ID;

public class PacketRegistry {
    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(DEPOSIT_REQUEST_ID, C2SPacketDepositRequest::receive);
    }

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(TOAST_RESULT_ID, S2CPacketToastResult::receive);
        ClientPlayNetworking.registerGlobalReceiver(DEPOSIT_RESULT_ID, S2CPacketDepositResult::receive);
    }
}
