package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.ShieldBlocking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pays Warrior XP when a player blocks damage with a warrior shield item.
 */
@Mixin(LivingEntity.class)
public class ShieldBlockingMixin {

    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void mmsJobs$shieldBlocked(
        ServerLevel level,
        DamageSource source,
        float damage,
        CallbackInfoReturnable<Float> cir
    ) {
        ShieldBlocking.blocked((LivingEntity) (Object) this, source, cir.getReturnValueF());
    }
}
