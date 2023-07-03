package retr0.quickstack.network;

import net.minecraft.util.Identifier;

import static retr0.quickstack.QuickStack.MOD_ID;

public abstract class PacketIdentifiers {
    public static final Identifier DEPOSIT_REQUEST_ID = new Identifier(MOD_ID, "request_quick_stack");
    public static final Identifier DEPOSIT_RESULT_ID = new Identifier(MOD_ID, "quick_stack_color_response");
    public static final Identifier TOAST_RESULT_ID = new Identifier(MOD_ID, "quick_stack_response");
}
