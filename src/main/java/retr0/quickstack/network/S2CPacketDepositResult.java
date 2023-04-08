package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import retr0.quickstack.util.InventoryUtil.InventoryInfo;
import retr0.quickstack.util.InventoryUtil.InventorySource;
import retr0.quickstack.util.InventoryUtil.InventorySource.SourceType;
import retr0.quickstack.util.OutlineColorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static retr0.quickstack.QuickStack.MOD_ID;

public class S2CPacketDepositResult {
    public static final Identifier DEPOSIT_RESULT_ID = new Identifier(MOD_ID, "quick_stack_color_response");

    public static void send(Map<Integer, List<InventoryInfo>> slotUsageMap, ServerPlayerEntity player) {
        var buf = PacketByteBufs.create();

        buf.writeByte(slotUsageMap.size());
        slotUsageMap.forEach(((slotId, usageInfoList) -> {
            buf.writeInt(slotId);
            buf.writeByte(usageInfoList.size());

            // Write associated inventory information.
            usageInfoList.forEach(inventoryInfo -> {
                var sourceType = inventoryInfo.source().sourceType();

                buf.writeEnumConstant(sourceType);
                if (sourceType == SourceType.BLOCK_ENTITY)
                    buf.writeBlockPos(inventoryInfo.sourcePosition());
                else if (sourceType == SourceType.INVENTORY_ENTITY) {
                    buf.writeUuid(((Entity) inventoryInfo.source().instance()).getUuid());
                }
            });
        }));
        ServerPlayNetworking.send(player, DEPOSIT_RESULT_ID, buf);
    }



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

            OutlineColorManager.getInstance().addMappings(client.player.world, slotUsageMap);
        });
    }
}
