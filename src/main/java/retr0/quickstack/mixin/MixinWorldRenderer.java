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
import retr0.quickstack.QuickStackClient;
import retr0.quickstack.util.OutlineRenderManager;
import retr0.quickstack.util.RenderUtil;

import static net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow @Final private BufferBuilderStorage bufferBuilders;
    @Shadow @Final private MinecraftClient client;

    @Unique private static boolean isRendering;
    @Unique private static OutlineVertexConsumerProvider outlineProvider;
    @Unique private static Integer containerColor;

    /**
     * Adds additional logic to enable the outline shader if any blocks are set to have an outline. ALso caches the
     * outline provider.
     */
    /* [ 23]    [  3]                         boolean  bl3                                                 YES       */
    /* [ 24]    [  4]                         boolean  bl4                                               >>YES<<     */
    /* [ 25]    [  0]  VertexConsumerProvider$Immediate  immediate                                           -       */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "render",
        at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0, shift = At.Shift.BEFORE),
        ordinal = 4)
    private boolean shouldRenderOutline(boolean original) {
        outlineProvider = bufferBuilders.getOutlineVertexConsumers();
        isRendering = OutlineRenderManager.INSTANCE.isRendering();

        return original || OutlineRenderManager.INSTANCE.isRendering();
    }



    /**
     * Renders outlines for valid block models.
     * @implNote Does not override the original block model rendering instead treating the outline as an overlay.
     */
    @Inject(method = "render", at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0))
    private void renderModelOutlines(
        MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera,
        GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix,
        CallbackInfo ci)
    {
        if (!isRendering) return;

        var cameraPos = camera.getPos();
        var cameraX = cameraPos.getX();
        var cameraY = cameraPos.getY();
        var cameraZ = cameraPos.getZ();

        OutlineRenderManager.INSTANCE.blockModelColorMap.forEach((blockPos, containerInfo) -> {
            var outlineColor = containerInfo.getLeft();
            var blockState = containerInfo.getRight();
            var bakedModel = client.getBlockRenderManager().getModel(blockState);
            var outlineConsumer = RenderUtil.modifyOutlineProviderColor(outlineProvider, outlineColor)
                .getBuffer(RenderLayer.getOutline(BLOCK_ATLAS_TEXTURE));

            // Render the selected block models with the outline vertex consumer.
            matrices.push();
            matrices.translate(blockPos.getX() - cameraX, blockPos.getY() - cameraY, blockPos.getZ() - cameraZ);
            client.getBlockRenderManager().getModelRenderer()
                .render(matrices.peek(), outlineConsumer, blockState, bakedModel, 0.0f, 0.0f, 0.0f, 0, 0);
            matrices.pop();
        });
    }



    /**
     * Caches the container color (if it exists) for the current block entity.
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "render",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/block/entity/BlockEntity;getPos()Lnet/minecraft/util/math/BlockPos;",
            ordinal = 0))
    private BlockPos getContainerColor(BlockPos original) {
        if (isRendering)
            containerColor = OutlineRenderManager.INSTANCE.blockEntityColorMap.get(original);

        return original; // No actual modification.
    }



    /**
     * Switches the {@link VertexConsumerProvider} used for the current block entity to the outline provider.
     */
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
    private VertexConsumerProvider useOutlineProvider(VertexConsumerProvider original) {
        if (!isRendering || containerColor == null) return original;

        return RenderUtil.modifyOutlineProviderColor(outlineProvider, containerColor);
    }



    /**
     * Allows animated block entities in the outline render layer buffer to render with the block crumbling overlay.
     */
    @Inject(
        // Compiler yells at this method descriptor for some reason, but I don't care...
        method = "method_22986(Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;",
        at = @At("RETURN"),
        cancellable = true)
    private static void unionOutlineConsumer(
        VertexConsumerProvider.Immediate immediate, VertexConsumer vertexConsumer, RenderLayer renderLayer,
        CallbackInfoReturnable<VertexConsumer> cir)
    {
        if (!isRendering || containerColor == null) return;

        var outlineConsumer = outlineProvider.getBuffer(renderLayer);
        cir.setReturnValue(renderLayer.hasCrumbling() ?
            VertexConsumers.union(vertexConsumer, outlineConsumer) : outlineConsumer);
    }
}
