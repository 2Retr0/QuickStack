package retr0.quickstack;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retr0.quickstack.util.InventoryUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;

public class QuickStack implements ModInitializer {
	public static final String MOD_ID = "quickstack";
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// WorldRenderEvents.LAST.register(context -> {
		// 	var matrices = context.matrixStack();
		// 	var bufferBuilders = ((MixinWorldRenderer) context.worldRenderer()).getBufferBuilders();
		// 	var vec3d = context.camera().getPos();
		// 	double x = vec3d.x, y = vec3d.y, z = vec3d.z;
		// 	var world = context.world();
		//
		//
		//
		// 	var renderManager = MinecraftClient.getInstance().getBlockRenderManager();
		// 	var pumpkinState = Blocks.CARVED_PUMPKIN.getDefaultState();
		// 	var pumpkinModel = renderManager.getModel(pumpkinState);
		//
		//
		// 	// LOGGER.info(pumpkinModelId.toString()); // BAD!
		//
		//
		// 	var textureId = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(new Identifier("textures/block/carved_pumpkin.png"));
		//
		// 	var random = Random.create();
		//
		// 	var pos = new BlockPos(300, 64, -150);
		//
		// 	matrices.push();
		// 	matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);
		//
		// 	var top = matrices.peek();
		//
		// 	// for (Direction direction : DIRECTIONS) {
		// 	// 	random.setSeed(42L);
		// 	// 	BlockModelRenderer.renderQuads(entry, vertexConsumer, red, green, blue, bakedModel.getQuads(state, direction, random), light, overlay);
		// 	// }
		//
		// 	var vertexConsumer = context.consumers().getBuffer(RenderLayer.getOutline(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
		//
		// 	MinecraftClient.getInstance().getBlockRenderManager().renderDamage(pumpkinState, pos, world, matrices, vertexConsumer);
		//
		// 	matrices.pop();
		//
		// 	// var vertexConsumer = context.consumers().getBuffer(RenderLayer.getOutline(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
		// 	//
		// 	// // ((MixinWorldRenderer) context.worldRenderer()).invokeDrawBlockOutline(matrices, vertexConsumer, context.camera().getFocusedEntity(), x, y, z, pos, world.getBlockState(pos));
		// 	//
		// 	// matrices.push();
		// 	// matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);
		// 	//
		// 	// WorldRenderer.drawBox(matrices, vertexConsumer, 0d, 0d, 0d, 1d, 1d, 1d, 1f, 1f, 1f, 1f);
		// 	//
		// 	// matrices.pop();
		//
		// 	/*var instance = ((MixinWorldRenderer) context.worldRenderer());
		//
		//
		// 	var focusedEntity = context.camera().getFocusedEntity();
		// 	var vec3d = context.camera().getPos();
		//
		// 	var vertexConsumer = context.consumers().getBuffer(RenderLayer.getOutline(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
		// 	var matrices = context.matrixStack();
		// 	var pos = new BlockPos(0, 0, 0);
		// 	var renderManager = MinecraftClient.getInstance().getBlockRenderManager();
		// 	var pumpkinState = Blocks.CARVED_PUMPKIN.getDefaultState();
		// 	var pumpkinModel = renderManager.getModel(pumpkinState);
		//
		// 	renderManager.getModelRenderer().render(world, pumpkinModel, pumpkinState, pos, matrices, vertexConsumer, false, Random.create(), pumpkinState.getRenderingSeed(pos), OverlayTexture.DEFAULT_UV);*/
		// });

		// TODO: BLOCK AND INVENTORY HIGHLIGHTING
		// TODO: PATH FINDING FOR VALID QUICKSTACK
		// TODO: ITEM FAVORITEING
		LOGGER.info("Hello Fabric world!");

		ServerPlayNetworking.registerGlobalReceiver(new Identifier(MOD_ID, "request_quick_stack"), ((server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				// TODO: Move to its own function!
				var itemContainerMap = new HashMap<Item, Queue<Inventory>>();
				var playerInventory = player.getInventory();

				// First Click //
				// TODO: Use pathfinding or something.
				var nearbyInventories = InventoryUtil.getNearbyInventories(server.getWorld(player.world.getRegistryKey()), player.getBlockPos(), 8);
				QuickStack.LOGGER.info("Found Nearby Inventories: " + nearbyInventories);

				// For each unique item in the player's inventory, create a corresponding priority queue in the
				// container map prioritizing the inventory with the most free slots (with respect to the item).
				var uniquePlayerItems = InventoryUtil.getUniqueItems(playerInventory, 9, 35);
				QuickStack.LOGGER.info("Found Player Unique Items: " + uniquePlayerItems);
				uniquePlayerItems.forEach(item -> itemContainerMap.put(item, new PriorityQueue<>(
					Comparator.comparingInt(inv -> -InventoryUtil.getAvailableSlots(inv, item)))));

				// For each nearby inventory, add the inventory to all queues in the container map which correspond to
				// an item which exists in said inventory.
				nearbyInventories.forEach(inventory -> {
					// Only consider the intersection of items between the target inventory and player inventory.
					var intersection = InventoryUtil.getUniqueItems(inventory);

					intersection.retainAll(uniquePlayerItems);
					intersection.forEach(item -> itemContainerMap.get(item).add(inventory));
				});
				QuickStack.LOGGER.info("Mappings: " + itemContainerMap);

				// Second Click (if also on button) //
				// TODO: This can be done while the item is being animated traversing to the quickstack icon.
				// For each item in the player's *main* inventory try to insert the item into the head inventory of the
				// associated queue.
				for (var slot = 9; slot <= 35; ++slot) {
					var itemStack = playerInventory.getStack(slot);
					var containerQueue = itemContainerMap.get(itemStack.getItem());

					if (containerQueue == null) continue;

					QuickStack.LOGGER.info("Trying to Stack: " + itemStack.getItem().getName().getString());
					while (!itemStack.equals(ItemStack.EMPTY) && !containerQueue.isEmpty()) {
						var headInventory = containerQueue.peek(); // Per-quick stack, only remove the head if full.

						// If the head inventory can't insert the entire stack, remove it from consideration and repeat
						// the process with the new head inventory.
						if (!InventoryUtil.insert(playerInventory, headInventory, slot)) {
							containerQueue.poll();
							itemStack = playerInventory.getStack(slot);
						}
					}
				}
			});
		}));
	}
}