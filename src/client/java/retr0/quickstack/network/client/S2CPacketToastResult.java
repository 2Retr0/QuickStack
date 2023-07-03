package retr0.quickstack.network.client;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import retr0.quickstack.QuickStackToast;
import retr0.quickstack.util.IconPair;

import java.util.ArrayList;

/**
 * Maps items to a "deposited total" along with an immutable icon for the container it was deposited into.
 */
public class S2CPacketToastResult {
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
                var volume = 0.5f;
                var pitch = player.getWorld().random.nextFloat() * 0.1f + 0.9f;
                player.playSound(
                    SoundEvents.BLOCK_BARREL_CLOSE, SoundCategory.NEUTRAL, volume, pitch);
            }
        });
    }
}
