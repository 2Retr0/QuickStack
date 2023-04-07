package retr0.quickstack.util;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.RideableInventory;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.Pair;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import retr0.quickstack.QuickStack;
import retr0.quickstack.compat.itemfavorites.CompatItemFavorites;
import retr0.quickstack.mixin.AccessorLootableContainerBlockEntity;
import retr0.quickstack.network.S2CPacketDepositResult;
import retr0.quickstack.network.S2CPacketToastResult;
import retr0.quickstack.util.InventoryUtil.InventoryInfo;

import java.util.*;

public final class QuickStackManager {
    private static final int MAX_STACK_SPREAD = 3;
    private static QuickStackManager instance;

    private QuickStackManager() { }



    public static void register() {
        if (QuickStackManager.instance == null) {
            instance = new QuickStackManager();
        }
    }



    public static QuickStackManager getInstance() {
        return instance;
    }



    /**
     * Searches for nearby block entities whose class extends {@link LootableContainerBlockEntity} and <em>doesn't</em>
     * implement {@link SidedInventory} (e.g., 'Container' block entities such as chests, barrels, and dispensers).
     * Found inventories which still has an active loot table (e.g., unopened naturally-generated chests) are also
     * disregarded.
     *
     * @return A {@code List} containing the found block entities as {@link InventoryInfo}s.
     */
    public static List<InventoryInfo> findNearbyInventories(World world, BlockPos pos, int radius) {
        var nearbyInventories = new ArrayList<InventoryInfo>();
        var mutablePos = new BlockPos.Mutable();
        var cuboidBlockIterator = new CuboidBlockIterator(
            pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
            pos.getX() + radius, pos.getY() + radius, pos.getZ() + radius);

        while (cuboidBlockIterator.step()) {
            mutablePos.set(cuboidBlockIterator.getX(), cuboidBlockIterator.getY(), cuboidBlockIterator.getZ());
            var blockEntity = world.getBlockEntity(mutablePos);

            // Note: container.getLootTableId() is null if the container is player-placed or has been opened by a player.
            if (blockEntity instanceof AccessorLootableContainerBlockEntity container
                && !(blockEntity instanceof SidedInventory)
                && container.getLootTableId() == null)
            {
                var blockState = blockEntity.getCachedState();
                // We add the inventory/double inventory for chest blocks, so long as the chest can be opened and is not
                // the "second chest" of a double chest (to prevent double counting).
                if (blockState.getBlock() instanceof ChestBlock
                    && (ChestBlock.isChestBlocked(world, mutablePos)
                    || ChestBlock.getDoubleBlockType(blockState) == DoubleBlockProperties.Type.SECOND))
                {
                    continue;
                }
                nearbyInventories.add(InventoryInfo.create(blockEntity));
            }
        }

        var entitySearchBox = Box.of(pos.toCenterPos(), radius * 2, radius * 2, radius * 2);
        world.getEntitiesByType(TypeFilter.instanceOf(Entity.class), entitySearchBox, entity -> {
            return entity instanceof RideableInventory || entity instanceof Inventory;
        }).forEach(entity -> {
            if (entity instanceof AbstractHorseEntity) // {
                nearbyInventories.add(InventoryInfo.create((AbstractHorseEntity) entity));
            // } else if (entity instanceof Inventory) {
            //     nearbyInventories.add(InventoryInfo.create((Inventory) entity));
            // }
        });

        return nearbyInventories;
    }



    /**
     * @return A mapping between each item in a player's inventory and a queue for each container (within a radius) that
     *         contains the item.
     */
    private HashMap<Item, Queue<InventoryInfo>> generateMappings(ServerPlayerEntity player, int searchRadius) {
        var itemContainerMap = new HashMap<Item, Queue<InventoryInfo>>();
        var playerInventory = player.getInventory();
        var serverWorld = player.getWorld();

        var nearbyInventories = findNearbyInventories(serverWorld, player.getBlockPos(), searchRadius);
        // For each unique item in the player's inventory, create a corresponding priority queue in the
        // container map prioritizing the inventory with the most free slots (with respect to the item).
        var uniquePlayerItems = InventoryUtil.getUniqueItems(playerInventory, 9, 35);
        uniquePlayerItems.forEach(item -> itemContainerMap.put(item, new PriorityQueue<>(Comparator.comparingInt(
            inventoryInfo -> -InventoryUtil.getAvailableSlots(inventoryInfo.sourceInventory(), item)))));

        if (itemContainerMap.isEmpty()) return itemContainerMap;

        // For each nearby inventory, add the inventory to all queues in the container map which correspond to
        // an item which exists in said inventory.
        nearbyInventories.forEach(inventoryInfo -> {
            // Only consider the intersection of items between the target inventory and player inventory.
            var intersection = InventoryUtil.getUniqueItems(inventoryInfo.sourceInventory());

            intersection.retainAll(uniquePlayerItems);
            intersection.forEach(item -> {
                if (itemContainerMap.containsKey(item)) itemContainerMap.get(item).add(inventoryInfo);
            });
        });
        return itemContainerMap;
    }



    /**
     * Initiates a quick stack operation on a player's inventory between all chests in the specified radius.
     */
    public void quickStack(ServerPlayerEntity player, int radius, boolean includeHotbar) {
        var itemContainerMap = generateMappings(player, radius);
        var pathFinder = new PathFinder(player.getWorld(), player.getBlockPos(), radius);
        var playerInventory = player.getInventory();

        if (itemContainerMap.isEmpty()) return;

        // For each item in the player's *main* inventory try to insert the item into the inventory of the associated
        // queue.
        int totalItemsDeposited = 0;
        var slotUsageMap = new HashMap<Integer, List<InventoryInfo>>();
        var itemUsageMap = new HashMap<Item, Pair<Integer, ItemStack>>();
        var containersUsed = new HashSet<InventoryInfo>();
        var startingSlot = includeHotbar ? 0 : PlayerInventory.getHotbarSize();
        for (var slotId = startingSlot; slotId < PlayerInventory.MAIN_SIZE; ++slotId) {
            var itemStack = playerInventory.getStack(slotId);
            var item = itemStack.getItem();
            var containerQueue = itemContainerMap.get(item);
            var associatedInventories = new ArrayList<InventoryInfo>(1);
            int stackSpread = 1, itemsDeposited = 0;

            if (containerQueue == null || CompatItemFavorites.isFavorite(itemStack)) continue;

            while (itemStack.getCount() != 0 && !containerQueue.isEmpty() && stackSpread++ < MAX_STACK_SPREAD) {
                var inventoryInfo = containerQueue.peek(); // Per-quick stack, only remove if full.
                var blockCenter = Vec3d.ofCenter(inventoryInfo.sourcePosition());
                var originalCount = itemStack.getCount();

                // If the inventory can't insert the entire stack, remove it from consideration and repeat
                // the process with the new inventory.
                //   * Note: We allow a singular stack to spread across a maximum of MAX_STACK_SPREAD containers.
                if (!pathFinder.hasNearLineOfSight(blockCenter, player.getPos().add(0, 1.5, 0)) ||
                    !InventoryUtil.insert(playerInventory, inventoryInfo.sourceInventory(), slotId))
                {
                    containerQueue.poll();
                }
                // If the entire stack wasn't inserted, the remaining stack will be used again; otherwise, it will
                // just be used for deposited items/container calculations.
                itemStack = playerInventory.getStack(slotId);
                if (originalCount != itemStack.getCount()) {
                    itemsDeposited += originalCount - itemStack.getCount();

                    itemUsageMap.putIfAbsent(item, new Pair<>(0, inventoryInfo.icon()));
                    associatedInventories.add(inventoryInfo);
                }
            }

            if (itemsDeposited > 0) {
                var itemUsageInfo = itemUsageMap.get(item);

                slotUsageMap.put(slotId, associatedInventories);
                containersUsed.addAll(associatedInventories);
                itemUsageInfo.setLeft(itemUsageInfo.getLeft() + itemsDeposited);
                totalItemsDeposited += itemsDeposited;
            }
        }

        // Sending the accumulated deposit results back to the client.
        if (totalItemsDeposited == 0) return;

        var totalContainersUsed = new HashSet<>(slotUsageMap.values()).size();
        QuickStack.LOGGER.info("{} quick stacked {} item{} into {} container{}",
            player.getName().getString(),
            totalItemsDeposited, (totalItemsDeposited > 1 ? "s" : ""),
            totalContainersUsed, (totalContainersUsed > 1 ? "s" : ""));

        S2CPacketDepositResult.send(slotUsageMap, player);
        S2CPacketToastResult.send(itemUsageMap, totalItemsDeposited, totalContainersUsed, player);

        // Play up to a maximum of two sound instances based on deposited container counts to prevent spam.
        for (var i = 0; i < Math.min(totalContainersUsed, 2) - 1; ++i) {
            player.playSound(
                SoundEvents.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, player.world.random.nextFloat() * 0.1f + 0.9f);
        }
    }
}
