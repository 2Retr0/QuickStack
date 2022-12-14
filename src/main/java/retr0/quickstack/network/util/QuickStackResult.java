package retr0.quickstack.network.util;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

import java.util.List;

public record QuickStackResult(int depositCount, int containerCount, List<IconMapping> iconMappings) {
    /**
     * @return A {@link PacketByteBuf} containing the contents of a specified {@code QuickStackResult} with the
     * following format:
     * <ul>
     *    <li>Three {@code int}s, representing the deposit count, container count, and the amount ({@code n}) of icon
     *        mappings respectively.</li>
     *    <li>{@code n} pairs of {@link ItemStack}s representing {@link IconMapping}s.</li>
     * </ul>
     */
    public static PacketByteBuf createByteBuf(QuickStackResult quickStackResult) {
        var buf = PacketByteBufs.create();

        buf.writeInt(quickStackResult.depositCount());
        buf.writeByte(quickStackResult.containerCount());
        buf.writeByte(quickStackResult.iconMappings().size());
        quickStackResult.iconMappings().forEach(iconMapping -> {
            buf.writeItemStack(iconMapping.itemIcon());
            buf.writeItemStack(iconMapping.containerIcon());
        });

        return buf;
    }

    /**
     * Record containing two {@link ItemStack}s, representing a deposited item's icon and its deposited container's icon
     * respectively.
     */
    public record IconMapping(ItemStack itemIcon, ItemStack containerIcon) { }


    // map slots to colors
}
