package retr0.quickstack;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import retr0.quickstack.util.IconPair;

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

    private static Text getDescription(int depositCount, int containerCount) {
        return Text.translatable(MOD_ID + ".toast.description",
            depositCount, getPluralityTranslation("item", depositCount),
            containerCount, getPluralityTranslation("chest", containerCount));
    }

    private static Text getPluralityTranslation(String dictKey, int valueAmount) {
        return Text.translatable(MOD_ID + ".dict." + dictKey + "." + (valueAmount == 1 ? "singular" : "plural"));
    }

    private void addDepositResult(int depositCount, int containerCount, List<IconPair> iconMappings) {
        this.totalDepositCount += depositCount;
        this.totalContainerCount += containerCount;
        this.iconMappings.addAll(iconMappings);

        justUpdated = true;
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        if (justUpdated) {
            lastStartedTimeMs = startTime;
            justUpdated = false;
        }

        if (iconMappings.isEmpty()) return Visibility.HIDE;

        // --- Draw Background ---
        context.drawTexture(TEXTURE, 0, 0, 0, 32, this.getWidth(), this.getHeight());

        // --- Draw Description ---
        var description = getDescription(totalDepositCount, totalContainerCount);
        context.drawText(manager.getClient().textRenderer, HEADER, 30, 7, 0xFF500050, false);
        context.drawText(manager.getClient().textRenderer, description, 30, 18, 0xFF000000, false);
        var iconMapping = iconMappings.get(
            (int)(startTime / Math.max(1L, DURATION_MS / iconMappings.size()) % iconMappings.size()));

        // --- Draw Current Container Icon ---
        context.getMatrices().push();
        context.getMatrices().scale(0.6f, 0.6f, 1.0f);
//        RenderSystem.applyModelViewMatrix();
        context.drawItemWithoutEntity(iconMapping.containerIcon(), 3, 3);
        context.getMatrices().pop();

        // --- Draw Current Item Icon ---
//        RenderSystem.applyModelViewMatrix();
        context.drawItemWithoutEntity(iconMapping.itemIcon(), 8, 8);

        return startTime - lastStartedTimeMs >= DURATION_MS ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
}
