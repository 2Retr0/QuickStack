package retr0.itemfavoriting.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import retr0.itemfavoriting.ItemFavoriting;
import retr0.quickstack.QuickStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(HandledScreen.class)
public class MixinHandledScreen<T extends ScreenHandler> extends Screen {
    @Shadow @Final protected T handler;
    @Shadow @Final protected Set<Slot> cursorDragSlots;
    @Shadow private int draggedStackRemainder;

    @Unique private final Set<Integer> favoriteSlots = new HashSet<>(List.of(36, 37, 38));
    @Unique private int cursorFavoriteSlot = 0;
    @Unique private boolean isHoldingFavoriteItem = false;

    @SuppressWarnings({"InvalidInjectorMethodSignature", "DataFlowIssue"})
    @Inject(
        method = "mouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"),
        locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void test(
        double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir, boolean bl, Slot slot)
    {
        var isAltHeld = InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT);

        if (!(button == 0 && isAltHeld && slot != null)) return;

        QuickStack.LOGGER.info("clicked with thingy!");
        cir.setReturnValue(true);
        cir.cancel();
    }

    // @SuppressWarnings("InvalidInjectorMethodSignature")
    // @Inject(
    //     method = "mouseClicked",
    //     at = @At(
    //         value = "INVOKE",
    //         target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
    //         ordinal = 1),
    //     locals = LocalCapture.CAPTURE_FAILSOFT)
    // private void modify(
    //     double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir, boolean bl, Slot slot, long l,
    //     int i, SlotActionType slotActionType)
    // {
    // }

    @Inject(
        method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V"),
        cancellable = true)
    private void test(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        ItemFavoriting.LOGGER.info("slotId: " + slotId + ", button: " + button + ", actionType: " + actionType.toString());
        // Item favoriting (slot favorite) should remain if:
        //  * item is not picked up, and items are taken from the stack and doesn't result in an empty stack
        //  * favorited item is placed down to a new slot in inventory and is not an empty stack (move
        //    slot favorite to new slot)
        //     - If last item in stack is picked up, move slot favorite to final place down location (left-click).
        //     - dragging all items until empty stack will destroy item favorite
        //  * double clicking ismilar items to stack all!
        // Item favoriting (slot favorite) should be destroyed if:
        //  * held favorite item is merged into a different stack
        //  * held favorite item is put into a container
        if (slot != null) {
            switch (actionType) {
                // Done when editing a stack (partially/completely) (i.e. picking up or placing down).
                case PICKUP:
                    var slotStack = slot.getStack();
                    var cursorStack = handler.getCursorStack();

                    // TODO: remove cursor favorite slot to simplif logic
                    ItemFavoriting.LOGGER.info("selected slot: " + slotId + (favoriteSlots.contains(slotId) ? " ...which is a favorite slot!" : ""));
                    if (handler.getCursorStack().isEmpty() && favoriteSlots.contains(slotId)) {
                        if (slotStack.getCount() > 1 && button != 0) break;

                        ItemFavoriting.LOGGER.info("Action 1"); // Grab favorite item stack from empty cursor
                        isHoldingFavoriteItem = true;
                        cursorFavoriteSlot = slotId;
                    } else if (isHoldingFavoriteItem && slot.canInsert(cursorStack)) {
                        var depositCount = button != 0 ? 1 : cursorStack.getCount();

                        if (areItemStacksEqual(cursorStack, slot.getStack()) &&
                            depositCount <= slotStack.getMaxCount() - slotStack.getCount())
                        {
                            if (cursorStack.getCount() > 1 && button != 0) break;

                            ItemFavoriting.LOGGER.info("Action 2"); // Fully deposited held favorite item onto similar stack
                            isHoldingFavoriteItem = false;
                            favoriteSlots.remove(cursorFavoriteSlot);
                            favoriteSlots.add(slotId);
                            cursorFavoriteSlot = -1;
                        } else if (!slot.hasStack()) {
                            if (cursorStack.getCount() > 1 && button != 0) break;

                            ItemFavoriting.LOGGER.info("Action 3"); // Fully deposited held favorite item onto empty stack
                            isHoldingFavoriteItem = false;
                            favoriteSlots.remove(cursorFavoriteSlot);
                            favoriteSlots.add(slotId);
                            cursorFavoriteSlot = -1;
                        } else {
                            // stacks are swapped
                            if (favoriteSlots.contains(slotId)) {
                                ItemFavoriting.LOGGER.info("Action 4"); // Swapped held favorite item with another favorite item.
                                favoriteSlots.remove(cursorFavoriteSlot);
                                favoriteSlots.add(slotId);
                                cursorFavoriteSlot = -1;
                            } else {
                                ItemFavoriting.LOGGER.info("Action 5"); // Swapped held favorite item with non-favorite item.
                                isHoldingFavoriteItem = false;
                                favoriteSlots.remove(cursorFavoriteSlot);
                                favoriteSlots.add(slotId);
                                cursorFavoriteSlot = -1;
                            }
                        }
                    } else if (favoriteSlots.contains(slotId) && slot.hasStack() && !areItemStacksEqual(handler.getCursorStack(), slot.getStack()) && slot.canInsert(handler.getCursorStack())) {
                        ItemFavoriting.LOGGER.info("Action 6"); // holding non-favorite item swapped with favorite-item.
                        isHoldingFavoriteItem = true;
                        cursorFavoriteSlot = slotId;
                    }
                    break;
                case QUICK_MOVE:
                    if (favoriteSlots.contains(slotId))
                        ci.cancel();
                    break;
                case THROW:
                    if (isHoldingFavoriteItem || favoriteSlots.contains(slotId))
                        ci.cancel();
                    break;
                case QUICK_CRAFT:
                    // If only one cursorDragSlot and no remainder, do normal procedure, otherwise if there is no remainder
                    // destroy favorite, and if there is a remainder, do nothing (it should auto fix?)
                    ItemFavoriting.LOGGER.info("DragSlots: " + Arrays.toString(cursorDragSlots.stream().mapToInt(slot2 -> slot2.id).toArray()) + "");
                    ItemFavoriting.LOGGER.info("           " + draggedStackRemainder);
                    break;
            }
            // TODO: Prevent dropping items out of inventory when holding favorite item
            // TODO:
        }
        ItemFavoriting.LOGGER.info("favoriteSlots: " + Arrays.toString(favoriteSlots.toArray()) + ", cursorFavoriteSlot: " + cursorFavoriteSlot + ", isHoldingFavoriteItem: " + isHoldingFavoriteItem + "\n");
    }

    private boolean areItemStacksEqual(ItemStack left, ItemStack right) {
        if (!left.isOf(right.getItem()))
            return false;
        if (left.getNbt() == null && right.getNbt() != null)
            return false;
        return left.getNbt() == null || left.getNbt().equals(right.getNbt());
    }

    protected MixinHandledScreen(Text title) { super(title); }
}
