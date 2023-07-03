package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import retr0.quickstack.util.IconPair;

import java.util.Comparator;
import java.util.Map;

import static retr0.quickstack.network.PacketIdentifiers.TOAST_RESULT_ID;

/**
 * Maps items to a "deposited total" along with an immutable icon for the container it was deposited into.
 */
public class S2CPacketToastResult {
    /**
     * Sends a {@link S2CPacketToastResult} packet to the client.
     */
    public static void send(
        Map<Item, Pair<Integer, ItemStack>> itemUsageMap, int totalItemsDeposited, int totalContainersUsed,
        ServerPlayerEntity player)
    {
        var topDeposited = itemUsageMap.entrySet().stream()
            .sorted(Comparator.comparingInt(entry -> -entry.getValue().getLeft()))
            .limit(3)
            .map(entry -> new IconPair(entry.getKey().getDefaultStack(), entry.getValue().getRight()))
            .toList();

        var buf = PacketByteBufs.create();
        // Note: In the case where an item is deposited into multiple containers, the container icon for the toast
        //       would be the highest priority container for the item (denoted by the entry in the item->container
        //       mappings).
        buf.writeInt(totalItemsDeposited);
        buf.writeByte(totalContainersUsed);
        buf.writeByte(topDeposited.size());
        topDeposited.forEach(iconPair -> {
            buf.writeItemStack(iconPair.itemIcon());
            buf.writeItemStack(iconPair.containerIcon());
        });
        ServerPlayNetworking.send(player, TOAST_RESULT_ID, buf);
    }
}
