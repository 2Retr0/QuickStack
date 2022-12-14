package retr0.quickstack.network.util;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record QuickStackColorResult(
    List<ColorMapping<BlockPos>> containerColorMap, List<ColorMapping<Integer>> slotColorMap)
{
    public QuickStackColorResult(int containerColorMapCount, int slotColorMapCount) {
        this(new ArrayList<>(containerColorMapCount), new ArrayList<>(slotColorMapCount));
    }

    public record ColorMapping<T>(T object, int color) { }
}
