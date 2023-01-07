package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import retr0.quickstack.QuickStack;
import retr0.quickstack.util.QuickStackUtil;

public class QuickStackRequestC2SPacket {
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
