package retr0.quickstack.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import retr0.quickstack.QuickStack;

import java.util.function.Function;

@Mixin(ChestBlockEntityRenderer.class)
public abstract class MixinChestBlockEntityRenderer<T extends BlockEntity> {
    @ModifyArg(
        method = "render*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/SpriteIdentifier;getVertexConsumer(Lnet/minecraft/client/render/VertexConsumerProvider;Ljava/util/function/Function;)Lnet/minecraft/client/render/VertexConsumer;"),
        index = 1)
    protected Function<Identifier, RenderLayer> modifyRenderFactory(Function<Identifier, RenderLayer> renderFactory) {
        return identifier -> QuickStack.sobelBuffer.getRenderLayer(renderFactory.apply(identifier));
    }
}
