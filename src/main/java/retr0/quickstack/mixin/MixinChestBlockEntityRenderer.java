package retr0.quickstack.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChestBlockEntityRenderer.class)
public abstract class MixinChestBlockEntityRenderer<T extends BlockEntity> {
    // @ModifyArg(
    //     method = "render*",
    //     at = @At(
    //         value = "INVOKE",
    //         target = "Lnet/minecraft/client/util/SpriteIdentifier;getVertexConsumer(Lnet/minecraft/client/render/VertexConsumerProvider;Ljava/util/function/Function;)Lnet/minecraft/client/render/VertexConsumer;"),
    //     index = 1)
    // protected Function<Identifier, RenderLayer> modifyRenderFactory(Function<Identifier, RenderLayer> renderFactory) {
    //     return identifier -> QuickStack.sobelBuffer.getRenderLayer(renderFactory.apply(identifier));
    // }

//    @ModifyVariable(
//        method = "render*",
//        at = @At(
//            value = "INVOKE_ASSIGN",
//            target = "Lnet/minecraft/client/util/SpriteIdentifier;getVertexConsumer(Lnet/minecraft/client/render/VertexConsumerProvider;Ljava/util/function/Function;)Lnet/minecraft/client/render/VertexConsumer;"),
//        index = 1
//    )
//    protected VertexConsumer modifyVertex(VertexConsumer vertexConsumer) {
//
//        return new OutlineVertexConsumer();
//    }
}
