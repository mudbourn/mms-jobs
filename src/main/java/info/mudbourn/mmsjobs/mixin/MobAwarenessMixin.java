package info.mudbourn.mmsjobs.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import info.mudbourn.mmsjobs.PetBond;
import info.mudbourn.mmsjobs.PowerupHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lowers how visible a sneaking Rogue is to mobs, and a cat-bonded Beastmaster to creepers.
 */
@Mixin(LivingEntity.class)
public class MobAwarenessMixin {

    @ModifyReturnValue(method = "getVisibilityPercent", at = @At("RETURN"))
    private double mmsJobs$sneakUnseen(double visibility, Entity looker) {
        LivingEntity self = (LivingEntity) (Object) this;
        return PetBond.visibility(self, looker, PowerupHooks.visibility(self, visibility));
    }
}
