package retr0.quickstack.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(WorldRenderer.class)
public interface AccessorWorldRenderer {
    @Accessor
    BlockEntityRenderDispatcher getBlockEntityRenderDispatcher();

    @Accessor
    Set<BlockEntity> getNoCullingBlockEntities();
}
