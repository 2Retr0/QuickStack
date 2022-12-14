package retr0.quickstack.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

import static retr0.quickstack.QuickStack.MOD_ID;

public class PacketRegistry {
    public static final Identifier QUICK_STACK_REQUEST_ID = new Identifier(MOD_ID, "request_quick_stack");
    public static final Identifier QUICK_STACK_RESPONSE_ID = new Identifier(MOD_ID, "quick_stack_response");
    public static final Identifier QUICK_STACK_COLOR_RESULT_ID = new Identifier(MOD_ID, "quick_stack_color_response");

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(QUICK_STACK_REQUEST_ID, QuickStackRequestC2SPacket::receive);
    }

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(QUICK_STACK_RESPONSE_ID, QuickStackResponseS2CPacket::receive);
        ClientPlayNetworking.registerGlobalReceiver(QUICK_STACK_COLOR_RESULT_ID, QuickStackColorResponseS2CPacket::receive);
    }
}
