package retr0.quickstack.mixin;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;

@Mixin(ShapeContext.class)
public interface MixinShapeContext {
    /**
     * Changes method to permit null as a valid argument.
     * @see RaycastContext#RaycastContext(Vec3d, Vec3d, ShapeType, FluidHandling, Entity)
     */
    @Inject(at = @At(value = "HEAD"), method = "of", cancellable = true)
    private static void permitNull(Entity entity, CallbackInfoReturnable<ShapeContext> ci) {
        if (entity != null) return;

        ci.setReturnValue(ShapeContext.absent());
        ci.cancel();
    }
}

