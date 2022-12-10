package retr0.quickstack.mixin;

import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    private static OutlineVertexConsumerProvider outlineConsumerProvider;

    /**
     * test
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "render",
        at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0),
        name = "bl4")
    private boolean shouldRenderOutline(boolean original) {
        return true;
    }



    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V",
            ordinal = 0, shift = At.Shift.AFTER),
        name = "vertexConsumerProvider2")
    private VertexConsumerProvider useOutlineConsumerCheck(VertexConsumerProvider original) {
        outlineConsumerProvider = bufferBuilders.getOutlineVertexConsumers();

        var color = 0xFFFAAAAA;
        var r = 0xFF & color >> 16;
        var g = 0xFF & color >> 8;
        var b = 0xFF & color;
        outlineConsumerProvider.setColor(r, g, b, 255);

        return outlineConsumerProvider;
    }



    @Inject(
        // Compiler yells at this method descriptor for some reason, but I don't care...
        method = "method_22986(Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;",
        at = @At("RETURN"),
        cancellable = true)
    private static void unionOutlineConsumer(
        VertexConsumerProvider.Immediate immediate, VertexConsumer vertexConsumer, RenderLayer renderLayer,
        CallbackInfoReturnable<VertexConsumer> cir)
    {
        var outlineConsumer = outlineConsumerProvider.getBuffer(renderLayer);

        cir.setReturnValue(renderLayer.hasCrumbling() ?
            VertexConsumers.union(vertexConsumer, outlineConsumer) : outlineConsumer);
    }
}
