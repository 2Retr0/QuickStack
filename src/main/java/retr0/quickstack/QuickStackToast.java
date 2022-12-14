package retr0.quickstack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import retr0.quickstack.network.util.QuickStackResult;

import java.util.ArrayList;
import java.util.List;

import static retr0.quickstack.QuickStack.MOD_ID;

public class QuickStackToast implements Toast {
    private static final long DURATION = 5000L;
    private static final Text HEADER = Text.translatable(MOD_ID + ".toast.header");

    private final List<QuickStackResult.IconMapping> iconMappings = new ArrayList<>();

    private int totalDepositCount = 0;
    private int totalContainerCount = 0;
    private long lastStartedTime;
    private boolean justUpdated;

    public QuickStackToast(QuickStackResult result) { addQuickStackInfo(result); }



    public static void show(ToastManager manager, QuickStackResult result) {
        var quickStackToast = manager.getToast(QuickStackToast.class, TYPE);
        if (quickStackToast == null)
            manager.add(new QuickStackToast(result));
        else
            quickStackToast.addQuickStackInfo(result);
    }



    public static Text getPluralityTranslation(String dictKey, int valueAmount) {
        return Text.translatable(MOD_ID + ".dict." + dictKey + "." + (valueAmount == 1 ? "singular" : "plural"));
    }



    public static Text getDescription(int depositCount, int containerCount) {
        return Text.translatable(MOD_ID + ".toast.description",
            depositCount, getPluralityTranslation("item", depositCount),
            containerCount, getPluralityTranslation("chest", containerCount));
    }



    @Override
    public Toast.Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        if (justUpdated) {
            lastStartedTime = startTime;
            justUpdated = false;
        }

        if (iconMappings.isEmpty()) return Toast.Visibility.HIDE;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        manager.drawTexture(matrices, 0, 0, 0, 32, this.getWidth(), this.getHeight());

        var description = getDescription(totalDepositCount, totalContainerCount);
        manager.getClient().textRenderer.draw(matrices, HEADER, 30.0f, 7.0f, 0xFF500050);
        manager.getClient().textRenderer.draw(matrices, description, 30.0f, 18.0f, 0xFF000000);
        var iconMapping = iconMappings.get(
            (int)(startTime / Math.max(1L, DURATION / iconMappings.size()) % iconMappings.size()));

        var matrixStack = RenderSystem.getModelViewStack();

        matrixStack.push();
        matrixStack.scale(0.6f, 0.6f, 1.0f);
        RenderSystem.applyModelViewMatrix();
        manager.getClient().getItemRenderer().renderInGui(iconMapping.containerIcon(), 3, 3);
        matrixStack.pop();

        RenderSystem.applyModelViewMatrix();
        manager.getClient().getItemRenderer().renderInGui(iconMapping.itemIcon(), 8, 8);

        return startTime - lastStartedTime >= DURATION ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }



    private void addQuickStackInfo(QuickStackResult quickStackInfo) {
        totalDepositCount += quickStackInfo.depositCount();
        totalContainerCount += quickStackInfo.containerCount();
        iconMappings.addAll(quickStackInfo.iconMappings());

        justUpdated = true;
    }
}
