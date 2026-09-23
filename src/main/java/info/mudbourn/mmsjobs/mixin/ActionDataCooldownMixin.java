package info.mudbourn.mmsjobs.mixin;

import com.daqem.arc.api.action.IActionType;
import com.daqem.arc.api.action.data.IActionDataType;
import com.daqem.arc.api.action.result.ActionResult;
import com.daqem.arc.data.ActionData;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown.WeaponClass;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the ARC action type before {@code sendToAction()} runs and stores
 * the cooldown category in a ThreadLocal so the companion
 * {@link JobXpCooldownMixin} can read it.
 */
@Mixin(ActionData.class)
public class ActionDataCooldownMixin {

    @Unique
    private static final Identifier mmsCompat$KILL_ENTITY = Identifier.fromNamespaceAndPath("arc", "on_kill_entity");

    @Unique
    private static final Identifier mmsCompat$HURT_ENTITY = Identifier.fromNamespaceAndPath("arc", "on_hurt_entity");

    @Shadow private IActionType<?> actionType;

    @Inject(method = "sendToAction", at = @At("HEAD"))
    private void mmsCompat$captureCooldownType(CallbackInfoReturnable<ActionResult> cir) {
        if (actionType == null) return;
        Identifier id = actionType.getIdentifier();
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
    @Unique
    private WeaponClass mmsCompat$weaponFor(Identifier actionTypeId) {
        if (!mmsCompat$KILL_ENTITY.equals(actionTypeId) && !mmsCompat$HURT_ENTITY.equals(actionTypeId)) {
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
