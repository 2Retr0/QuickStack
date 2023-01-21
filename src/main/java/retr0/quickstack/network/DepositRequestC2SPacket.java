package retr0.quickstack.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import retr0.quickstack.QuickStack;
import retr0.quickstack.util.QuickStackUtil;

import static retr0.quickstack.QuickStack.MOD_ID;

public class DepositRequestC2SPacket {
    public static final Identifier DEPOSIT_REQUEST_ID = new Identifier(MOD_ID, "request_quick_stack");

    public static void send() {
        ClientPlayNetworking.send(DEPOSIT_REQUEST_ID, PacketByteBufs.empty());
    }

    /**
     * Executes a quick stack operation for the sender player.
     */
    public static void receive(
        MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf,
        PacketSender responseSender)
    {
        QuickStack.LOGGER.info("QuickStackRequest Received!");
        server.execute(() -> {
            QuickStack.LOGGER.info("QuickStackRequest Executed!");
            QuickStackUtil.quickStack(player, 8);
        });
    }
}
