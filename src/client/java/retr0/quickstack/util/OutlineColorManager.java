package retr0.quickstack.util;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import retr0.quickstack.util.InventoryUtil.InventorySource.SourceType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

@Environment(EnvType.CLIENT)
public final class OutlineColorManager {
    private static final long DURATION_MS = 5000L; // Interval (upon closing the current screen) to render outlines.
    private static final Integer[] COLORS = new Integer[]{ 0xDC315D, 0xE5AB4C, 0x6AD6A1, 0x3C62D5, 0xD34DC0, 0xFFFFFF };

    private static OutlineColorManager instance;

    private final MinecraftClient client;
    private final FixedQueue<Integer> colorQueue = new FixedQueue<>(COLORS);
    private final Object2IntMap<BlockPos> blockColorMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> entityColorMap = new Object2IntOpenHashMap<>();
    private final Int2IntOpenHashMap slotColorMap = new Int2IntOpenHashMap();

    private long lastStartedTimeMs = -1L;
    private boolean isRendering = false;
    private boolean waitForScreenClose = false;

    private OutlineColorManager(MinecraftClient client) {
        this.client = client;
    }



    public static void register() {
        if (OutlineColorManager.instance == null) {
            instance = new OutlineColorManager(MinecraftClient.getInstance());
            ClientTickEvents.START_WORLD_TICK.register(clientWorld -> instance.tick());
        }
    }



    /**
     * Updates timers for rendering outlines/resetting mappings. Should be called every world tick.
     */
    private void tick() {
        if (!isRendering) return;

        if (waitForScreenClose && client.currentScreen == null)
            startRendering();
        else if (!waitForScreenClose && Util.getMeasuringTimeMs() - lastStartedTimeMs > DURATION_MS)
            stopRendering();
    }



    /**
     * Sets up the render countdown and begins rendering block outlines.
     */
    private void startRendering() {
        isRendering = true;
        // If any screen is open, wait for it to exit before starting countdown.
        if (client.currentScreen == null) {
            waitForScreenClose = false;
            lastStartedTimeMs = Util.getMeasuringTimeMs();
            // slotColorMap.clear();
        } else
            waitForScreenClose = true;
    }



    /**
     * Resets any generated mappings, color queue, and stops rendering block outlines.
     */
    private void stopRendering() {
        isRendering = false;
        blockColorMap.clear();
        entityColorMap.clear();
        slotColorMap.clear();
        colorQueue.reset();
    }



    public static OutlineColorManager getInstance() {
        return instance;
    }



    public boolean isRendering() {
        return isRendering;
    }



    public void forEachBlock(BiConsumer<? super BlockPos, ? super Integer> consumer) {
        blockColorMap.forEach(consumer);
    }



    public int getBlockOutlineColor(BlockPos blockPos) {
        return blockColorMap.getInt(blockPos);
    }



    public int getEntityOutlineColor(UUID uuid) {
        return entityColorMap.getInt(uuid);
    }



    public int getSlotOutlineColor(int slotId) {
        return slotColorMap.get(slotId);
    }



    public void addMappings(World world, Map<Integer, List<InventoryUtil.InventorySource<?>>> slotUsageMap) {
        // If container already has color, get all slots and add color
        slotUsageMap.forEach((slotId, inventorySourceList) -> {
            inventorySourceList.forEach(inventorySource -> {
                var sourceType = inventorySource.sourceType();
                var color = -1;

                if (sourceType == SourceType.BLOCK_ENTITY) {
                    var sourcePosition = (BlockPos) inventorySource.instance();
                    var blockState = world.getBlockState(sourcePosition);

                    color = blockColorMap.computeIfAbsent(sourcePosition, blockPos -> colorQueue.getNext());
                    // Also include second chest if block is a double chest.
                    //   * Note: getFacing() points in the direction of the second chest for the double chest.
                    if (blockState.getBlock() instanceof ChestBlock &&
                        ChestBlock.getDoubleBlockType(blockState) != DoubleBlockProperties.Type.SINGLE)
                    {
                        var neighborChestPosition = sourcePosition.add(ChestBlock.getFacing(blockState).getVector());
                        blockColorMap.putIfAbsent(neighborChestPosition, color);
                    }
                } else if (sourceType == SourceType.INVENTORY_ENTITY) {
                    var sourceUuid = (UUID) inventorySource.instance();

                    color = entityColorMap.computeIfAbsent(sourceUuid, uuid -> colorQueue.getNext());
                }
                slotColorMap.putIfAbsent((int) slotId, color);
            });
        });
        startRendering();
    }

    /**
     * A fixed-size queue of entries of which can be circularly-iterated.
     */
    private static class FixedQueue<T> {
        private final T[] queue;
        private int currentIndex = 0;

        public FixedQueue(T[] items) { queue = items; }

        /**
         * @return The next entry to be polled in the queue. The first entry will be returned if the previous entry
         * polled was the last entry in the queue.
         */
        public T getNext() { return queue[currentIndex++ % queue.length]; }

        /**
         * Resets the instance such that the next element polled would be the first element in the queue.
         */
        public void reset() { currentIndex = 0; }
    }
}
