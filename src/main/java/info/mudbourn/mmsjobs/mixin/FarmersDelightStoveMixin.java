package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.FarmersDelightCook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pays Cook XP to the player who last put food on a Stove when it finishes cooking.
 */
@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.AbstractStoveBlockEntity")
public class FarmersDelightStoveMixin {

    @Inject(method = "placeFood", at = @At("HEAD"))
    private void mmsJobs$loaded(
        Entity entity,
        ItemStack stack,
        RecipeHolder<?> recipe,
        CallbackInfoReturnable<Boolean> cir
    ) {
        FarmersDelightCook.loaded((BlockEntity) (Object) this, entity);
    }

    @Inject(
        method = "cookAndOutputItems",
        at = @At(
            value = "INVOKE",
            target = "Lvectorwing/farmersdelight/common/utility/ItemUtils;spawnItemEntity(Lnet/minecraft/class_1937;Lnet/minecraft/class_1799;DDDDDD)V"
        )
    )
    private void mmsJobs$finished(ServerLevel level, CallbackInfo ci) {
        FarmersDelightCook.finished((BlockEntity) (Object) this);
    }
}
