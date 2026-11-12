package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.DragonBond;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Untamed dragons leave Beastmasters alone unless provoked.
@Mixin(Mob.class)
public class DragonDocileMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void mmsJobs$spareBeastmaster(LivingEntity target, CallbackInfo ci) {
        if (target != null && DragonBond.ignoresTarget((Mob) (Object) this, target)) ci.cancel();
    }
}
