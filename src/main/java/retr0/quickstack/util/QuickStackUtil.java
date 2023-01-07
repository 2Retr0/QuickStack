package retr0.quickstack.util;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import retr0.quickstack.QuickStack;
import retr0.quickstack.mixin.AccessorLootableContainerBlockEntity;
import retr0.quickstack.network.PacketRegistry;
import retr0.quickstack.network.util.QuickStackResult;

import java.util.*;

public final class QuickStackUtil {
    /**
     * Searches for nearby block entities whose class extends {@link LootableContainerBlockEntity} and <em>doesn't</em>
     * implement {@link SidedInventory} (e.g., 'Container' block entities such as chests, barrels, and dispensers).
     * Found inventories which still has an active loot table (e.g., unopened naturally-generated chests) are also
     * disregarded.
     *
     * @return A {@code List} containing the found block entities as {@link InventoryInfo}s.
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
            if (blockEntity instanceof AccessorLootableContainerBlockEntity container
                && !(blockEntity instanceof SidedInventory)
                && container.getLootTableId() == null)
            {
                var blockState = world.getBlockState(mutablePos);
                var block = blockState.getBlock();
                var inventory = (Inventory) container;
                var blockIcon = new ItemStack(block.asItem());

                // We add the inventory/double inventory for chest blocks, so long as the chest is not the "second
                // chest" of a double chest (to prevent double counting).
                if (block instanceof ChestBlock chestBlock) {
                    if (ChestBlock.getDoubleBlockType(blockState) != DoubleBlockProperties.Type.SECOND)
                        inventory = ChestBlock.getInventory(chestBlock, blockState, world, mutablePos, false);
                    else continue;
                }
                nearbyContainers.add(new InventoryInfo(inventory, new BlockPos(mutablePos), blockIcon));
            }
        }
        return nearbyContainers;
    }



    /**
     * Initiates a quick stack operation for the specified player considering all chests within the specified radius
     * for depositing.
     */
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
        uniquePlayerItems.forEach(item -> {
            if (!(item instanceof BundleItem || item instanceof ToolItem || item instanceof ArrowItem ||
                item.isFood() || item instanceof PotionItem || item instanceof BucketItem || item.getTranslationKey().equalsIgnoreCase("block.minecraft.torch") ||
                item instanceof RangedWeaponItem || item instanceof CompassItem))
            {
                itemContainerMap.put(item, new PriorityQueue<>(
                    Comparator.comparingInt(inventoryInfo -> -InventoryUtil.getAvailableSlots(inventoryInfo.inventory(), item))));
            }
            QuickStack.LOGGER.info(item.getTranslationKey());
        });

        if (itemContainerMap.isEmpty()) {
            QuickStack.LOGGER.info("Nothing found!");
            return;
        }

        // For each nearby inventory, add the inventory to all queues in the container map which correspond to
        // an item which exists in said inventory.
        nearbyInventories.forEach(inventoryInfo -> {
            // Only consider the intersection of items between the target inventory and player inventory.
            var intersection = InventoryUtil.getUniqueItems(inventoryInfo.inventory());

            intersection.retainAll(uniquePlayerItems);
            intersection.forEach(item -> {
                if (itemContainerMap.containsKey(item))
                    itemContainerMap.get(item).add(inventoryInfo);
            });
        });
        QuickStack.LOGGER.info("Mappings: " + itemContainerMap);

        // Second Click (if also on button) //
        // TODO: This can be done while the item is being animated traversing to the quickstack icon.
        // For each item in the player's *main* inventory try to insert the item into the head inventory of the
        // associated queue.
        var depositCount = 0;
        var containerUsageMap = new ContainerUsageMap();
        var pathFinder = new PathFinder(serverWorld, 8, player.getBlockPos());

        // TODO: Do not hard code inventory slot indices!
        for (var slot = 9; slot < 36; ++slot) {
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

                    containerUsageMap.updateUsage(headInfo.blockPos, slot);

                    // TODO: We need a map for blockpos to color and slots to color

                    // Update itemUsageMap with the deposited amount.
                    itemUsageMap.updateUsage(originalItem, depositCount, headInfo.icon);
                }
            }
        }

        // Get top 3 items and their respective inventories.
        // Note: In the case where an item is deposited into multiple containers, the container icon for the toast would
        //       be the highest priority container for the item (denoted by the entry in the item->container mappings)
        //       which is a good consequence of this overly-engineered design!
        var containerCount = containerUsageMap.size();
        if (depositCount > 0) {
            QuickStack.LOGGER.info(player.getName().getString() +
                " quick stacked " + depositCount + " item" + (depositCount > 1 ? "s" : "") +
                " into " + containerCount + " container" + (containerCount > 1 ? "s" : ""));

            var quickStackInfo = new QuickStackResult(depositCount, containerCount, itemUsageMap.getTopNDeposited(3));
            ServerPlayNetworking.send(player, PacketRegistry.QUICK_STACK_RESPONSE_ID, QuickStackResult.createByteBuf(quickStackInfo));
            ServerPlayNetworking.send(player, PacketRegistry.QUICK_STACK_COLOR_RESULT_ID, containerUsageMap.createByteBuf(player));
            playSound(player, containerCount);
        }
    }

    private static void playSound(PlayerEntity player, int count) {
        var emitPos = player.getEyePos().subtract(player.getRotationVector().multiply(3));

        for (var i = 0; i < Math.min(2, count); ++i) {
            DelayedCallbackManager.INSTANCE.scheduleCallback(() -> player.world.playSound(
                null, emitPos.x, emitPos.y, emitPos.z, SoundEvents.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS,
                0.5f, player.world.random.nextFloat() * 0.1f + 0.9f));
        }
    }



    /**
     * Record containing an inventory, the {@link BlockPos} of its respective block entity, and an {@link ItemStack}
     * representing said block entity.
     */
    private record InventoryInfo(Inventory inventory, BlockPos blockPos, ItemStack icon) { }



    /**
     * Record representing information assigned to a container (i.e. block entity) containing the tint color for the
     * block entity and the slots of the player inventory whose item stack has been transferred (completely or
     * partially) to the container.
     */
    private static class ContainerUsageMap {
        private final Map<BlockPos, List<Integer>> containerUsageMap = new HashMap<>();
        private final Set<Integer> slotsUsed = new HashSet<>();

        /**
         * Adds a specified amount to the deposited total entry for an item key creating a new entry if needed.
         * @param key The specified item key.
         * @param toAdd The amount to add to the item entry's deposited total.
         * @param defaultContainerIcon An {@link ItemStack}, representing the deposited container's icon, to bind to
         *                             the entry if the entry does not yet exist.
         */
        public void updateUsage(BlockPos key, int slot) {
            if (slotsUsed.contains(slot))
                return;
            else
                slotsUsed.add(slot);

            if (containerUsageMap.containsKey(key))
                containerUsageMap.get(key).add(slot);
            else
                containerUsageMap.put(key, new ArrayList<>(List.of(slot)));
        }



        public int size() { return containerUsageMap.size(); }



        public PacketByteBuf createByteBuf(ServerPlayerEntity player) {
            var buf = PacketByteBufs.create();

            buf.writeByte(containerUsageMap.size());
            // TODO: CONURRENCY MODIFICATION
            containerUsageMap.forEach(((blockPos, slots) -> {
                buf.writeBlockPos(blockPos);   // Write container's BlockPos.
                buf.writeByte(slots.size());   // Write container's associated slot list size.
                slots.forEach(buf::writeByte); // Write container's associated slots.
            }));

            return buf;
        }
    }



    /**
     * Maps items to a "deposited total" along with an immutable icon for the container it was deposited into.
     */
    private static class ItemUsageMap {
        private final Map<Item, Pair<Integer, ItemStack>> itemUsageMap = new HashMap<>();

        /**
         * Adds a specified amount to the deposited total entry for an item key creating a new entry if needed.
         * @param key The specified item key.
         * @param toAdd The amount to add to the item entry's deposited total.
         * @param defaultContainerIcon An {@link ItemStack}, representing the deposited container's icon, to bind to
         *                             the entry if the entry does not yet exist.
         */
        public void updateUsage(Item key, int toAdd, ItemStack defaultContainerIcon) {
            if (itemUsageMap.containsKey(key)) {
                var itemUsage = itemUsageMap.get(key);
                itemUsage.setLeft(itemUsage.getLeft() + toAdd);
            } else
                itemUsageMap.put(key, new Pair<>(0, defaultContainerIcon));
        }



        /**
         * @return A sorted {@code List} (in descending order) of the top {@code n} most-deposited items mapped to
         * their entry's container icon.
         */
        public List<QuickStackResult.IconMapping> getTopNDeposited(int n) {
            var topUsedList = new ArrayList<QuickStackResult.IconMapping>(n);
            var topUsedQueue = new PriorityQueue<Pair<Integer, QuickStackResult.IconMapping>>(
                Comparator.comparingInt(info -> -info.getLeft()));

            // Create a priority queue of all icon mappings in the map sorted by deposited count.
            itemUsageMap.forEach((item, usageInfo) -> {
                topUsedQueue.add(new Pair<>(
                    usageInfo.getLeft(),
                    new QuickStackResult.IconMapping(item.getDefaultStack(), usageInfo.getRight())));
            });

            // Create a list containing the top 'n' mappings by polling the priority queue.
            for (var i = 0; i < n && !topUsedQueue.isEmpty(); ++i)
                topUsedList.add(topUsedQueue.poll().getRight());
            return topUsedList;
        }
    }

    private QuickStackUtil() { }
}
