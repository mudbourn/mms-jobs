package info.mudbourn.mmsjobs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Rogue Backstab and Shadow Cloak.
public final class RogueStealth {
    private static final int CLOAK_TICKS = 15;
    private static final double BEHIND_DOT = -0.3;

    private static final Map<UUID, Integer> SNEAK_TICKS = new HashMap<>();
    private static final Set<UUID> CLOAKED = new HashSet<>();

    private RogueStealth() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickCloak(player);
            }
        });
    }

    // Raises melee damage from a Rogue who strikes a target from behind or unnoticed.
    public static float backstab(LivingEntity target, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker) || source.getDirectEntity() != attacker) return amount;
        if (attacker == target) return amount;

        reveal(attacker);
        float bonus = switch (PowerupRanks.rank(attacker, "rogue", "backstab")) {
            case 4 -> 0.65f;
            case 3 -> 0.5f;
            case 2 -> 0.35f;
            case 1 -> 0.2f;
            default -> 0.0f;
        };
        if (bonus == 0.0f || !unaware(target, attacker)) return amount;

        JobXpGrants.grant(attacker, "rogue", JobXpGrants.HARD, JobXpSource.BACKSTAB);
        return amount * (1.0f + bonus);
    }

    private static boolean unaware(LivingEntity target, ServerPlayer attacker) {
        if (target instanceof Mob mob && mob.getTarget() != attacker) return true;

        Vec3 facing = target.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 toAttacker = attacker.position().subtract(target.position()).multiply(1, 0, 1).normalize();
        return facing.dot(toAttacker) < BEHIND_DOT;
    }

    // Starts the sneak timer over after the Rogue attacks.
    private static void reveal(ServerPlayer player) {
        SNEAK_TICKS.remove(player.getUUID());
        uncloak(player);
    }

    private static void tickCloak(ServerPlayer player) {
        int delay = switch (PowerupRanks.rank(player, "rogue", "shadow_cloak")) {
            case 4, 3 -> 0;
            case 2 -> 30;
            case 1 -> 60;
            default -> -1;
        };
        if (delay < 0 || !player.isShiftKeyDown() || player.isSpectator()) {
            SNEAK_TICKS.remove(player.getUUID());
            uncloak(player);
            return;
        }

        int ticks = SNEAK_TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (ticks < delay) return;

        MobEffectInstance current = player.getEffect(MobEffects.INVISIBILITY);
        if (current != null && current.getDuration() > CLOAK_TICKS) return;

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, CLOAK_TICKS, 0, false, false, true));
        CLOAKED.add(player.getUUID());
    }

    private static void uncloak(ServerPlayer player) {
        if (!CLOAKED.remove(player.getUUID())) return;

        MobEffectInstance current = player.getEffect(MobEffects.INVISIBILITY);
        if (current != null && current.getDuration() <= CLOAK_TICKS) player.removeEffect(MobEffects.INVISIBILITY);
    }

    public static void forget(UUID playerId) {
        SNEAK_TICKS.remove(playerId);
        CLOAKED.remove(playerId);
    }
}
