package retr0.quickstack.util;

import net.minecraft.util.Formatting;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

public class ColorQueue {
    public static final Queue<Integer> COLORS = Arrays.stream(Formatting.values())
        .map(Formatting::getColorValue)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(ArrayDeque::new));

    private final Queue<Integer> localColorQueue = COLORS;

    @SuppressWarnings("DataFlowIssue")
    public int getNextColor() {
        var nextColor = localColorQueue.peek();
        localColorQueue.add(nextColor);
        return nextColor;
    }
}
