package info.mudbourn.mmsjobs;

import com.daqem.arc.api.action.IAction;
import com.daqem.arc.api.action.IActionType;
import com.daqem.arc.api.reward.IReward;
import com.daqem.jobsplus.integration.arc.reward.rewards.job.JobExpReward;
import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown.WeaponClass;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

// Pays Jobs+ XP for events that Arc does not report, through the same gate as Arc grants.
public final class JobXpGrants {

    // Cooldown category of a semi-passive grant
    public static final Identifier EASY = Identifier.fromNamespaceAndPath("arc", "on_interact_entity");

    // Cooldown category of an active grant
    public static final Identifier HARD = Identifier.fromNamespaceAndPath("arc", "on_hurt_player");

    private JobXpGrants() {}

    // Grants the XP range that a job's data gives a source to a holder of that job, unless on cooldown.
    public static void grant(ServerPlayer player, String job, Identifier category, IActionType<?> source) {
        grant(player, job, category, source, false);
    }

    // As above, with the PvP-denied flag the cooldown mixin reads for player-on-player combat.
    public static void grant(
        ServerPlayer player,
        String job,
        Identifier category,
        IActionType<?> source,
        boolean pvpDenied
    ) {
        Job held = held(player, job);
        double xp = roll(player, held, source);
        if (xp <= 0) return;

        JobsPlusActionCooldown.beginAction(category);
        JobsPlusActionCooldown.setPvpDenied(pvpDenied);
        try {
            held.addExperience(xp);
        } finally {
            JobsPlusActionCooldown.endAction();
        }
    }

    // Grants a combat bonus on top of the kill that earned it, outside the cooldown but weapon-gated and scaled.
    public static void bonus(ServerPlayer player, String job, IActionType<?> source, WeaponClass weapon, double scale) {
        Job held = held(player, job);
        double xp = roll(player, held, source);
        if (xp <= 0) return;

        JobsPlusActionCooldown.beginAction(source.getIdentifier());
        JobsPlusActionCooldown.setBonus(true);
        JobsPlusActionCooldown.setWeaponClass(weapon);
        JobsPlusActionCooldown.setCombatScale(scale);
        try {
            held.addExperience(xp);
        } finally {
            JobsPlusActionCooldown.endAction();
        }
    }

    private static Job held(ServerPlayer player, String job) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return null;
        return jobsPlayer.jobsplus$getJob(Identifier.fromNamespaceAndPath("jobsplus", job));
    }

    private static double roll(ServerPlayer player, Job held, IActionType<?> source) {
        if (held == null) return 0;

        for (IAction action : held.getJobInstance().getActions()) {
            if (action.getType() != source) continue;
            for (IReward reward : action.getRewards()) {
                if (reward instanceof JobExpReward range) {
                    return range.getMin() + (range.getMax() - range.getMin()) * player.getRandom().nextDouble();
                }
            }
        }
        return 0;
    }
}
