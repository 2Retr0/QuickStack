package retr0.itemfavoriting;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemFavoriting implements ClientModInitializer {
	public static final String MOD_ID = "itemfavoriting";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Item favoriting should stop:
		//  * drops from clicking outside the inventory
		//  * pressing q
		//  * quick moving to a container
		//  * quick stacking
		// Item favoriting (slot favorite) should remain if:
		//  * item is not picked up, and items are taken from the stack and doesn't result in an empty stack
		//  * favorited item is placed down to a new slot in inventory and is not an empty stack (move
		//    slot favorite to new slot)
		//     - If last item in stack is picked up, move slot favorite to final place down location (left-click).
		//     - dragging all items until empty stack will destroy item favorite
		//  * quick moved to new slot in inventory (move slot favorite to new slot)
		//  * double clicking ismilar items to stack all!
	}
}
