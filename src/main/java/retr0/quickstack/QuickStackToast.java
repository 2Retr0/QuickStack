package retr0.quickstack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static retr0.quickstack.QuickStack.MOD_ID;

public class QuickStackToast implements Toast {
    private static final long DURATION_MS = 5000L;
    private static final Text HEADER = Text.translatable(MOD_ID + ".toast.header");

    private final List<IconPair> iconMappings = new ArrayList<>();

    private int totalDepositCount = 0;
    private int totalContainerCount = 0;
    private long lastStartedTimeMs;
    private boolean justUpdated;

    public QuickStackToast(int depositCount, int containerCount, List<IconPair> iconMappings) {
        addDepositResult(depositCount, containerCount, iconMappings);
    }



    public static void show(ToastManager manager, int depositCount, int containerCount, List<IconPair> iconMappings) {
        var quickStackToast = manager.getToast(QuickStackToast.class, TYPE);
        if (quickStackToast == null)
            manager.add(new QuickStackToast(depositCount, containerCount, iconMappings));
        else
            quickStackToast.addDepositResult(depositCount, containerCount, iconMappings);
    }



    private static Text getPluralityTranslation(String dictKey, int valueAmount) {
        return Text.translatable(MOD_ID + ".dict." + dictKey + "." + (valueAmount == 1 ? "singular" : "plural"));
    }



    private static Text getDescription(int depositCount, int containerCount) {
        return Text.translatable(MOD_ID + ".toast.description",
            depositCount, getPluralityTranslation("item", depositCount),
            containerCount, getPluralityTranslation("chest", containerCount));
    }



    @Override
    public Toast.Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        if (justUpdated) {
            lastStartedTimeMs = startTime;
            justUpdated = false;
        }

        if (iconMappings.isEmpty()) return Toast.Visibility.HIDE;

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Draw background
        manager.drawTexture(matrices, 0, 0, 0, 32, this.getWidth(), this.getHeight());

        // Draw description
        var description = getDescription(totalDepositCount, totalContainerCount);
        manager.getClient().textRenderer.draw(matrices, HEADER, 30.0f, 7.0f, 0xFF500050);
        manager.getClient().textRenderer.draw(matrices, description, 30.0f, 18.0f, 0xFF000000);
        var iconMapping = iconMappings.get(
            (int)(startTime / Math.max(1L, DURATION_MS / iconMappings.size()) % iconMappings.size()));

        var matrixStack = RenderSystem.getModelViewStack();
        // Draw current container icon
        matrixStack.push();
        matrixStack.scale(0.6f, 0.6f, 1.0f);
        RenderSystem.applyModelViewMatrix();
        manager.getClient().getItemRenderer().renderInGui(iconMapping.containerIcon(), 3, 3);
        matrixStack.pop();

        // Draw current item icon
        RenderSystem.applyModelViewMatrix();
        manager.getClient().getItemRenderer().renderInGui(iconMapping.itemIcon(), 8, 8);

        return startTime - lastStartedTimeMs >= DURATION_MS ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }



    private void addDepositResult(int depositCount, int containerCount, List<IconPair> iconMappings) {
        this.totalDepositCount += depositCount;
        this.totalContainerCount += containerCount;
        this.iconMappings.addAll(iconMappings);

        justUpdated = true;
    }

    /**
     * Record containing two {@link ItemStack}s, representing a deposited item's icon and its deposited container's icon
     * respectively.
     */
    public record IconPair(ItemStack itemIcon, ItemStack containerIcon) { }
}
