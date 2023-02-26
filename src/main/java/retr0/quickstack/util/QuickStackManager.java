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
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import retr0.itemfavorites.ItemFavorites;
import retr0.itemfavorites.extension.ExtensionItemStack;
import retr0.quickstack.QuickStack;
import retr0.quickstack.mixin.AccessorLootableContainerBlockEntity;
import retr0.quickstack.network.S2CPacketDepositResult;
import retr0.quickstack.network.S2CPacketToastResult;

import java.util.*;

public class QuickStackManager {
    public static final QuickStackManager INSTANCE = new QuickStackManager();

    private static final int PATHFINDING_RADIUS = 8;
    private static final int SEARCH_RADIUS = 8;
    private static final int MAX_STACK_SPREAD = 3;
    private static final int SOUND_PLAY_DELAY_MS = 40;
    private static final boolean ALLOW_HOTBAR = false;

    private final Deque<PlayerEntity> queuedSoundInstances = new ArrayDeque<>();
    private long previousSoundPlayTimeMs = -1;

    public void tick() {
        var currentTimeMs = Util.getMeasuringTimeMs();
        if (queuedSoundInstances.isEmpty() || currentTimeMs - previousSoundPlayTimeMs < SOUND_PLAY_DELAY_MS)
            return;

        playSound(queuedSoundInstances.poll(), 1);
        previousSoundPlayTimeMs = currentTimeMs;
    }



    // TODO: Redo sound logic in some future version! Using a queue for **all** players on the server is very bad!
    private void playSound(PlayerEntity player, int times) {
        var emitPos = player.getEyePos().subtract(player.getRotationVector().multiply(3));
        player.world.playSound(null, emitPos.x, emitPos.y, emitPos.z, SoundEvents.BLOCK_BARREL_CLOSE,
            SoundCategory.BLOCKS, 0.5f, player.world.random.nextFloat() * 0.1f + 0.9f);

        // Add remaining play times to the sound queue to be played at an offset.
        for (var i = 0; i < times - 1; ++i) queuedSoundInstances.add(player);
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



    private HashMap<Item, Queue<InventoryInfo>> generateMappings(ServerPlayerEntity player) {
        var itemContainerMap = new HashMap<Item, Queue<InventoryInfo>>();
        var playerInventory = player.getInventory();
        var serverWorld = player.getWorld();

        var nearbyInventories = findNearbyInventories(serverWorld, player.getBlockPos(), SEARCH_RADIUS);
        // For each unique item in the player's inventory, create a corresponding priority queue in the
        // container map prioritizing the inventory with the most free slots (with respect to the item).
        var uniquePlayerItems = InventoryUtil.getUniqueItems(playerInventory, 9, 35);
        uniquePlayerItems.forEach(item -> {
            itemContainerMap.put(item, new PriorityQueue<>(Comparator.comparingInt(
                inventoryInfo -> -InventoryUtil.getAvailableSlots(inventoryInfo.inventory(), item))));
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
        return itemContainerMap;
    }



    /**
     * Initiates a quick stack operation for the specified player considering all chests within the specified radius
     * for depositing.
     */
    public void quickStack(ServerPlayerEntity player) {
        var dryRun = true;
        var itemContainerMap = generateMappings(player);
        var playerInventory = player.getInventory();
        var serverWorld = player.getWorld();

        if (itemContainerMap.isEmpty()) return;

        // For each item in the player's *main* inventory try to insert the item into the inventory of the associated
        // queue.
        var depositCount = 0;
        var containerCount = 0;
        var depositResultPacket = new S2CPacketDepositResult();
        var toastResultPacket = new S2CPacketToastResult();
        var pathFinder = new PathFinder(serverWorld, PATHFINDING_RADIUS, player.getBlockPos());
        for (var slot = PlayerInventory.getHotbarSize(); slot < PlayerInventory.MAIN_SIZE; ++slot) {
            var itemStack = playerInventory.getStack(slot);
            var containerQueue = itemContainerMap.get(itemStack.getItem());
            var stackSpread = 1;

            // noinspection DataFlowIssue //
            if (containerQueue == null || (FabricLoader.getInstance().isModLoaded(ItemFavorites.MOD_ID) &&
                ((ExtensionItemStack) (Object) itemStack).isFavorite()))
            {
                continue;
            }

            while (itemStack.getCount() != 0 && !containerQueue.isEmpty() && stackSpread++ < MAX_STACK_SPREAD) {
                var inventoryInfo = containerQueue.peek(); // Per-quick stack, only remove if full.
                var blockCenter = Vec3d.ofCenter(inventoryInfo.blockPos());
                var originalCount = itemStack.getCount();
                var originalItem = itemStack.getItem();

                // If the inventory can't insert the entire stack, remove it from consideration and repeat
                // the process with the new inventory.
                //   * Note: We allow a singular stack to spread across a maximum of MAX_STACK_SPREAD containers.
                if (!pathFinder.hasNearLineOfSight(blockCenter, player.getPos().add(0, 1.5, 0)) ||
                    !InventoryUtil.insert(playerInventory, inventoryInfo.inventory(), slot))
                {
                    containerQueue.poll();
                }
                // If the entire stack wasn't inserted, the remaining stack will be used again; otherwise, it will
                // just be used for deposited items/container calculations.
                itemStack = playerInventory.getStack(slot);
                if (originalCount != itemStack.getCount()) {
                    depositCount += originalCount - itemStack.getCount();

                    depositResultPacket.updateContainerSlots(inventoryInfo.blockPos, slot);
                    toastResultPacket.updateDepositAmount(originalItem, depositCount, inventoryInfo.blockPos, inventoryInfo.icon);
                }
            }

            containerCount = depositResultPacket.getDepositedContainerCount();
        }

        // Sending the accumulated deposit results back to the client.
        if (depositCount > 0) {
            QuickStack.LOGGER.info("{} quick stacked {} item{} into {} container{}",
                player.getName().getString(),
                depositCount, (depositCount > 1 ? "s" : ""),
                containerCount, (containerCount > 1 ? "s" : ""));

            S2CPacketToastResult.send(toastResultPacket, player);
            S2CPacketDepositResult.send(depositResultPacket, player);

            // Play up to a maximum of two sound instances for deposited container counts > 1 to prevent spam.
            playSound(player, Math.min(containerCount, 2));
        }
    }



    /**
     * Record containing an inventory, the {@link BlockPos} of its respective block entity, and an {@link ItemStack}
     * representing said block entity.
     */
    private record InventoryInfo(Inventory inventory, BlockPos blockPos, ItemStack icon) { }
}
