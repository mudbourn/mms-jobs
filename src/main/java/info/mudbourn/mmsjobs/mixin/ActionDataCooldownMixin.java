package info.mudbourn.mmsjobs.mixin;

import com.daqem.arc.api.action.IActionType;
import com.daqem.arc.api.action.data.IActionDataType;
import com.daqem.arc.api.action.result.ActionResult;
import com.daqem.arc.data.ActionData;
import info.mudbourn.mmsjobs.CombatLogBridge;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown.WeaponClass;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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

    @Unique
    private static final Identifier mmsCompat$HURT_PLAYER = Identifier.fromNamespaceAndPath("arc", "on_hurt_player");

    @Shadow private IActionType<?> actionType;

    @Inject(method = "sendToAction", at = @At("HEAD"))
    private void mmsCompat$captureCooldownType(CallbackInfoReturnable<ActionResult> cir) {
        if (actionType == null) return;
        Identifier id = actionType.getIdentifier();
        JobsPlusActionCooldown.setCooldownType(id);
        JobsPlusActionCooldown.setWeaponClass(mmsCompat$weaponFor(id));
        JobsPlusActionCooldown.setPvpDenied(mmsCompat$pvpDenied(id));
    }

    @Inject(method = "sendToAction", at = @At("RETURN"))
    private void mmsCompat$releaseCooldownType(CallbackInfoReturnable<ActionResult> cir) {
        JobsPlusActionCooldown.clearCooldownType();
    }

    @Unique
    private static boolean mmsCompat$isCombat(Identifier id) {
        return mmsCompat$KILL_ENTITY.equals(id) || mmsCompat$HURT_ENTITY.equals(id) || mmsCompat$HURT_PLAYER.equals(id);
    }

    // Whether a player-on-player combat grant involves someone with PvP off.
    @Unique
    private boolean mmsCompat$pvpDenied(Identifier actionTypeId) {
        if (!mmsCompat$isCombat(actionTypeId)) return false;
        ActionData data = (ActionData) (Object) this;
        if (!(data.getData(IActionDataType.ENTITY) instanceof ServerPlayer target)) return false;
        if (!CombatLogBridge.isPvpEnabled(target)) return true;
        return data.getPlayer().arc$getPlayer() instanceof ServerPlayer actor
            && actor != target
            && !CombatLogBridge.isPvpEnabled(actor);
    }

    /**
     * Classifies the weapon behind a combat grant from its damage source.
     *
     * <p>A melee hit without a held weapon, or a hit with no direct entity,
     * counts as unarmed. Non-combat actions and grants without a damage source
     * return {@link WeaponClass#NONE}.</p>
     */
    @Unique
    private WeaponClass mmsCompat$weaponFor(Identifier actionTypeId) {
        if (!mmsCompat$isCombat(actionTypeId)) {
            return WeaponClass.NONE;
        }
        DamageSource source = ((ActionData) (Object) this).getData(IActionDataType.DAMAGE_SOURCE);
        if (source == null) return WeaponClass.NONE;
        Entity direct = source.getDirectEntity();
        if (direct == null) return WeaponClass.UNARMED;
        boolean projectile = direct instanceof Projectile;
        if (!projectile && direct instanceof LivingEntity attacker && !mmsCompat$isMeleeWeapon(attacker.getMainHandItem())) {
            return WeaponClass.UNARMED;
        }
        String entityId = EntityType.getKey(direct.getType()).toString();
        return JobsPlusActionCooldown.classifyWeapon(entityId, direct instanceof AbstractArrow, projectile);
    }

    // A melee weapon is any held stack that carries an attack damage modifier.
    @Unique
    private static boolean mmsCompat$isMeleeWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
            .modifiers()
            .stream()
            .anyMatch(entry -> entry.attribute().is(Attributes.ATTACK_DAMAGE));
    }
}
