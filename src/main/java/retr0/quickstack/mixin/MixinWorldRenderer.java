package retr0.quickstack.mixin;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import retr0.quickstack.event.BlockEntityRenderEvents;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;checkEmpty(Lnet/minecraft/client/util/math/MatrixStack;)V", ordinal = 1, shift = At.Shift.AFTER))
    public void afterBlockEntityRender(
            MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera,
            GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix,
            CallbackInfo ci)
    {
        BlockEntityRenderEvents.END.invoker().onRenderingEnd();
    }

//    @ModifyArg(
//        method = "render",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
//            ordinal = 1),
//        index = 3)
//    public VertexConsumerProvider afterBlockEntityRender(VertexConsumerProvider original) {
//        return new OutlineVertexConsumerProvider((VertexConsumerProvider.Immediate) original);
//    }
}
