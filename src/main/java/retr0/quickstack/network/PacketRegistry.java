package retr0.quickstack.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import static retr0.quickstack.network.DepositRequestC2SPacket.DEPOSIT_REQUEST_ID;
import static retr0.quickstack.network.DepositResultS2CPacket.DEPOSIT_RESULT_ID;
import static retr0.quickstack.network.ToastResultS2CPacket.TOAST_RESULT_ID;

public class PacketRegistry {
    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(DEPOSIT_REQUEST_ID, DepositRequestC2SPacket::receive);
    }

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(TOAST_RESULT_ID, ToastResultS2CPacket::receive);
        ClientPlayNetworking.registerGlobalReceiver(DEPOSIT_RESULT_ID, DepositResultS2CPacket::receive);
    }
}
