package retr0.quickstack.compat.sodium.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import retr0.quickstack.util.OutlineColorManager;
import retr0.quickstack.util.RenderUtil;

import java.util.SortedSet;

@Pseudo @Mixin(SodiumWorldRenderer.class)
public abstract class MixinSodiumWorldRenderer {
    @Unique private static boolean isRendering;
    @Unique private static OutlineVertexConsumerProvider outlineProvider;
    @Unique private static int containerColor;

    /**
     * Caches the outline vertex consumer provider.
     */
    @Inject(method = "renderTileEntities", at = @At("HEAD"))
    private void cacheOutlineProvider(
        MatrixStack matrices, BufferBuilderStorage bufferBuilders, Long2ObjectMap<SortedSet<BlockBreakingInfo>> progress,
        Camera camera, float tickDelta, CallbackInfo ci)
    {
        outlineProvider = bufferBuilders.getOutlineVertexConsumers();
        isRendering = OutlineColorManager.getInstance().isRendering();
    }



    /**
     * Caches the container color (if it exists) for the current block entity.
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(
        method = "renderTileEntities",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/block/entity/BlockEntity;getPos()Lnet/minecraft/util/math/BlockPos;",
            ordinal = 0))
    private BlockPos cacheContainerColor(BlockPos original) {
        if (isRendering)
            containerColor = OutlineColorManager.getInstance().getBlockOutlineColor(original);

        return original; // No actual modification.
    }



    /**
     * Switches the {@link VertexConsumerProvider} used for the current block entity to the outline provider.
     */
    /* [ 16]    [  0]                     BlockEntity  blockEntity                                         -         */
    /* [ 17]    [  0]                        BlockPos  pos                                                 -         */
    /* [ 18]    [  0]          VertexConsumerProvider  consumer                                          >>YES<<     */
    @ModifyVariable(
        method = "renderTileEntities",
        at = @At(
            value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;",
            shift = At.Shift.AFTER),
        ordinal = 0, remap = false)
    private VertexConsumerProvider useOutlineProvider(
        VertexConsumerProvider original, MatrixStack matrices, BufferBuilderStorage bufferBuilders)
    {
        if (!isRendering || containerColor == 0) return original;

        return RenderUtil.modifyOutlineProviderColor(outlineProvider, containerColor);
    }



    /**
     * Allows animated block entities in the outline render layer buffer to render with the block crumbling overlay.
     */
    @Inject(
        method = "lambda$renderTileEntities$0",
        at = @At("RETURN"),
        cancellable = true,
        remap = false)
    private static void unionOutlineConsumer(
        VertexConsumer vertexConsumer, VertexConsumerProvider.Immediate immediate, RenderLayer renderLayer,
        CallbackInfoReturnable<VertexConsumer> cir)
    {
        if (!isRendering || containerColor == 0) return;

        var outlineConsumer = outlineProvider.getBuffer(renderLayer);
        cir.setReturnValue(renderLayer.hasCrumbling() ?
            VertexConsumers.union(vertexConsumer, outlineConsumer) : outlineConsumer);
    }
}
