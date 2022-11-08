package retr0.quickstack.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import retr0.quickstack.QuickStack;

import static retr0.quickstack.QuickStack.MOD_ID;

@Mixin(InventoryScreen.class)
public abstract class MixinInventoryScreen extends AbstractInventoryScreen<PlayerScreenHandler> {
    @Shadow @Final private static Identifier RECIPE_BUTTON_TEXTURE;

    @Unique private ButtonWidget quickStackButton;

    /**
     * Adds a quick stack button which requests a quick stack upon press.
     */
    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
    public void addQuickStackButton(CallbackInfo ci) {
        quickStackButton = new TexturedButtonWidget(x + 128, height / 2 - 22, 20, 18, 0, 0, 19, RECIPE_BUTTON_TEXTURE,
            button -> {
                QuickStack.LOGGER.info("Pressed QuickStack Button!");

                ClientPlayNetworking.send(new Identifier(MOD_ID, "request_quick_stack"), PacketByteBufs.empty());
            });
        this.addDrawableChild(quickStackButton);
    }



    /**
     * Corrects the position of the quick stack button when opening and closing the recipe book.
     */
    @ModifyArg(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TexturedButtonWidget;<init>(IIIIIIILnet/minecraft/util/Identifier;Lnet/minecraft/client/gui/widget/ButtonWidget$PressAction;)V"))
    public ButtonWidget.PressAction updateQuickStackButtonPosition(ButtonWidget.PressAction original) {
        return button -> {
            original.onPress(button);
            ((TexturedButtonWidget)quickStackButton).setPos(x + 128, height / 2 - 22);
        };
    }

    public MixinInventoryScreen(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }
}