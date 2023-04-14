package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import retr0.quickstack.QuickStackToast;
import retr0.quickstack.QuickStackToast.IconPair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import static retr0.quickstack.QuickStack.MOD_ID;

/**
 * Maps items to a "deposited total" along with an immutable icon for the container it was deposited into.
 */
public class S2CPacketToastResult {
    public static final Identifier TOAST_RESULT_ID = new Identifier(MOD_ID, "quick_stack_response");

    /**
     * Sends a {@link S2CPacketToastResult} packet to the client.
     */
    public static void send(
        Map<Item, Pair<Integer, ItemStack>> itemUsageMap, int totalItemsDeposited, int totalContainersUsed,
        ServerPlayerEntity player)
    {
        var topDeposited = itemUsageMap.entrySet().stream()
            .sorted(Comparator.comparingInt(entry -> -entry.getValue().getLeft()))
            .limit(3)
            .map(entry -> new IconPair(entry.getKey().getDefaultStack(), entry.getValue().getRight()))
            .toList();

        var buf = PacketByteBufs.create();
        // Note: In the case where an item is deposited into multiple containers, the container icon for the toast
        //       would be the highest priority container for the item (denoted by the entry in the item->container
        //       mappings).
        buf.writeInt(totalItemsDeposited);
        buf.writeByte(totalContainersUsed);
        buf.writeByte(topDeposited.size());
        topDeposited.forEach(iconPair -> {
            buf.writeItemStack(iconPair.itemIcon());
            buf.writeItemStack(iconPair.containerIcon());
        });
        ServerPlayNetworking.send(player, TOAST_RESULT_ID, buf);
    }



    /**
     * Shows/updates a {@link QuickStackToast} instance with the packet data on the client.
     */
    public static void receive(
        MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender)
    {
        var totalItemsDeposited = buf.readInt();
        var totalContainersUsed = buf.readByte();
        var iconMappings = new ArrayList<IconPair>();

        var iconMappingsCount = buf.readByte();
        for (var i = 0; i < iconMappingsCount; ++i) {
            var itemIcon = buf.readItemStack();
            var containerIcon = buf.readItemStack();

            iconMappings.add(new IconPair(itemIcon, containerIcon));
        }

        client.execute(() -> {
            QuickStackToast.show(client.getToastManager(), totalItemsDeposited, totalContainersUsed, iconMappings);

            var player = client.player;
            if (player == null) return;
            // Play up to a maximum of three sound instances based on deposited container counts to prevent spam.
            for (var i = 0; i < Math.min(totalContainersUsed, 3); ++i) {
                player.playSound(
                    SoundEvents.BLOCK_BARREL_CLOSE, SoundCategory.NEUTRAL, 0.5f, player.world.random.nextFloat() * 0.1f + 0.9f);
            }
        });
    }
}
