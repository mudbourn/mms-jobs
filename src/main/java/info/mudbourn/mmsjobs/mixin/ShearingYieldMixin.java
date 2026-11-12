package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.PowerupHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives a Farmer a chance at bonus wool when shearing a sheep by hand.
 */
@Mixin(Sheep.class)
public class ShearingYieldMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.AFTER
        )
    )
    private void mmsJobs$bonusWool(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        PowerupHooks.sheared((Sheep) (Object) this, player);
    }
}
