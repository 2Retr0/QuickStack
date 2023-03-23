package retr0.quickstack.compat.itemfavorites;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import retr0.itemfavorites.extension.ExtensionItemStack;

/**
 * Wrapper class for ItemFavorites compatibility.
 */
public final class CompatItemFavorites {
    public static boolean isFavorite(ItemStack itemStack) {
        if (!FabricLoader.getInstance().isModLoaded("itemfavorites")) return false;

        // noinspection DataFlowIssue
        return ((ExtensionItemStack) (Object) itemStack).isFavorite();
    }
}
