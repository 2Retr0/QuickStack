package retr0.quickstack.util;

import net.minecraft.item.ItemStack;

/**
 * Record containing two {@link ItemStack}s, representing a deposited item's icon and its deposited container's icon
 * respectively.
 */
public record IconPair(ItemStack itemIcon, ItemStack containerIcon) { }
