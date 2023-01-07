package retr0.quickstack.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import retr0.quickstack.util.ColorManager;

import static net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
import static retr0.quickstack.util.ColorManager.MODEL_COLOR_MAP;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow @Final private BufferBuilderStorage bufferBuilders;
    @Shadow @Final private MinecraftClient client;

    private static OutlineVertexConsumerProvider outlineConsumerProvider;
    private static Integer containerColor;

    /* [ 23]    [  3]                         boolean  bl3                                                 YES       */
    /* [ 24]    [  4]                         boolean  bl4                                               >>YES<<     */
    /* [ 25]    [  0]  VertexConsumerProvider$Immediate  immediate                                           -       */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "render",
        at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0, shift = At.Shift.BEFORE),
        ordinal = 4)
    private boolean shouldRenderOutline(boolean original) {
        return true;
    }



    @Inject(method = "render", at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0))
    private void renderModelOutlines(
        MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera,
        GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix,
        CallbackInfo ci)
    {
        if (ColorManager.finished) return;

        var cameraPos = camera.getPos();
        var cameraX = cameraPos.getX();
        var cameraY = cameraPos.getY();
        var cameraZ = cameraPos.getZ();

        MODEL_COLOR_MAP.forEach((blockPos, containerInfo) -> {
            var outlineColor = containerInfo.getLeft();
            var blockState = containerInfo.getRight();
            var bakedModel = client.getBlockRenderManager().getModel(blockState);
            matrices.push();
            matrices.translate((double)blockPos.getX() - cameraX, (double)blockPos.getY() - cameraY, (double)blockPos.getZ() - cameraZ);
            client.getBlockRenderManager().getModelRenderer().render(
                matrices.peek(), getOutlineConsumer(outlineColor), blockState, bakedModel, 0.0f, 0.0f, 0.0f, 0, 0);
            matrices.pop();
        });
    }

    @Unique
    private VertexConsumer getOutlineConsumer(int color) {
        var outlineConsumerProvider = bufferBuilders.getOutlineVertexConsumers();

        var r = 0xFF & color >> 16;
        var g = 0xFF & color >> 8;
        var b = 0xFF & color;
        outlineConsumerProvider.setColor(r, g, b, 0x80);

        return outlineConsumerProvider.getBuffer(RenderLayer.getOutline(BLOCK_ATLAS_TEXTURE));
    }


    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "render",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/block/entity/BlockEntity;getPos()Lnet/minecraft/util/math/BlockPos;",
            ordinal = 0))
    private BlockPos getBlockPos(BlockPos original) {
        containerColor = ColorManager.BLOCK_ENTITY_COLOR_MAP.get(original);

        return original;
    }



    /* [ 30]    [  0]                     BlockEntity  blockEntity                                         -         */
    /* [ 31]    [  0]                        BlockPos  blockPos2                                           -         */
    /* [ 32]    [  0]          VertexConsumerProvider  vertexConsumerProvider2                           >>YES<<     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V",
            ordinal = 0, shift = At.Shift.AFTER),
        ordinal = 0)
    private VertexConsumerProvider useOutlineConsumerCheck(VertexConsumerProvider original) {
        if (containerColor == null) return original;

        outlineConsumerProvider = bufferBuilders.getOutlineVertexConsumers();
        var r = 0xFF & containerColor >> 16;
        var g = 0xFF & containerColor >> 8;
        var b = 0xFF & containerColor;
        outlineConsumerProvider.setColor(r, g, b, 0xFF);

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
        if (containerColor == null) return;

        var outlineConsumer = outlineConsumerProvider.getBuffer(renderLayer);
        cir.setReturnValue(renderLayer.hasCrumbling() ?
            VertexConsumers.union(vertexConsumer, outlineConsumer) : outlineConsumer);
    }
}
