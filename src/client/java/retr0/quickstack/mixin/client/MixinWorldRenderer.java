package retr0.quickstack.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import retr0.quickstack.util.OutlineColorManager;
import retr0.quickstack.util.RenderUtil;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Unique private static boolean isRendering;
    @Unique private static OutlineVertexConsumerProvider outlineProvider;
    @Unique private static int containerColor;
    @Shadow @Final private BufferBuilderStorage bufferBuilders;
    @Shadow @Final private MinecraftClient client;
    @Shadow private @Nullable ClientWorld world;



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
        if (!isRendering || containerColor == 0) return;

        var outlineConsumer = outlineProvider.getBuffer(renderLayer);
        cir.setReturnValue(renderLayer.hasCrumbling() ?
            VertexConsumers.union(vertexConsumer, outlineConsumer) : outlineConsumer);
    }



    /**
     * Adds additional logic to enable the outline shader if any blocks are set to have an outline. ALso caches the
     * outline provider.
     */
    /* [ 22]                                        -                                                                */
    /* [ 23]    [  2]                         boolean  bl3                                               >>YES<<     */
    /* [ 24]    [  0]  VertexConsumerProvider$Immediate  immediate                                           -       */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "render",
        at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0, shift = At.Shift.BEFORE),
        ordinal = 2)
    private boolean shouldRenderOutline(boolean original) {
        outlineProvider = bufferBuilders.getOutlineVertexConsumers();
        isRendering = OutlineColorManager.getInstance().isRendering();

        return original || isRendering;
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

        RenderUtil.drawBlockModelOutlines(client, matrices, camera, world, outlineProvider);
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
            containerColor = OutlineColorManager.getInstance().getBlockOutlineColor(original);

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
    private VertexConsumerProvider useOutlineProviderBlockEntities(VertexConsumerProvider original) {
        if (!isRendering || containerColor == 0) return original;

        return RenderUtil.modifyOutlineProviderColor(outlineProvider, containerColor);
    }



    /**
     * Switches the {@link VertexConsumerProvider} used for the current entity to the outline provider. This overrides
     * the color of the entity if it is already glowing, but does not change whether the entity is <i>marked</i> as
     * glowing or not.
     */
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderEntity(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"),
        index = 6)
    private VertexConsumerProvider useOutlineProviderEntities(
        Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices,
        VertexConsumerProvider original)
    {
        if (!isRendering) return original;

        var entityColor = OutlineColorManager.getInstance().getEntityOutlineColor(entity.getUuid());

        if (entityColor != 0)
            return RenderUtil.modifyOutlineProviderColor(outlineProvider, entityColor);
        return original;
    }
}
