package retr0.quickstack.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import retr0.quickstack.QuickStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.block.DoubleBlockProperties.Type.FIRST;

@Environment(EnvType.CLIENT)
public class OutlineRenderManager {
    private static final long DURATION_MS = 5000L; // Interval (upon closing the current screen) to render outlines.
    private static final Integer[] COLORS = new Integer[]{ 0xDC315D, 0xE5AB4C, 0x6AD6A1, 0x3C62D5, 0xD34DC0, 0xFFFFFF };

    public static final OutlineRenderManager INSTANCE = new OutlineRenderManager(MinecraftClient.getInstance());

    private final FixedQueue<Integer> colorQueue = new FixedQueue<>(COLORS);
    private final MinecraftClient client;

    public final HashMap<BlockPos, Pair<Integer, BlockState>> blockModelColorMap = new HashMap<>();
    public final HashMap<BlockPos, Integer> blockEntityColorMap = new HashMap<>();
    public final HashMap<Integer, Integer> slotColorMap = new HashMap<>();

    private long lastStartedTimeMs = -1L;
    private boolean isRendering = false;
    private boolean waitForScreenClose = false;

    private OutlineRenderManager(MinecraftClient client) {
        this.client = client;
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

    public boolean isRendering() { return isRendering; }


    // get colors
    public void addMappings(World world, Map<BlockPos, List<Integer>> containerSlotMap) {
        // If container already has color, get all slots and add color
        containerSlotMap.forEach((containerPos, associatedSlots) -> {
            QuickStack.LOGGER.info("Considering Pos: " + containerPos);
            int color;
            var blockState = world.getBlockState(containerPos);

            switch (blockState.getRenderType()) {
                case MODEL -> {
                    color = blockModelColorMap.computeIfAbsent(containerPos,
                        pos -> new Pair<>(colorQueue.getNext(), blockState)).getLeft();
                }
                case ENTITYBLOCK_ANIMATED -> {
                    color = blockEntityColorMap.computeIfAbsent(containerPos, pos -> colorQueue.getNext());

                    var block = blockState.getBlock();
                    // Also update the color of the second chest if the container is a double chest.
                    //   * Note: getFacing() points in the direction of the second chest for the double chest.
                    if (block instanceof ChestBlock && ChestBlock.getDoubleBlockType(blockState) == FIRST) {
                        blockEntityColorMap.put(containerPos.add(ChestBlock.getFacing(blockState).getVector()), color);
                    }
                }
                default -> {
                    QuickStack.LOGGER.error("Encountered unexpected render type while generating outline color mappings!");
                    return;
                }
            }
            associatedSlots.forEach(slot -> slotColorMap.putIfAbsent(slot, color));
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
        blockEntityColorMap.clear();
        blockModelColorMap.clear();
        slotColorMap.clear();
        colorQueue.reset();
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
