package retr0.quickstack.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import static net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
import static retr0.quickstack.QuickStack.MOD_ID;

@Environment(EnvType.CLIENT)
public final class RenderUtil {
    private static final Identifier SLOT_OUTLINE_TEXTURE = new Identifier(MOD_ID, "textures/gui/slot_outline.png");

    private RenderUtil() { }



    public static void drawBlockModelOutlines(
        MinecraftClient client, MatrixStack matrices, Camera camera, ClientWorld world,
        OutlineVertexConsumerProvider outlineProvider)
    {
        var cameraPos = camera.getPos();
        var cameraX = cameraPos.getX();
        var cameraY = cameraPos.getY();
        var cameraZ = cameraPos.getZ();

        var blockRenderManager = client.getBlockRenderManager();
        var modelRenderer = blockRenderManager.getModelRenderer();
        OutlineColorManager.getInstance().forEachBlock((blockPos, outlineColor) -> {
            var blockState = world.getBlockState(blockPos);

            if (blockState.getRenderType() == BlockRenderType.MODEL) {
                var bakedModel = blockRenderManager.getModel(blockState);
                var outlineConsumer = RenderUtil.modifyOutlineProviderColor(outlineProvider, outlineColor)
                        .getBuffer(RenderLayer.getOutline(BLOCK_ATLAS_TEXTURE));

                // Render the selected block models with the outline vertex consumer.
                matrices.push();
                matrices.translate(blockPos.getX() - cameraX, blockPos.getY() - cameraY, blockPos.getZ() - cameraZ);
                modelRenderer.render(matrices.peek(), outlineConsumer, blockState, bakedModel, 0f, 0f, 0f, 0, 0);
                matrices.pop();
            }
        });
    }



    /**
     * Changes the target color of an {@link OutlineVertexConsumerProvider} for a given color.
     * @param outlineProvider The target {@link OutlineVertexConsumerProvider}.
     * @param color The 24-bit RGB color to change to.
     */
    public static OutlineVertexConsumerProvider modifyOutlineProviderColor(
        OutlineVertexConsumerProvider outlineProvider, int color)
    {
        var r = 0xFF & color >> 16;
        var g = 0xFF & color >> 8;
        var b = 0xFF & color;
        outlineProvider.setColor(r, g, b, 0xFF);

        return outlineProvider;
    }



    public static void drawSlotOutlineColor(MatrixStack matrices, Slot slot) {
        // TODO: Outline should draw between rendered item and rendered item count. Fixing would require a Mixin.
        var slotColor = OutlineColorManager.getInstance().getSlotOutlineColor(slot.getIndex());
        if (slotColor == 0) return;

        int x = slot.x, y = slot.y;
        // Brighten slot coloring + desaturation: https://stackoverflow.com/a/44259659
        // slotColor = ~(0x7F7F7F7F & (~slotColor >> 1));
        var r = (0xFF & slotColor >> 16) / 255f;
        var g = (0xFF & slotColor >> 8) / 255f;
        var b = (0xFF & slotColor)     / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, SLOT_OUTLINE_TEXTURE);

        // Draw outer slot outline
        RenderSystem.setShaderColor(r, g, b, 0.8f);
        DrawableHelper.drawTexture(matrices, x, y, 0, 0, 16, 16, 16, 32);

        // Draw inner slot outline
        RenderSystem.setShaderColor(r, g, b, 0.4f);
        DrawableHelper.drawTexture(matrices, x, y, 0, 16, 16, 16, 16, 32);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
