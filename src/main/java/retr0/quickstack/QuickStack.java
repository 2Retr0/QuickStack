package retr0.quickstack;

import ladysnake.satin.api.managed.ManagedFramebuffer;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockApplyCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retr0.quickstack.network.PacketRegistry;

public class QuickStack implements ModInitializer {
    public static final String MOD_ID = "quickstack";
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean renderingBlit = false;
    // literally the same as minecraft's blit, we are just checking that custom paths work
    public static final ManagedShaderEffect sobelEffect = ShaderEffectManager.getInstance().manage(new Identifier(MOD_ID, "shaders/post/block_outline.json"));
    public static final ManagedFramebuffer sobelBuffer = sobelEffect.getTarget("final");

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // TODO: BLOCK AND INVENTORY HIGHLIGHTING
        // TODO: PATH FINDING FOR VALID QUICKSTACK
        // TODO: ITEM FAVORITEING
        LOGGER.info("Hello Fabric world!");

        PacketRegistry.registerC2SPackets();
        PacketRegistry.registerS2CPackets();

        // CONFIG IDEAS:
        // * ALLOW HOTBAR QUICKSTACK
        // * REQUIRE EXACT NBT
        // * SEARCH RADIUS

        ClientPickBlockApplyCallback.EVENT.register((player, result, stack) -> {
            renderingBlit = !renderingBlit;
            return stack;
        });



//        RenderLayer blockRenderLayer = sobelBuffer.getRenderLayer(RenderLayer.getTranslucent());
//        RenderLayerHelper.registerBlockRenderLayer(blockRenderLayer);

        // WorldRenderEvents.LAST.register(context -> {
        //     var blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        //     var blockState = Blocks.DIRT.getDefaultState();
        //     var bakedModel = blockRenderManager.getModel(blockState);
        //
        //     var vertexConsumerProvider = context.consumers();
        //     var matrixStack = context.matrixStack();
        //     var camera = context.camera();
        //     var cameraPos = camera.getPos();
        //
        //     var blockPos = new BlockPos(300, 64, -150);
        //
        //     if (vertexConsumerProvider == null)
        //         LOGGER.info("CONSUMER PROVIDER WAS NULL!");
        //     else {
        //         var vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getOutline(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        //
        //         matrixStack.push();
        //         matrixStack.translate(blockPos.getX() - cameraPos.x, blockPos.getY() - cameraPos.y, blockPos.getZ() - cameraPos.z);
        //
        //         blockRenderManager.getModelRenderer().render(matrixStack.peek(), vertexConsumer, blockState, bakedModel, 0.0f, 0.0f, 0.0f, 0x333333, 0xFFFFFF);
        //         // blockRenderManager.renderBlockAsEntity(blockState, matrixStack, vertexConsumerProvider, 0xF000F0, OverlayTexture.DEFAULT_UV);
        //
        //         matrixStack.pop();
        //     }
        //     //     protected static final Target OUTLINE_TARGET = new Target("outline_target", () -> MinecraftClient.getInstance().worldRenderer.getEntityOutlinesFramebuffer().beginWrite(false), () -> MinecraftClient.getInstance().getFramebuffer().beginWrite(false));
        // });

        var client = MinecraftClient.getInstance();

//        You would have to make your own render layer and use a new shader. You then also have to add a new uniform, so you can give the nbt value to the shader.
//        Afterwards, you can set the block to use this new render layer, add it to the block render layers and inject a immediate.draw call for that render layer into WorldRenderer.render.


//        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
//            var immediate = (VertexConsumerProvider.Immediate) context.consumers();
//
//            if (immediate == null) return;
//
//            sobelBuffer.beginWrite(false);
//        });
//
        /*BlockEntityRenderEvents.END.register(() -> {
            RenderSystem.enableBlend();
            sobelBuffer.draw(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), false);
            RenderSystem.disableBlend();
        });*/
//
//        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(((context, hitResult) -> {
//            client.getFramebuffer().beginWrite(false);
//
//            RenderSystem.enableBlend();
//            sobelBuffer.draw(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), false);
//            RenderSystem.disableBlend();
//
//            return true;
//        }));

        /*ShaderEffectRenderCallback.EVENT.register(tickDelta -> {
            if (!renderingBlit) return;

            // CANT DRAW SEPARATELY MAYBE BUFFER BUILDER THE CHEST GLOW THEN RENDER NORMALLY BEFORE RENDING BUILT FRAMEBUFFER

            sobelEffect.render(tickDelta);
            client.getFramebuffer().beginWrite(false);

            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ZERO,
                GlStateManager.DstFactor.ONE);
            sobelBuffer.draw(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), false);
            sobelBuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            client.getFramebuffer().beginWrite(false);
            RenderSystem.disableBlend();
        });*/
    }
}
