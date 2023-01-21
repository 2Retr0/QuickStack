package retr0.quickstack.util;

import net.minecraft.client.render.OutlineVertexConsumerProvider;

public class RenderUtil {
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
}
