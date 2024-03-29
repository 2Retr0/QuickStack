package retr0.quickstack.util;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import retr0.quickstack.mixin.AccessorAbstractHorseEntity;
import retr0.quickstack.util.InventoryUtil.InventorySource.SourceType;

import java.util.HashSet;
import java.util.Set;

public final class InventoryUtil {
    private InventoryUtil() { }



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

            if (!itemStack.isEmpty()) uniqueItems.add(itemStack.getItem());
        }
        return uniqueItems;
    }



    /**
     * Attempts to insert the {@code fromSlot} item stack in inventory {@code from} into inventory {@code to} with a slot
     * prioritization similar to shift-clicking from a player's inventory.
     *
     * @return {@code true} if the item stack was fully inserted into inventory {@code to}; otherwise, {@code false}
     *     (e.g., if inventory {@code to} because full before the item stack was emptied).
     * @see HopperBlockEntity#transfer(Inventory, Inventory, ItemStack, Direction)
     */
    public static boolean insert(Inventory from, Inventory to, int fromSlot) {
        var targetStack = from.getStack(fromSlot);
        var originalCount = targetStack.getCount();

        if (to == null || targetStack.equals(ItemStack.EMPTY) || getAvailableSlots(to, targetStack.getItem()) == 0)
            return false;

        var remainingStack = HopperBlockEntity.transfer(null, to, from.removeStack(fromSlot), null);
        if (remainingStack.getCount() != originalCount) {
            to.markDirty();
            from.markDirty();
            from.setStack(fromSlot, remainingStack);
        }
        return remainingStack.isEmpty();
    }



    /**
     * @return The number of slots in {@code inventory} which are empty or have a non-full stack of {@code targetItem}.
     */
    public static int getAvailableSlots(Inventory inventory, Item targetItem) {
        var available = 0;
        for (var slot = 0; slot < inventory.size(); ++slot) {
            var itemStack = inventory.getStack(slot);
            var hasNonFullStack = itemStack.isOf(targetItem) && itemStack.getCount() < itemStack.getMaxCount();

            if (itemStack.isEmpty() || hasNonFullStack) ++available;
        }
        return available;
    }



    /**
     * Record containing an {@link Inventory}, the position of its respective instance, its instance object, and an
     * icon representing the instance.
     */
    public record InventoryInfo(
        Inventory sourceInventory, BlockPos sourcePosition, InventorySource<?> source, ItemStack icon)
    {
        public static InventoryInfo create(BlockEntity source) {
            var inventory = (Inventory) source;
            var inventorySource = new InventorySource<>(source, SourceType.BLOCK_ENTITY);

            var blockState = source.getCachedState();
            var block = blockState.getBlock();
            if (block instanceof ChestBlock chestBlock)
                inventory = ChestBlock.getInventory(chestBlock, blockState, source.getWorld(), source.getPos(), true);

            return new InventoryInfo(inventory, source.getPos(), inventorySource, block.asItem().getDefaultStack());
        }



        public static InventoryInfo create(AbstractDonkeyEntity source) {
            // TODO: Horse entity is bad hopper entity insert doesn't work with it!
            var inventory = ((AccessorAbstractHorseEntity) source).getItems();
            var inventorySource = new InventorySource<>(source, SourceType.INVENTORY_ENTITY);

            return new InventoryInfo(inventory, source.getBlockPos(), inventorySource, Items.SADDLE.getDefaultStack());
        }



        public static InventoryInfo create(VehicleInventory source) {
             var inventory = (Inventory) source;
             var pos3d = source.getPos();
             var pos3i = new BlockPos(MathHelper.floor(pos3d.x), MathHelper.floor(pos3d.y), MathHelper.floor(pos3d.z));
             var inventorySource = new InventorySource<>(source, SourceType.INVENTORY_ENTITY);

             var icon = Items.CHEST_MINECART.getDefaultStack();
             // TODO: This isn't a very robust way of getting the associated icon of an entity!
             if (source instanceof BoatEntity boat)
                 icon = boat.asItem().getDefaultStack();

             return new InventoryInfo(inventory, pos3i, inventorySource, icon);
        }
    }

    public record InventorySource<T>(T instance, SourceType sourceType) {
        public enum SourceType {BLOCK_ENTITY, INVENTORY_ENTITY}
    }
}
