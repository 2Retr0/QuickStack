package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import retr0.quickstack.QuickStack;
import retr0.quickstack.QuickStackToast;
import retr0.quickstack.network.util.QuickStackResult;

import java.util.ArrayList;

public class QuickStackResponseS2CPacket {
    /**
     * Shows/updates a {@link QuickStackToast} instance containing the {@link QuickStackResult}'s information from the
     * received packet.
     */
    public static void receive(
        MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender)
    {
        QuickStack.LOGGER.info("QuickStackResponse Received!");
        var depositCount = buf.readInt();
        var containerCount = buf.readByte();
        var iconMappings = new ArrayList<QuickStackResult.IconMapping>();

        var iconMappingsCount = buf.readByte();
        for (var i = 0; i < iconMappingsCount; ++i) {
            var itemIcon = buf.readItemStack();
            var containerIcon = buf.readItemStack();

            iconMappings.add(new QuickStackResult.IconMapping(itemIcon, containerIcon));
        }
        QuickStack.LOGGER.info("QuickStackResponse Finished Processing!");

        client.execute(() -> {
            QuickStack.LOGGER.info("QuickStackResponse Executed!");
            var quickStackInfo = new QuickStackResult(depositCount, containerCount, iconMappings);
            QuickStackToast.show(client.getToastManager(), quickStackInfo);
        });
    }
}
