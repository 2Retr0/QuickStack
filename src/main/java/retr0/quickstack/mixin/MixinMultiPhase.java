package retr0.quickstack.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.client.render.RenderLayer$MultiPhase")
public class MixinMultiPhase extends RenderLayer {
    // @Shadow @Final @Mutable
    // static BiFunction<Identifier, RenderPhase.Cull, RenderLayer> CULLING_LAYERS = Util.memoize((texture, culling) -> RenderLayer.of("outline", VertexFormats.POSITION_COLOR_TEXTURE, VertexFormat.DrawMode.QUADS, 256, false, true, RenderLayer.MultiPhaseParameters.builder().shader(OUTLINE_SHADER).texture(new RenderPhase.Texture(texture, false, false)).cull(culling).depthTest(ALWAYS_DEPTH_TEST).target(OUTLINE_TARGET).build(RenderLayer.OutlineMode.IS_OUTLINE)));


    public MixinMultiPhase(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }
}
