package info.mudbourn.mmsjobs.mixin;

import com.daqem.arc.api.action.IActionType;
import com.daqem.arc.api.action.data.IActionDataType;
import com.daqem.arc.api.action.result.ActionResult;
import com.daqem.arc.data.ActionData;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown.WeaponClass;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the ARC action type before {@code sendToAction()} runs and stores
 * the cooldown category in a ThreadLocal so the companion
 * {@link JobXpCooldownMixin} can read it.
 *
 * <p>This avoids the need for the XP mixin to depend on ARC classes — it only
 * reads the pre-computed {@link JobsPlusActionCooldown.CooldownCategory}.</p>
 */
@Mixin(ActionData.class)
public class ActionDataCooldownMixin {

    @Shadow private IActionType<?> actionType;

    @Inject(method = "sendToAction", at = @At("HEAD"))
    private void mmsCompat$captureCooldownType(CallbackInfoReturnable<ActionResult> cir) {
        if (actionType == null) return;
        String id = actionType.getIdentifier().toString();
        JobsPlusActionCooldown.setCooldownType(id);
        JobsPlusActionCooldown.setWeaponClass(mmsCompat$weaponFor(id));
    }

    /**
     * Classifies the weapon behind a combat grant from its damage source.
     *
     * <p>Any projectile, whether shot like an arrow or thrown like a trident,
     * counts as ranged (archer); a direct melee hit counts as melee (warrior).
     * Non-combat actions and grants without a damage source return
     * {@link WeaponClass#NONE}.</p>
     */
    @org.spongepowered.asm.mixin.Unique
    private WeaponClass mmsCompat$weaponFor(String actionTypeId) {
        if (!actionTypeId.equals("arc:on_kill_entity") && !actionTypeId.equals("arc:on_hurt_entity")) {
            return WeaponClass.NONE;
        }
        DamageSource source = ((ActionData) (Object) this).getData(IActionDataType.DAMAGE_SOURCE);
        if (source == null) return WeaponClass.NONE;
        Entity direct = source.getDirectEntity();
        if (direct == null) return WeaponClass.MELEE;
        boolean projectile = direct instanceof Projectile;
        String entityId = EntityType.getKey(direct.getType()).toString();
        return JobsPlusActionCooldown.classifyWeapon(entityId, projectile);
    }
}
