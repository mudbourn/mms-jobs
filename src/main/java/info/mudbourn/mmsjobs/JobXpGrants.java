package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

// Pays Jobs+ XP for events that Arc does not report, through the same cooldown gate as Arc grants.
public final class JobXpGrants {

    // Cooldown category of a semi-passive grant
    public static final Identifier EASY = Identifier.fromNamespaceAndPath("arc", "on_interact_entity");

    // Cooldown category of an active grant
    public static final Identifier HARD = Identifier.fromNamespaceAndPath("arc", "on_hurt_player");

    private JobXpGrants() {}

    // Grants a random amount between min and max to a job the player holds, unless on cooldown.
    public static void grant(ServerPlayer player, String job, Identifier category, double min, double max) {
        grant(player, job, category, min, max, false);
    }

    // As above, with the PvP-denied flag the cooldown mixin reads for player-on-player combat.
    public static void grant(
        ServerPlayer player,
        String job,
        Identifier category,
        double min,
        double max,
        boolean pvpDenied
    ) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return;

        Job held = jobsPlayer.jobsplus$getJob(Identifier.fromNamespaceAndPath("jobsplus", job));
        if (held == null) return;

        double xp = min + (max - min) * player.getRandom().nextDouble();
        JobsPlusActionCooldown.beginAction(category);
        JobsPlusActionCooldown.setPvpDenied(pvpDenied);
        try {
            held.addExperience(xp);
        } finally {
            JobsPlusActionCooldown.endAction();
        }
    }
}
