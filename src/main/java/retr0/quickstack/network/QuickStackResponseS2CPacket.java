package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Pair;
import retr0.quickstack.QuickStack;
import retr0.quickstack.QuickStackToast;
import retr0.quickstack.util.QuickStackUtil;

import java.util.ArrayList;

public class QuickStackResponseS2CPacket {
    /**
     * @return A {@link PacketByteBuf} containing two integers representing the {@link QuickStackUtil.QuickStackInfo}
     * {@code depositCount} and {@code containerCount} as well as an additional integer denoting the {@code iconSet}
     * size.
     */
    public static PacketByteBuf create(QuickStackUtil.QuickStackInfo quickStackInfo) {
        var buf = PacketByteBufs.create();
        buf.writeInt(quickStackInfo.depositCount());
        buf.writeInt(quickStackInfo.containerCount());
        buf.writeInt(quickStackInfo.iconMappings().size());
        quickStackInfo.iconMappings().forEach(iconMapping -> {
            QuickStack.LOGGER.info("Found!" + iconMapping);
            buf.writeItemStack(iconMapping.getLeft());
            buf.writeItemStack(iconMapping.getRight());
        });

        return buf;
    }



    public static void receive(
        MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender)
    {
        var depositCount = buf.readInt();
        var containerCount = buf.readInt();
        var iconMappings = new ArrayList<Pair<ItemStack, ItemStack>>();

        var iconMappingsCount = buf.readInt();
        for (var i = 0; i < iconMappingsCount; ++i) {
            iconMappings.add(new Pair<>(buf.readItemStack(), buf.readItemStack()));
        }

        client.execute(() -> {
            var quickStackInfo = new QuickStackUtil.QuickStackInfo(depositCount, containerCount, iconMappings);
            QuickStackToast.show(client.getToastManager(), quickStackInfo);
        });
    }
}
