package retr0.quickstack.network.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import static retr0.quickstack.network.PacketIdentifiers.DEPOSIT_REQUEST_ID;

public class C2SPacketDepositRequest {
    public static void send() {
        ClientPlayNetworking.send(DEPOSIT_REQUEST_ID, PacketByteBufs.empty());
    }
}
