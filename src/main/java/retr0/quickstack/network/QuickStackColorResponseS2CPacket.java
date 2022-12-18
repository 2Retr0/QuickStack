package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import retr0.quickstack.QuickStack;
import retr0.quickstack.util.ColorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuickStackColorResponseS2CPacket {
    public static void receive(
        MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender)
    {
        QuickStack.LOGGER.info("QuickStackColorResponse Received!");
        var screenHandlerSyncId = buf.readInt();
        var depositedContainerCount = buf.readByte();
        var containerSlotMap = new HashMap<BlockPos, List<Integer>>();

        for (var i = 0; i < depositedContainerCount; ++i) {
            var containerPos = buf.readBlockPos();
            var associatedSlots = new ArrayList<Integer>();

            var associatedSlotCount = buf.readByte();
            for (var j = 0; j < associatedSlotCount; ++j) {
                int slot = buf.readByte();
                associatedSlots.add(slot);
            }
            containerSlotMap.put(containerPos, associatedSlots);
        }
        QuickStack.LOGGER.info("QuickStackColorResponse Finished Processing!");

        client.execute(() -> {
            QuickStack.LOGGER.info("QuickStackColorResponse Executed!");
            // if (client.player.playerScreenHandler.syncId != screenHandlerSyncId) return;
            ColorManager.addMappings(client, containerSlotMap);
        });
    }
}
