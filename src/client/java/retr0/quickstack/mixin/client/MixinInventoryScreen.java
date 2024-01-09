package retr0.quickstack.mixin.client;

import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget.PressAction;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import retr0.quickstack.network.client.C2SPacketDepositRequest;

import static retr0.quickstack.QuickStack.MOD_ID;

@Mixin(InventoryScreen.class)
public abstract class MixinInventoryScreen extends AbstractInventoryScreen<PlayerScreenHandler> {
    @Unique private static final Identifier QUICK_STACK_BUTTON_TEXTURE = new Identifier(MOD_ID, "textures/gui/quick_stack_button.png");
    @Unique private static final ButtonTextures QUICK_STACK_BUTTONS = new ButtonTextures(new Identifier(MOD_ID, "quickstack/button"), new Identifier(MOD_ID, "quickstack/button_highlighted"));


    @Unique private ButtonWidget quickStackButton;

    private MixinInventoryScreen(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }



    /**
     * Adds a quick stack button which requests a quick stack upon press.
     */
    @Inject(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
    private void addQuickStackButton(CallbackInfo ci) {
        // noinspection DataFlowIssue // player is non-null while in game.
        if (client.player.isSpectator()) return;

        int x = this.x + 128, y = height / 2 - 22;
        quickStackButton = new TexturedButtonWidget(x, y, 20, 18, QUICK_STACK_BUTTONS,
            button -> C2SPacketDepositRequest.send());

        this.addDrawableChild(quickStackButton);
    }



    /**
     * Corrects the position of the quick stack button when opening and closing the recipe book.
     */
    @ModifyArg(
        method = "init",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TexturedButtonWidget;<init>(IIIILnet/minecraft/client/gui/screen/ButtonTextures;Lnet/minecraft/client/gui/widget/ButtonWidget$PressAction;)V"))
    private PressAction updateQuickStackButtonPosition(PressAction original) {
        // noinspection DataFlowIssue // player is non-null while in game.
        if (client.player.isSpectator()) return original;

        int x = this.x + 128, y = height / 2 - 22;
        return button -> {
            original.onPress(button);
            quickStackButton.setPosition(x, y);
        };
    }
}
