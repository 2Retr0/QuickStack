package retr0.quickstack.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import retr0.quickstack.QuickStackClient;
import retr0.quickstack.util.OutlineRenderManager;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends Screen {
    // FIXME: this is awful, find a better solution!
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V",
            shift = At.Shift.AFTER),
        locals = LocalCapture.CAPTURE_FAILSOFT)
    private void renderSlotColoring(
        MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci, int i, int j,
        MatrixStack matrixStack, int k, Slot slot)
    {
        var slotColor = OutlineRenderManager.INSTANCE.slotColorMap.get(slot.getIndex());
        if (slotColor == null || slot.inventory != client.player.getInventory()) return;

        var slotShadowColor = 0xFF000000 | slotColor;
        var slotHighlightColor = 0xFF000000 | ~(0x7F7F7F7F & (~slotColor >> 1));

        var slotShadowColor2 = 0x40000000 | slotColor;
        // https://stackoverflow.com/a/44259659
        var slotHighlightColor2 = 0x40000000 | ~(0x7F7F7F7F & (~slotColor >> 1));

        int x = slot.x, y = slot.y;
        // HandledScreen.fillGradient(matrices, x,   y,   x+16, y+16, slotHighlightColor, slotHighlightColor, getZOffset());
        // HandledScreen.fillGradient(matrices, x+1, y+1, x+15, y+15, slotShadowColor, slotShadowColor, getZOffset());

        HandledScreen.fillGradient(matrices, x,   y,   x+1, y+16, slotHighlightColor, slotHighlightColor, getZOffset());
        HandledScreen.fillGradient(matrices, x,   y,   x+16, y+1, slotHighlightColor, slotHighlightColor, getZOffset());
        HandledScreen.fillGradient(matrices, x,   y+15,   x+16, y+16, slotShadowColor, slotShadowColor, getZOffset());
        HandledScreen.fillGradient(matrices, x+15,   y,   x+16, y+16, slotShadowColor, slotShadowColor, getZOffset());

        HandledScreen.fillGradient(matrices, x+1,   y+1,   x+2, y+15, slotShadowColor2, slotShadowColor2, getZOffset());
        HandledScreen.fillGradient(matrices, x+1,   y+1,   x+15, y+2, slotShadowColor2, slotShadowColor2, getZOffset());
        HandledScreen.fillGradient(matrices, x+1,   y+14,   x+15, y+15, slotShadowColor2, slotShadowColor2, getZOffset());
        HandledScreen.fillGradient(matrices, x+14,   y+1,   x+15, y+15, slotShadowColor2, slotShadowColor2, getZOffset());
    }

    protected MixinHandledScreen(Text title) { super(title); }
}
