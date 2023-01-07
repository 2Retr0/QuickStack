package retr0.itemfavoriting.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import retr0.itemfavoriting.ItemFavoriting;

import java.util.Arrays;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity {
    @Unique private static final String DATA_TAG_NAME = "favoriteSlots";


    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readSortType(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(DATA_TAG_NAME)) {
            var favoriteSlots = nbt.getIntArray(DATA_TAG_NAME);
            ItemFavoriting.LOGGER.info(Arrays.toString(favoriteSlots));
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeSortType(NbtCompound nbt, CallbackInfo ci) {
        nbt.put(DATA_TAG_NAME, new NbtIntArray(new int[] { 1, 2, 3 }));
    }
}
