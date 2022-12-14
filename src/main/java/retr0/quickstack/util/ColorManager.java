package retr0.quickstack.util;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.util.Formatting.*;

public class ColorManager {
    public static final HashMap<BlockPos, Integer> CONTAINER_COLOR_MAP = new HashMap<>();
    public static final HashMap<Integer, Integer> SLOT_COLOR_MAP = new HashMap<>();

    private static final ColorQueue colorQueue = new ColorQueue();

    // get colors
    public static void addMappings(Map<BlockPos, List<Integer>> containerSlotMap) {
        // If container already has color, get all slots and add color
        containerSlotMap.forEach((containerPos, associatedSlots) -> {
            int color;
            if (!CONTAINER_COLOR_MAP.containsKey(containerPos)) {
                color = colorQueue.getNextColor();
                CONTAINER_COLOR_MAP.put(containerPos, color);
            } else
                color = CONTAINER_COLOR_MAP.get(containerPos);

            associatedSlots.forEach(slot -> SLOT_COLOR_MAP.putIfAbsent(slot, color));
        });
    }

    // get alpha (5 second transition)
    // flush cached colors


    @SuppressWarnings("DataFlowIssue")
    private static class ColorQueue {
        private final int[] colorQueue = {
            RED.getColorValue(),  GOLD.getColorValue(),        YELLOW.getColorValue(),    GREEN.getColorValue(),
            BLUE.getColorValue(), DARK_PURPLE.getColorValue(), DARK_AQUA.getColorValue(), WHITE.getColorValue()
        };
        // private final int[] colorQueue = { WHITE.getColorValue() };
        private int currentIndex = 0;

        public int getNextColor() { return colorQueue[currentIndex++ % colorQueue.length]; }
    }
}
