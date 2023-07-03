package retr0.quickstack.network.client;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import retr0.quickstack.util.InventoryUtil.InventorySource;
import retr0.quickstack.util.InventoryUtil.InventorySource.SourceType;
import retr0.quickstack.util.OutlineColorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class S2CPacketDepositResult {
    public static void receive(
        MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender)
    {
        var slotUsageMap = new HashMap<Integer, List<InventorySource<?>>>();

        var slotUsageSize = buf.readByte();
        for (var i = 0; i < slotUsageSize; ++i) {
            var associatedInventories = new ArrayList<InventorySource<?>>(1);
            var slotId = buf.readInt();

            var associatedInventoriesSize = buf.readByte();
            for (var j = 0; j < associatedInventoriesSize; ++j) {
                var sourceType = buf.readEnumConstant(SourceType.class);

                if (sourceType == SourceType.BLOCK_ENTITY)
                    associatedInventories.add(new InventorySource<>(buf.readBlockPos(), SourceType.BLOCK_ENTITY));
                else if (sourceType == SourceType.INVENTORY_ENTITY)
                    associatedInventories.add(new InventorySource<>(buf.readUuid(), SourceType.INVENTORY_ENTITY));
            }
            slotUsageMap.put(slotId, associatedInventories);
        }

        client.execute(() -> {
            if (client.player == null) return;

            OutlineColorManager.getInstance().addMappings(client.player.getWorld(), slotUsageMap);
        });
    }
}
