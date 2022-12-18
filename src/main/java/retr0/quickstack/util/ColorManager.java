package retr0.quickstack.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties.Type;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.util.Formatting.WHITE;

public class ColorManager {
    public static final HashMap<BlockPos, Integer> BLOCK_ENTITY_COLOR_MAP = new HashMap<>();
    public static final HashMap<BlockPos, Pair<Integer, BlockState>> MODEL_COLOR_MAP = new HashMap<>();
    public static final HashMap<Integer, Integer> SLOT_COLOR_MAP = new HashMap<>();

    private static final ColorQueue colorQueue = new ColorQueue();
    private static long lastUpdatedTimeMs = -1L;
    public static boolean finished = true;

    static {
        ClientTickEvents.START_WORLD_TICK.register(clientWorld -> {
            if (Util.getMeasuringTimeMs() - lastUpdatedTimeMs < 6000L) return;

            BLOCK_ENTITY_COLOR_MAP.clear();
            MODEL_COLOR_MAP.clear();
            SLOT_COLOR_MAP.clear();
            finished = true;
        });
    }


    // get colors
    public static void addMappings(MinecraftClient client, Map<BlockPos, List<Integer>> containerSlotMap) {
        // If container already has color, get all slots and add color
        containerSlotMap.forEach((containerPos, associatedSlots) -> {
            int color;

            var blockState = client.player.world.getBlockState(containerPos);
            if (blockState.getRenderType() == BlockRenderType.MODEL) {
                color = MODEL_COLOR_MAP.computeIfAbsent(containerPos,
                    pos -> new Pair<>(colorQueue.getNextColor(), blockState)).getLeft();
            } else {
                color = BLOCK_ENTITY_COLOR_MAP.computeIfAbsent(containerPos, pos -> colorQueue.getNextColor());

                var block = blockState.getBlock();
                // Force update the color of the second chest if the container is a double chest.
                // Note: getFacing() points in the direction of the second chest for the double chest.
                if (block instanceof ChestBlock && ChestBlock.getDoubleBlockType(blockState) == Type.FIRST)
                    BLOCK_ENTITY_COLOR_MAP.put(containerPos.add(ChestBlock.getFacing(blockState).getVector()), color);
            }

            associatedSlots.forEach(slot -> SLOT_COLOR_MAP.putIfAbsent(slot, color));
        });
    }

    // get alpha (5 second transition)
    // flush cached colors


    @SuppressWarnings("DataFlowIssue")
    private static class ColorQueue {
        private final int[] colorQueue = {
            // RED.getColorValue(),  GOLD.getColorValue(),        YELLOW.getColorValue(),    GREEN.getColorValue(),
            // BLUE.getColorValue(), DARK_PURPLE.getColorValue(), DARK_AQUA.getColorValue(), WHITE.getColorValue()
            WHITE.getColorValue()
        };
        // private final int[] colorQueue = { WHITE.getColorValue() };
        private int currentIndex = 0;

        public int getNextColor() {
            ColorManager.lastUpdatedTimeMs = Util.getMeasuringTimeMs();
            ColorManager.finished = false;

            return colorQueue[currentIndex++ % colorQueue.length];
        }
    }
}
