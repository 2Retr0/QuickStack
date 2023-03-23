package retr0.quickstack.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import retr0.quickstack.util.OutlineColorManager;

import java.util.*;

import static retr0.quickstack.QuickStack.MOD_ID;

/**
 * Record representing information assigned to a container (i.e. block entity) containing the tint color for the
 * block entity and the slots of the player inventory whose item stack has been transferred (completely or
 * partially) to the container.
 */
public class S2CPacketDepositResult {
    public static final Identifier DEPOSIT_RESULT_ID = new Identifier(MOD_ID, "quick_stack_color_response");

    private final Map<BlockPos, List<Integer>> containerSlotMap = new HashMap<>();
    private final Set<Integer> usedSlots = new HashSet<>();

    public void updateContainerSlots(BlockPos depositedContainer, int slot) {
        if (usedSlots.contains(slot)) return;

        usedSlots.add(slot);

        if (containerSlotMap.containsKey(depositedContainer))
            containerSlotMap.get(depositedContainer).add(slot);
        else
            containerSlotMap.put(depositedContainer, new ArrayList<>(List.of(slot)));
    }


    public int getDepositedContainerCount() {
        return containerSlotMap.size();
    }



    public static void send(S2CPacketDepositResult depositResult, ServerPlayerEntity player) {
        var buf = PacketByteBufs.create();

        buf.writeByte(depositResult.containerSlotMap.size());
        depositResult.containerSlotMap.forEach(((blockPos, slots) -> {
            buf.writeBlockPos(blockPos);   // Write container's BlockPos.
            buf.writeByte(slots.size());   // Write container's associated slot list size.
            slots.forEach(buf::writeByte); // Write container's associated slots.
        }));
        ServerPlayNetworking.send(player, DEPOSIT_RESULT_ID, buf);
    }



    public static void receive(
        MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender)
    {
        var depositedContainerCount = buf.readByte();
        var containerSlotMap = new HashMap<BlockPos, List<Integer>>();

        for (var i = 0; i < depositedContainerCount; ++i) {
            var containerPos = buf.readBlockPos();
            var associatedSlots = new ArrayList<Integer>();

            var associatedSlotCount = buf.readByte();
            for (var j = 0; j < associatedSlotCount; ++j) {
                int slot = buf.readByte();
                associatedSlots.add(slot);
            }
            containerSlotMap.put(containerPos, associatedSlots);
        }

        client.execute(() -> {
            if (client.player == null) return;

            OutlineColorManager.getInstance().addMappings(client.player.world, containerSlotMap);
        });
    }
}
