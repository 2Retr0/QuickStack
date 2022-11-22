package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import retr0.quickstack.util.QuickStackUtil;

public class QuickStackRequestC2SPacket {
    public static void receive(
        MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender)
    {
        server.execute(() -> QuickStackUtil.quickStack(player, 8));
    }
}
