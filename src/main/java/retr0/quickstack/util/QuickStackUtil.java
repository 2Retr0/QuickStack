package retr0.quickstack.util;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import retr0.quickstack.QuickStack;
import retr0.quickstack.mixin.MixinLootableContainerBlockEntity;
import retr0.quickstack.network.PacketRegistry;
import retr0.quickstack.network.QuickStackResponseS2CPacket;

import java.util.*;

public final class QuickStackUtil {
    private record InventoryInfo(Inventory inventory, BlockPos blockPos, ItemStack icon) { }

    private record ItemUsageMap(HashMap<Item, Pair<Integer, ItemStack>> itemUsageMap) {
        public ItemUsageMap() { this(new HashMap<>()); }

        public void updateUsage(Item key, int toAdd, ItemStack defaultIcon) {
            if (itemUsageMap.containsKey(key)) {
                var itemUsage = itemUsageMap.get(key);
                itemUsage.setLeft(itemUsage.getLeft() + toAdd);
            } else
                itemUsageMap.put(key, new Pair<>(0, defaultIcon));
        }



        public List<Pair<ItemStack, ItemStack>> getTopNUsed(int n) {
            var topUsedList = new ArrayList<Pair<ItemStack, ItemStack>>(n);
            var topUsedMaxHeap = new PriorityQueue<Pair<Integer, Pair<ItemStack, ItemStack>>>(
                Comparator.comparingInt(info -> -info.getLeft()));

            itemUsageMap.forEach((item, usageInfo) -> topUsedMaxHeap.add(
                new Pair<>(usageInfo.getLeft(), new Pair<>(item.getDefaultStack(), usageInfo.getRight()))));

            for (var i = 0; i < n && !topUsedMaxHeap.isEmpty(); ++i)
                topUsedList.add(topUsedMaxHeap.poll().getRight());

            return topUsedList;
        }
    }

    public record QuickStackInfo(int depositCount, int containerCount, List<Pair<ItemStack, ItemStack>> iconMappings) { }

    /**
     * Searches for nearby block entities whose class extends {@link LootableContainerBlockEntity} and <em>doesn't</em>
     * implement {@link SidedInventory} (i.e., 'Container' block entities such as chests, barrels, and dispensers).
     * Found inventories which still has an active loot table (e.g., unopened naturally-generated chests) are also
     * disregarded.
     *
     * @return A {@code List} containing the found nearby block entities as inventories.
     */
    public static List<InventoryInfo> findNearbyInventories(World world, BlockPos pos, int radius) {
        var nearbyContainers = new ArrayList<InventoryInfo>();
        var mutablePos = new BlockPos.Mutable();
        var cuboidBlockIterator = new CuboidBlockIterator(
            pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
            pos.getX() + radius, pos.getY() + radius, pos.getZ() + radius);

        while (cuboidBlockIterator.step()) {
            mutablePos.set(cuboidBlockIterator.getX(), cuboidBlockIterator.getY(), cuboidBlockIterator.getZ());
            var blockEntity = world.getBlockEntity(mutablePos);

            // Note: LootTableId is null if the container is player-placed or has been opened by a player.
            if (blockEntity instanceof MixinLootableContainerBlockEntity container
                && !(blockEntity instanceof SidedInventory)
                && container.getLootTableId() == null)
            {
                var blockIcon = new ItemStack(world.getBlockState(mutablePos).getBlock().asItem());
                nearbyContainers.add(new InventoryInfo((Inventory) container, new BlockPos(mutablePos), blockIcon));
            }
        }
        return nearbyContainers;
    }



    public static void quickStack(ServerPlayerEntity player, int radius) {
        var itemContainerMap = new HashMap<Item, Queue<InventoryInfo>>();
        var itemUsageMap = new ItemUsageMap();
        var playerInventory = player.getInventory();
        var serverWorld = player.getWorld();

        // First Click //
        var nearbyInventories = findNearbyInventories(serverWorld, player.getBlockPos(), radius);
        QuickStack.LOGGER.info("Found Nearby Inventories: " + nearbyInventories);

        // For each unique item in the player's inventory, create a corresponding priority queue in the
        // container map prioritizing the inventory with the most free slots (with respect to the item).
        var uniquePlayerItems = InventoryUtil.getUniqueItems(playerInventory, 9, 35);
        QuickStack.LOGGER.info("Found Player Unique Items: " + uniquePlayerItems);
        uniquePlayerItems.forEach(item -> itemContainerMap.put(item, new PriorityQueue<>(
            Comparator.comparingInt(inventoryInfo -> -InventoryUtil.getAvailableSlots(inventoryInfo.inventory(), item)))));

        // For each nearby inventory, add the inventory to all queues in the container map which correspond to
        // an item which exists in said inventory.
        nearbyInventories.forEach(inventoryInfo -> {
            // Only consider the intersection of items between the target inventory and player inventory.
            var intersection = InventoryUtil.getUniqueItems(inventoryInfo.inventory());

            intersection.retainAll(uniquePlayerItems);
            intersection.forEach(item -> itemContainerMap.get(item).add(inventoryInfo));
        });
        QuickStack.LOGGER.info("Mappings: " + itemContainerMap);

        // Second Click (if also on button) //
        // TODO: This can be done while the item is being animated traversing to the quickstack icon.
        // For each item in the player's *main* inventory try to insert the item into the head inventory of the
        // associated queue.
        var depositCount = 0;
        var usedContainers = new HashSet<BlockPos>();
        var pathFinder = new PathFinder(serverWorld, 8, player.getBlockPos());

        for (var slot = 9; slot <= 35; ++slot) {
            var itemStack = playerInventory.getStack(slot);
            var containerQueue = itemContainerMap.get(itemStack.getItem());

            if (containerQueue == null) continue;

            QuickStack.LOGGER.info("Trying to Stack: " + itemStack.getItem().getName().getString());
            while (itemStack.getCount() != 0 && !containerQueue.isEmpty()) {
                var headInfo = containerQueue.peek(); // Per-quick stack, only remove the head if full.
                var blockCenter = Vec3d.ofCenter(headInfo.blockPos());
                var originalCount = itemStack.getCount();
                var originalItem = itemStack.getItem();

                // If the head inventory can't insert the entire stack, remove it from consideration and repeat
                // the process with the new head inventory.
                if (!pathFinder.hasNearLineOfSight(blockCenter, player.getPos().add(0, 1.5, 0)) ||
                    !InventoryUtil.insert(playerInventory, headInfo.inventory(), slot)) {
                    QuickStack.LOGGER.info("COULDN'T REMOVE ENTIRE STACK");

                    containerQueue.poll();
                }
                // If the entire stack wasn't inserted, the remaining stack will be used again; otherwise, it will
                // just be used for deposited items/container calculations.
                itemStack = playerInventory.getStack(slot);

                if (originalCount != itemStack.getCount()) {
                    depositCount += originalCount - itemStack.getCount();
                    usedContainers.add(headInfo.blockPos);

                    // Update itemUsageMap with the deposited amount.
                    itemUsageMap.updateUsage(originalItem, depositCount, headInfo.icon);
                }
            }
        }

        // Get top 3 items and their respective inventories.
        // Note: In the case where an item is deposited into multiple containers, the container icon for the toast would
        //       be the highest priority container for the item (denoted by the entry in the item->container mappings)
        //       which is a good consequence of this overly-engineered design!
        var containerCount = usedContainers.size();
        if (depositCount > 0) {
            QuickStack.LOGGER.info(player.getName().getString() +
                " quick stacked " + depositCount + " item" + (depositCount > 1 ? "s" : "") +
                " into " + containerCount + " container" + (containerCount > 1 ? "s" : ""));

            var quickStackInfo = new QuickStackInfo(depositCount, containerCount, itemUsageMap.getTopNUsed(3));
            ServerPlayNetworking.send(player, PacketRegistry.QUICK_STACK_RESPONSE_ID, QuickStackResponseS2CPacket.create(quickStackInfo));
        }
    }

    private QuickStackUtil() { }
}
