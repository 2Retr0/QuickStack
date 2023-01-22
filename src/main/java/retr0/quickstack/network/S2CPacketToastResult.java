package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import retr0.quickstack.QuickStack;
import retr0.quickstack.QuickStackToast;
import retr0.quickstack.QuickStackToast.IconPair;

import java.util.*;

import static retr0.quickstack.QuickStack.MOD_ID;

/**
 * Maps items to a "deposited total" along with an immutable icon for the container it was deposited into.
 */
public class S2CPacketToastResult {
    public static final Identifier TOAST_RESULT_ID = new Identifier(MOD_ID, "quick_stack_response");

    private final Map<Item, Pair<Integer, ItemStack>> itemUsageMap = new HashMap<>();
    private final Set<BlockPos> usedContainers = new HashSet<>();

    private int totalDepositAmount = 0;

    /**
     * Adds a specified amount to the deposited total entry of an item creating a new entry if needed.
     * @param depositedItem The specified item.
     * @param depositedCount The amount to add to the item entry's deposited total.
     * @param containerIcon An {@link ItemStack}, representing the deposited container's icon, to bind to
     *                      the item entry if it does not yet exist.
     */
    public void updateDepositAmount(
        Item depositedItem, int depositedCount, BlockPos containerPos, ItemStack containerIcon)
    {
        if (itemUsageMap.containsKey(depositedItem)) {
            var itemUsage = itemUsageMap.get(depositedItem);
            itemUsage.setLeft(itemUsage.getLeft() + depositedCount);
        } else
            itemUsageMap.put(depositedItem, new Pair<>(depositedCount, containerIcon));

        totalDepositAmount += depositedCount;
        usedContainers.add(containerPos);
    }



    /**
     * @return A sorted {@code List} (in descending order) containing the icons for the top {@code n} most-deposited
     * items along with the icons for their respective container.
     */
    @SuppressWarnings("SameParameterValue")
    private List<IconPair> getTopNDeposited(int n) {
        var topUsedList = new ArrayList<IconPair>(n);
        var topUsedQueue = new PriorityQueue<Pair<Integer, IconPair>>(
            Comparator.comparingInt(info -> -info.getLeft()));

        // Create a priority queue of all icon mappings in the map sorted by deposited count.
        itemUsageMap.forEach((item, usageInfo) ->
            topUsedQueue.add(new Pair<>(usageInfo.getLeft(), new IconPair(item.getDefaultStack(), usageInfo.getRight()))));

        // Create a list containing the top 'n' mappings by polling the priority queue.
        for (var i = 0; i < n && !topUsedQueue.isEmpty(); ++i)
            topUsedList.add(topUsedQueue.poll().getRight());
        return topUsedList;
    }



    /**
     * Sends a {@link S2CPacketToastResult} packet to the client.
     */
    public static void send(S2CPacketToastResult toastResult, ServerPlayerEntity player) {
        var totalUsedContainers = toastResult.usedContainers.size();
        var topDeposited = toastResult.getTopNDeposited(3);
        var buf = PacketByteBufs.create();

        //   * Note: In the case where an item is deposited into multiple containers, the container icon for the toast
        //           would be the highest priority container for the item (denoted by the entry in the item->container
        //           mappings).
        buf.writeInt(toastResult.totalDepositAmount);
        buf.writeByte(totalUsedContainers);
        buf.writeByte(topDeposited.size());
        topDeposited.forEach(iconPair -> {
            buf.writeItemStack(iconPair.itemIcon());
            buf.writeItemStack(iconPair.containerIcon());
        });
        ServerPlayNetworking.send(player, TOAST_RESULT_ID, buf);
    }



    /**
     * Shows/updates a {@link QuickStackToast} instance with the packet data on the client.
     */
    public static void receive(
        MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender)
    {
        var depositCount = buf.readInt();
        var containerCount = buf.readByte();
        var iconMappings = new ArrayList<IconPair>();

        var iconMappingsCount = buf.readByte();
        for (var i = 0; i < iconMappingsCount; ++i) {
            var itemIcon = buf.readItemStack();
            var containerIcon = buf.readItemStack();

            iconMappings.add(new IconPair(itemIcon, containerIcon));
        }

        client.execute(() -> QuickStackToast.show(client.getToastManager(), depositCount, containerCount, iconMappings));
    }
}
