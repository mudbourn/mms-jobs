package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import com.daqem.jobsplus.player.job.powerup.JobPowerupManager;
import com.daqem.jobsplus.player.job.powerup.PowerupState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Explorer "Sprint Efficiency" as a transient {@code movement_speed} modifier.
 *
 * <p>The modifier is present only while the player owns the powerup and is
 * sprinting. Rank I adds 20% and rank II adds 40% as
 * {@code ADD_MULTIPLIED_TOTAL}, matching vanilla Speed I and II.</p>
 *
 * <p>References Jobs+ types directly, so {@link MmsJobs} registers it only
 * when Jobs+ is loaded.</p>
 */
public final class SprintEfficiency {

    private static final Identifier EXPLORER_JOB =
        Identifier.fromNamespaceAndPath("jobsplus", "explorer");
    private static final Identifier POWERUP_I =
        Identifier.fromNamespaceAndPath("jobsplus", "explorer/sprint_efficiency_i");
    private static final Identifier POWERUP_II =
        Identifier.fromNamespaceAndPath("jobsplus", "explorer/sprint_efficiency_ii");

    /** Id of the movement-speed modifier this class owns. */
    private static final Identifier MODIFIER_ID =
        Identifier.fromNamespaceAndPath("mms_jobs", "sprint_efficiency");

    // Vanilla Speed I and II amounts
    private static final double AMOUNT_I = 0.20;
    private static final double AMOUNT_II = 0.40;

    private SprintEfficiency() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                apply(player);
            }
        });
    }

    private static void apply(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        AttributeModifier current = speed.getModifier(MODIFIER_ID);
        double amount = player.isSprinting() ? bonusFor(player) : 0.0;

        if (amount <= 0.0) {
            if (current != null) speed.removeModifier(MODIFIER_ID);
            return;
        }

        if (current == null || current.amount() != amount) {
            speed.addOrUpdateTransientModifier(
                new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** 0 if the player is not an Explorer with a Sprint Efficiency powerup active. */
    private static double bonusFor(ServerPlayer player) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return 0.0;

        Job job = jobsPlayer.jobsplus$getJob(EXPLORER_JOB);
        if (job == null) return 0.0;

        JobPowerupManager powerups = job.getPowerupManager();
        if (isActive(powerups, POWERUP_II)) return AMOUNT_II;
        if (isActive(powerups, POWERUP_I)) return AMOUNT_I;
        return 0.0;
    }

    private static boolean isActive(JobPowerupManager powerups, Identifier id) {
        return powerups.getPowerup(id)
            .map(p -> p.getState() == PowerupState.ACTIVE)
            .orElse(false);
    }
}
