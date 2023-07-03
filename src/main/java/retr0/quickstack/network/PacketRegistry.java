package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import static retr0.quickstack.network.C2SPacketDepositRequest.DEPOSIT_REQUEST_ID;

public class PacketRegistry {
    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(DEPOSIT_REQUEST_ID, C2SPacketDepositRequest::receive);
    }
}
