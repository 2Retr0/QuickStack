package retr0.quickstack.compat.sodium.mixin;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SodiumWorldRenderer.class)
public class MixinSodiumWorldRenderer {
    private static OutlineVertexConsumerProvider outlineConsumerProvider;

    @ModifyVariable(
        // TODO: MAKE SURE THESE METHOD DESCRIPTORS WORK ON ANY SODIUM INSTANCE.
        method = "renderTileEntities",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;",
            shift = At.Shift.AFTER),
        name = "consumer",
        remap = false)
    private VertexConsumerProvider useOutlineConsumerCheck(
        VertexConsumerProvider original, MatrixStack matrices, BufferBuilderStorage bufferBuilders)
    {
        outlineConsumerProvider = bufferBuilders.getOutlineVertexConsumers();

        var color = 0xFFFAAAAA;
        var r = 0xFF & color >> 16;
        var g = 0xFF & color >> 8;
        var b = 0xFF & color;
        outlineConsumerProvider.setColor(r, g, b, 255);

        return outlineConsumerProvider;
    }


    @Inject(
        method = "lambda$renderTileEntities$0",
        at = @At("RETURN"),
        cancellable = true,
        remap = false)
    private static void test(
        VertexConsumer vertexConsumer, VertexConsumerProvider.Immediate immediate, RenderLayer renderLayer,
        CallbackInfoReturnable<VertexConsumer> cir)
    {
        var outlineConsumer = outlineConsumerProvider.getBuffer(renderLayer);

        cir.setReturnValue(renderLayer.hasCrumbling() ?
            VertexConsumers.union(vertexConsumer, outlineConsumer) : outlineConsumer);
    }
}
