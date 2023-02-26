package retr0.quickstack.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import retr0.quickstack.QuickStackClient;
import retr0.quickstack.util.OutlineRenderManager;

import static retr0.quickstack.QuickStack.MOD_ID;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen extends Screen {
    @Unique private static final Identifier QUICK_STACK_BUTTON_TEXTURE =
        new Identifier(MOD_ID, "textures/gui/slot_outline.png");

    /**
     * Renders a colored outline if the target slot has an assigned color.
     */
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V",
            shift = At.Shift.AFTER),
        locals = LocalCapture.CAPTURE_FAILSOFT)
    private void renderSlotOutline(
        MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci, int i, int j,
        MatrixStack matrixStack, int k, Slot slot)
    {
        var slotColor = OutlineRenderManager.INSTANCE.slotColorMap.get(slot.getIndex());
        if (slotColor == null || !(slot.inventory instanceof PlayerInventory)) return;

        int x = slot.x, y = slot.y;
        // Brighten slot coloring + desaturation: https://stackoverflow.com/a/44259659
        // slotColor = ~(0x7F7F7F7F & (~slotColor >> 1));
        var r = (0xFF & slotColor >> 16) / 255f;
        var g = (0xFF & slotColor >> 8) / 255f;
        var b = (0xFF & slotColor)     / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, QUICK_STACK_BUTTON_TEXTURE);

        // Draw outer slot outline
        RenderSystem.setShaderColor(r, g, b, 0.8f);
        DrawableHelper.drawTexture(matrices, x, y, 0, 0, 16, 16, 16, 32);

        // Draw inner slot outline
        RenderSystem.setShaderColor(r, g, b, 0.4f);
        DrawableHelper.drawTexture(matrices, x, y, 0, 16, 16, 16, 16, 32);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private MixinHandledScreen(Text title) { super(title); }
}
