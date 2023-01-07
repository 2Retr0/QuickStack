package retr0.quickstack.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import retr0.quickstack.QuickStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.util.Formatting.WHITE;

public class ColorManager {
    private static final long DURATION = 5000L;
    public static final HashMap<BlockPos, Integer> BLOCK_ENTITY_COLOR_MAP = new HashMap<>();
    public static final HashMap<BlockPos, Pair<Integer, BlockState>> MODEL_COLOR_MAP = new HashMap<>();
    public static final HashMap<Integer, Integer> SLOT_COLOR_MAP = new HashMap<>();

    private static final ColorQueue colorQueue = new ColorQueue();
    private static long lastUpdatedTimeMs = -1L;
    public static boolean finished = true;
    public static boolean waitForScreenClose = false;

    static {
        ClientTickEvents.START_WORLD_TICK.register(clientWorld -> {
            // Begin tracking
            if (waitForScreenClose && MinecraftClient.getInstance().currentScreen == null) {
                waitForScreenClose = false;
                beginTracking();
            }

            if (!finished && Util.getMeasuringTimeMs() - lastUpdatedTimeMs > DURATION) {
                finished = true;
                BLOCK_ENTITY_COLOR_MAP.clear();
                MODEL_COLOR_MAP.clear();
                SLOT_COLOR_MAP.clear();
                colorQueue.reset();
            }
        });
    }


    // get colors
    public static void addMappings(MinecraftClient client, Map<BlockPos, List<Integer>> containerSlotMap) {
        // If container already has color, get all slots and add color
        containerSlotMap.forEach((containerPos, associatedSlots) -> {
            QuickStack.LOGGER.info("Considering Pos: " + containerPos);
            int color;

            var blockState = client.player.world.getBlockState(containerPos);
            if (blockState.getRenderType() == BlockRenderType.MODEL) {
                color = MODEL_COLOR_MAP.computeIfAbsent(containerPos,
                    pos -> new Pair<>(colorQueue.getNextColor(), blockState)).getLeft();
            } else {
                color = BLOCK_ENTITY_COLOR_MAP.computeIfAbsent(containerPos, pos -> colorQueue.getNextColor());

                // Force update the color of the second chest if the container is a double chest.
                // Note: getFacing() points in the direction of the second chest for the double chest.
                if (blockState.getBlock() instanceof ChestBlock &&
                    ChestBlock.getDoubleBlockType(blockState) == DoubleBlockProperties.Type.FIRST)
                {
                    BLOCK_ENTITY_COLOR_MAP.put(containerPos.add(ChestBlock.getFacing(blockState).getVector()), color);
                }
            }
            associatedSlots.forEach(slot -> SLOT_COLOR_MAP.putIfAbsent(slot, color));
        });

        // If any screen is open, wait for it to exit before starting countdown.
        if (client.currentScreen != null)
            waitForScreenClose = true;
        else
            beginTracking();
    }



    private static void beginTracking() {
        lastUpdatedTimeMs = Util.getMeasuringTimeMs();
        finished = false;
    }



    @SuppressWarnings("DataFlowIssue")
    private static class ColorQueue {
        private final int[] colorQueue = {
            // RED.getColorValue(),  GOLD.getColorValue(),        YELLOW.getColorValue(),    GREEN.getColorValue(),
            // BLUE.getColorValue(), DARK_PURPLE.getColorValue(), DARK_AQUA.getColorValue(), WHITE.getColorValue()
            0xDC315D, 0xE5AB4C, 0x6AD6A1, 0x3C62D5, 0xD34DC0, WHITE.getColorValue()
        };
        private int currentIndex = 0;

        public int getNextColor() { return colorQueue[currentIndex++ % colorQueue.length]; }

        public void reset() { currentIndex = 0; }
    }
}
