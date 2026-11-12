package info.mudbourn.mmsjobs;

import info.mudbourn.mmsjobs.JobsPlusActionCooldown.WeaponClass;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import java.util.Set;

// Hunter long shots, Warrior boss kills and Beastmaster pet kills, paid when a mob dies.
public final class CombatBonuses {
    private static final double LONG_SHOT_BLOCKS = 30;
    private static final Set<EntityType<?>> BOSSES = Set.of(
        EntityType.ENDER_DRAGON,
        EntityType.WITHER,
        EntityType.ELDER_GUARDIAN,
        EntityType.WARDEN
    );

    private CombatBonuses() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(CombatBonuses::died);
    }

    private static void died(LivingEntity target, DamageSource source) {
        Entity killer = source.getEntity();
        if (killer instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner && target instanceof Enemy) {
            JobXpGrants.grant(owner, "beastmaster", JobXpGrants.HARD, JobXpSource.PET_KILL);
            return;
        }
        if (!(killer instanceof ServerPlayer player) || !(target instanceof Enemy)) return;

        Entity direct = source.getDirectEntity();
        WeaponClass weapon = weaponOf(direct, player);
        if (BOSSES.contains(target.getType())) {
            JobXpGrants.bonus(player, "warrior", JobXpSource.SLAY_BOSS, weapon, 1.0);
        }
        if (direct instanceof AbstractArrow && player.distanceTo(target) >= LONG_SHOT_BLOCKS) {
            JobXpGrants.bonus(player, "hunter", JobXpSource.LONG_SHOT, weapon, 1.0);
        }
    }

    // Weapon class of the killing blow, as the cooldown gate classifies it.
    private static WeaponClass weaponOf(Entity direct, ServerPlayer player) {
        if (direct == null) return WeaponClass.UNARMED;
        if (direct == player) return player.getMainHandItem().isEmpty() ? WeaponClass.UNARMED : WeaponClass.MELEE;
        String entityId = EntityType.getKey(direct.getType()).toString();
        return JobsPlusActionCooldown.classifyWeapon(entityId, direct instanceof AbstractArrow, direct instanceof Projectile);
    }
}
