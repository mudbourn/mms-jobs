package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import com.daqem.jobsplus.player.job.powerup.JobPowerupManager;
import com.daqem.jobsplus.player.job.powerup.PowerupState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * Reads how far a player has bought into a three-rank Jobs+ powerup chain,
 * one rank higher when their Origins class matches the job.
 */
public final class PowerupRanks {

    private static final Map<String, String> CLASS_FOR_JOB = Map.of(
        "hunter", "archer",
        "smith", "blacksmith",
        "alchemist", "cleric"
    );

    private PowerupRanks() {}

    /**
     * @return the bought rank from {@link #baseRank}, plus one when the player
     *         owns at least rank 1 and their class matches the job, so 0 to 4
     */
    public static int rank(Player player, String job, String powerup) {
        int base = baseRank(player, job, powerup);
        if (base == 0 || !(player instanceof ServerPlayer serverPlayer)) return base;
        return ClassSynergy.has(serverPlayer, classFor(job)) ? base + 1 : base;
    }

    // The Origins class id that pairs with a job id.
    public static String classFor(String job) {
        return CLASS_FOR_JOB.getOrDefault(job, job);
    }

    /**
     * @return 3, 2 or 1 for the highest active {@code jobsplus:<job>/<powerup>_iii},
     *         {@code _ii} or {@code _i} rank, otherwise 0
     */
    public static int baseRank(Player player, String job, String powerup) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return 0;

        Job held = jobsPlayer.jobsplus$getJob(Identifier.fromNamespaceAndPath("jobsplus", job));
        if (held == null) return 0;

        JobPowerupManager powerups = held.getPowerupManager();
        if (isActive(powerups, job, powerup + "_iii")) return 3;
        if (isActive(powerups, job, powerup + "_ii")) return 2;
        if (isActive(powerups, job, powerup + "_i")) return 1;
        return 0;
    }

    private static boolean isActive(JobPowerupManager powerups, String job, String powerup) {
        return powerups.getPowerup(Identifier.fromNamespaceAndPath("jobsplus", job + "/" + powerup))
            .map(p -> p.getState() == PowerupState.ACTIVE)
            .orElse(false);
    }
}
