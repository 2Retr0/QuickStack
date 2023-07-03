package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import retr0.quickstack.config.QuickStackConfig;
import retr0.quickstack.util.QuickStackManager;

import static retr0.quickstack.QuickStack.MOD_ID;

public class C2SPacketDepositRequest {
    public static final Identifier DEPOSIT_REQUEST_ID = new Identifier(MOD_ID, "request_quick_stack");

    /**
     * Executes a quick stack operation for the sender player.
     */
    public static void receive(
        MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf,
        PacketSender responseSender)
    {
        server.execute(() -> QuickStackManager.getInstance().quickStack(
                player, QuickStackConfig.containerSearchRadius, QuickStackConfig.allowHotbarQuickStack));
    }
}
