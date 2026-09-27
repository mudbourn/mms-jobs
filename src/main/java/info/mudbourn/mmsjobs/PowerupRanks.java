package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import com.daqem.jobsplus.player.job.powerup.JobPowerupManager;
import com.daqem.jobsplus.player.job.powerup.PowerupState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Reads how far a player has bought into a two-rank Jobs+ powerup chain.
 */
public final class PowerupRanks {

    private PowerupRanks() {}

    /**
     * @return 2 if {@code jobsplus:<job>/<powerup>_ii} is active, 1 if the
     *         {@code _i} rank is, otherwise 0
     */
    public static int rank(Player player, String job, String powerup) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return 0;

        Job held = jobsPlayer.jobsplus$getJob(Identifier.fromNamespaceAndPath("jobsplus", job));
        if (held == null) return 0;

        JobPowerupManager powerups = held.getPowerupManager();
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
