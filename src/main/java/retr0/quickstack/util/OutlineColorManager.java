package retr0.quickstack.util;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import retr0.quickstack.QuickStack;

import java.util.*;
import java.util.function.BiConsumer;

import static net.minecraft.block.DoubleBlockProperties.Type.FIRST;

@Environment(EnvType.CLIENT)
public final class OutlineColorManager {
    private static final long DURATION_MS = 5000L; // Interval (upon closing the current screen) to render outlines.
    private static final Integer[] COLORS = new Integer[]{ 0xDC315D, 0xE5AB4C, 0x6AD6A1, 0x3C62D5, 0xD34DC0, 0xFFFFFF };

    private static OutlineColorManager instance;

    private final MinecraftClient client;
    private final FixedQueue<Integer> colorQueue = new FixedQueue<>(COLORS);
    private final Object2IntMap<BlockPos> blockColorMap = new Object2IntOpenHashMap<>();
    private final Int2IntOpenHashMap slotColorMap = new Int2IntOpenHashMap();

    private long lastStartedTimeMs = -1L;
    private boolean isRendering = false;
    private boolean waitForScreenClose = false;

    public static void register() {
        if (OutlineColorManager.instance == null) {
            instance = new OutlineColorManager(MinecraftClient.getInstance());
            ClientTickEvents.START_WORLD_TICK.register(clientWorld -> instance.tick());
        }
    }

    public static OutlineColorManager getInstance() {
        return instance;
    }

    /**
     * Updates timers for rendering outlines/resetting mappings. Should be called every world tick.
     */
    public void tick() {
        if (!isRendering) return;

        if (waitForScreenClose && client.currentScreen == null)
            startRendering();
        else if (!waitForScreenClose && Util.getMeasuringTimeMs() - lastStartedTimeMs > DURATION_MS)
            stopRendering();
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

    public int getSlotOutlineColor(int slotId) {
        return slotColorMap.get(slotId);
    }



    // get colors
    public void addMappings(World world, Map<BlockPos, List<Integer>> containerSlotMap) {
        // If container already has color, get all slots and add color
        containerSlotMap.forEach((containerPos, associatedSlots) -> {
            QuickStack.LOGGER.info("Considering Pos: " + containerPos);
            var blockState = world.getBlockState(containerPos);
            int color = blockColorMap.computeIfAbsent(containerPos, pos -> colorQueue.getNext());

            // Also update the color of the second chest if the container is a double chest.
            //   * Note: getFacing() points in the direction of the second chest for the double chest.
            if (blockState.getRenderType() == BlockRenderType.ENTITYBLOCK_ANIMATED) {
                var block = blockState.getBlock();
                if (block instanceof ChestBlock && ChestBlock.getDoubleBlockType(blockState) == FIRST)
                    blockColorMap.put(containerPos.add(ChestBlock.getFacing(blockState).getVector()), color);
            }

            // Ensure color is not zero as that is reserved as the null value.
            associatedSlots.forEach(slot -> slotColorMap.putIfAbsent((int) slot, color == 0 ? color + 1 : color));
        });
        startRendering();
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
        slotColorMap.clear();
        colorQueue.reset();
    }

    private OutlineColorManager(MinecraftClient client) {
        this.client = client;
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
