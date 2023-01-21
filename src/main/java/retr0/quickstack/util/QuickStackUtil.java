package retr0.quickstack.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import retr0.itemfavorites.extension.ExtensionItemStack;
import retr0.quickstack.QuickStack;
import retr0.quickstack.mixin.AccessorLootableContainerBlockEntity;
import retr0.quickstack.network.DepositResultS2CPacket;
import retr0.quickstack.network.ToastResultS2CPacket;

import java.util.*;

public final class QuickStackUtil {
    private static int MAX_STACK_SPREAD = 3;

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



    private static HashMap<Item, Queue<InventoryInfo>> generateMappings(ServerPlayerEntity player, int radius) {
        var itemContainerMap = new HashMap<Item, Queue<InventoryInfo>>();
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
            itemContainerMap.put(item, new PriorityQueue<>(
                Comparator.comparingInt(inventoryInfo -> -InventoryUtil.getAvailableSlots(inventoryInfo.inventory(), item))));
            QuickStack.LOGGER.info(item.getTranslationKey());
        });

        if (itemContainerMap.isEmpty()) return itemContainerMap;

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
        return itemContainerMap;
    }


    /**
     * Initiates a quick stack operation for the specified player considering all chests within the specified radius
     * for depositing.
     */
    public static void quickStack(ServerPlayerEntity player, int radius) {
        var itemContainerMap = generateMappings(player, radius);
        var playerInventory = player.getInventory();
        var serverWorld = player.getWorld();

        if (itemContainerMap.isEmpty()) {
            QuickStack.LOGGER.info("Nothing found!");
            return;
        }

        // Second Click (if also on button) //
        // TODO: This can be done while the item is being animated traversing to the quickstack icon.
        // For each item in the player's *main* inventory try to insert the item into the head inventory of the
        // associated queue.
        var depositCount = 0;
        var depositResultPacket = new DepositResultS2CPacket();
        var toastResultPacket = new ToastResultS2CPacket();
        var pathFinder = new PathFinder(serverWorld, 8, player.getBlockPos());
        for (var slot = PlayerInventory.getHotbarSize(); slot < PlayerInventory.MAIN_SIZE; ++slot) {
            var itemStack = playerInventory.getStack(slot);
            var containerQueue = itemContainerMap.get(itemStack.getItem());
            var stackSpread = 1;

            if (containerQueue == null || (FabricLoader.getInstance().isModLoaded("itemfavorites") &&
                ((ExtensionItemStack) (Object) itemStack).isFavorite())) continue;

            QuickStack.LOGGER.info("Trying to Stack: " + itemStack.getItem().getName().getString());
            while (itemStack.getCount() != 0 && !containerQueue.isEmpty() && stackSpread++ < MAX_STACK_SPREAD) {
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

                    depositResultPacket.updateContainerSlots(headInfo.blockPos, slot);
                    toastResultPacket.updateDepositAmount(originalItem, depositCount, headInfo.blockPos, headInfo.icon);
                }
            }
        }

        // Get top 3 items and their respective inventories.
        // Note: In the case where an item is deposited into multiple containers, the container icon for the toast would
        //       be the highest priority container for the item (denoted by the entry in the item->container mappings)
        //       which is a good consequence of this overly-engineered design!
        var containerCount = depositResultPacket.getDepositedContainerCount();
        if (depositCount > 0) {
            QuickStack.LOGGER.info("{} quick stacked {} item{} into {} container{}",
                player.getName().getString(),
                depositCount, (depositCount > 1 ? "s" : ""),
                containerCount, (containerCount > 1 ? "s" : ""));

            ToastResultS2CPacket.send(toastResultPacket, player);
            DepositResultS2CPacket.send(depositResultPacket, player);
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

    private QuickStackUtil() { }
}
