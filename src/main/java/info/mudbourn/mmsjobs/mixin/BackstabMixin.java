package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.RogueStealth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Rogue Backstab damage and breaking Shadow Cloak on attack.
@Mixin(LivingEntity.class)
public class BackstabMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float mmsJobs$backstab(float amount, ServerLevel level, DamageSource source) {
        return RogueStealth.backstab((LivingEntity) (Object) this, source, amount);
    }
}
