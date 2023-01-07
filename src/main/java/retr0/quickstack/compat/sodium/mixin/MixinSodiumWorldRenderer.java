package retr0.quickstack.compat.sodium.mixin;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import retr0.quickstack.util.ColorManager;

@Pseudo @Mixin(SodiumWorldRenderer.class)
public class MixinSodiumWorldRenderer {
    private static OutlineVertexConsumerProvider outlineConsumerProvider;
    private static Integer containerColor;

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "renderTileEntities",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/block/entity/BlockEntity;getPos()Lnet/minecraft/util/math/BlockPos;",
            ordinal = 0))
    private BlockPos getBlockPos(BlockPos original) {
        containerColor = ColorManager.BLOCK_ENTITY_COLOR_MAP.get(original);

        return original;
    }



    /* [ 16]    [  0]                     BlockEntity  blockEntity                                         -         */
    /* [ 17]    [  0]                        BlockPos  pos                                                 -         */
    /* [ 18]    [  0]          VertexConsumerProvider  consumer                                          >>YES<<     */
    @ModifyVariable(
        // TODO: MAKE SURE THESE METHOD DESCRIPTORS WORK ON ANY SODIUM INSTANCE.
        method = "renderTileEntities",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;",
            shift = At.Shift.AFTER),
        ordinal = 0, remap = false)
    private VertexConsumerProvider useOutlineConsumerCheck(
        VertexConsumerProvider original, MatrixStack matrices, BufferBuilderStorage bufferBuilders)
    {
        if (containerColor == null) return original;

        outlineConsumerProvider = bufferBuilders.getOutlineVertexConsumers();
        var r = 0xFF & containerColor >> 16;
        var g = 0xFF & containerColor >> 8;
        var b = 0xFF & containerColor;
        outlineConsumerProvider.setColor(r, g, b, 0xFF);

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
        if (containerColor == null) return;

        var outlineConsumer = outlineConsumerProvider.getBuffer(renderLayer);
        cir.setReturnValue(renderLayer.hasCrumbling() ?
            VertexConsumers.union(vertexConsumer, outlineConsumer) : outlineConsumer);
    }
}
