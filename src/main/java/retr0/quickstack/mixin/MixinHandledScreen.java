package retr0.quickstack.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import retr0.quickstack.util.RenderUtil;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends Screen {
    /**
     * Renders a colored outline if the target slot has an assigned color.
     */
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;isPointOverSlot(Lnet/minecraft/screen/slot/Slot;DD)Z",
            shift = At.Shift.AFTER),
        locals = LocalCapture.CAPTURE_FAILSOFT)
    private void renderSlotOutline(
        MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci, int i, int j, int k, Slot slot)
    {
        if (!(slot.inventory instanceof PlayerInventory)) return;

        RenderUtil.drawSlotOutlineColor(matrices, slot);
    }

    private MixinHandledScreen(Text title) { super(title); }
}
