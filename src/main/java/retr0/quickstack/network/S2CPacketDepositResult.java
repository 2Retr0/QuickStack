package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import retr0.quickstack.util.InventoryUtil.InventoryInfo;
import retr0.quickstack.util.InventoryUtil.InventorySource.SourceType;

import java.util.List;
import java.util.Map;

import static retr0.quickstack.network.PacketIdentifiers.DEPOSIT_RESULT_ID;

public class S2CPacketDepositResult {
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
}
