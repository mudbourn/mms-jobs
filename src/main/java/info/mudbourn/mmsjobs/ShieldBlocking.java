package info.mudbourn.mmsjobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Warrior XP for blocking damage with a shield-type weapon.
public final class ShieldBlocking {

    private static final TagKey<Item> WARRIOR_SHIELDS = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("mms_jobs", "warrior_shields")
    );

    private ShieldBlocking() {}

    // Pays a warrior who blocked damage while holding a tagged item.
    public static void blocked(LivingEntity blocker, DamageSource source, float blockedDamage) {
        if (blockedDamage <= 0 || !(blocker instanceof ServerPlayer player)) return;

        ItemStack shield = player.getItemBlockingWith();
        if (shield == null || !shield.is(WARRIOR_SHIELDS)) return;

        JobXpGrants.grant(player, "warrior", JobXpGrants.HARD, 1, 3, pvpDenied(player, source));
    }

    // Whether either side of a player-on-player hit has PvP off.
    private static boolean pvpDenied(ServerPlayer blocker, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker == blocker) return false;
        return !CombatLogBridge.isPvpEnabled(blocker) || !CombatLogBridge.isPvpEnabled(attacker);
    }
}
