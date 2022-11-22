package retr0.quickstack.util;

import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

public final class InventoryUtil {
    /**
     * @see InventoryUtil#getUniqueItems(Inventory, int, int)
     */
    public static Set<Item> getUniqueItems(Inventory inventory) {
        return getUniqueItems(inventory, 0, inventory.size() - 1);
    }



    /**
     * @return A {@code Set} containing all unique (NBT-disregarded) items in {@code inventory} within the slot bounds.
     */
    public static Set<Item> getUniqueItems(Inventory inventory, int fromSlot, int toSlot) {
        var uniqueItems = new HashSet<Item>();

        // Return the empty set if the lower-bound is beyond the upper-bound.
        if (fromSlot > toSlot) return uniqueItems;

        for (var slot = fromSlot; slot <= Math.min(toSlot, inventory.size() - 1); ++slot) {
            var itemStack = inventory.getStack(slot);

            if (!itemStack.equals(ItemStack.EMPTY)) uniqueItems.add(itemStack.getItem());
        }
        return uniqueItems;
    }



    /**
     * @return The number of slots in {@code inventory} which are empty or have a non-full stack of {@code targetItem}.
     */
    public static int getAvailableSlots(Inventory inventory, Item targetItem) {
        var available = 0;

        for (var slot = 0; slot < inventory.size(); ++slot) {
            var itemStack = inventory.getStack(slot);

            if (itemStack.equals(ItemStack.EMPTY) ||
                (itemStack.getItem().equals(targetItem) && itemStack.getCount() < itemStack.getMaxCount()))
            {
                ++available;
            }
        }
        return available;
    }



    /**
     * Attempts to insert the {@code fromSlot} item stack in inventory {@code from} into inventory {@code to} with a slot
     * prioritization similar to shift-clicking from a player's inventory.
     * @see HopperBlockEntity#transfer(Inventory, Inventory, ItemStack, Direction)
     *
     * @return {@code true} if the item stack was fully inserted into inventory {@code to}; otherwise, {@code false}
     * (e.g., if inventory {@code to} because full before the item stack was emptied).
     */
    public static boolean insert(Inventory from, Inventory to, int fromSlot) {
        // TODO: NOT BEING MARKED DIRTY ENOUGH? DESYNC IN STACK COUNT OVER RESTARTS
        var targetStack = from.getStack(fromSlot);

        if (to == null || targetStack.equals(ItemStack.EMPTY) || getAvailableSlots(to, targetStack.getItem()) == 0)
            return false;

        var originalCount = targetStack.getCount();
        var remainingStack = HopperBlockEntity.transfer(null, to, from.removeStack(fromSlot), null);

        // Mark both inventories dirty if any items transferred.
        if (remainingStack.getCount() != originalCount) {
            to.markDirty();
            from.markDirty();
        }
        // Return true if the stack was fully transferred; otherwise, add the remaining stack back into inventory 'from'
        // and return false.
        if (remainingStack.isEmpty()) return true;

        from.setStack(fromSlot, remainingStack);
        return false;
    }

    private InventoryUtil() { }
}
