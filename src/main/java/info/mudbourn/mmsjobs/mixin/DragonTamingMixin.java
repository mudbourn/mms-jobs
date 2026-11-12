package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.DragonBond;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Beastmaster-only dragon taming.
@Mixin(
    targets = {
        "nordmods.uselessreptile.common.entity.base.URDragonEntity",
        "nordmods.uselessreptile.common.entity.LightningChaser"
    },
    remap = false
)
public class DragonTamingMixin {

    @Inject(
        method = "method_5992(Lnet/minecraft/class_1657;Lnet/minecraft/class_1268;)Lnet/minecraft/class_1269;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void mmsJobs$beastmasterOnly(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (DragonBond.blockTaming((TamableAnimal) (Object) this, player, player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
