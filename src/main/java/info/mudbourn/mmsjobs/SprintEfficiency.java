package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobManager;
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
 * Explorer "Sprint Efficiency" as a real movement-speed bonus instead of a potion effect.
 *
 * <p>The powerup used to be an {@code arc:on_sprint} action that re-applied a short
 * {@code minecraft:speed} effect every tick. That surfaced as a status effect: it showed
 * an (icon-less) buff, could be cleared by milk/death, refreshed and faded on its own
 * duration, and stacked awkwardly with real Speed potions. arc has no attribute-modifier
 * reward, so the effect was the only way to fake "while sprinting" purely in data.</p>
 *
 * <p>Instead we manage a <b>transient</b> {@code movement_speed} modifier directly. It is
 * present only while the player owns the powerup <i>and</i> is sprinting, and is removed the
 * instant either stops. Transient modifiers are never written to the player file, so logging
 * out mid-sprint cannot leave the bonus stuck on. The magnitude matches the effect it
 * replaces exactly — vanilla Speed adds {@code 0.2 * level} as {@code ADD_MULTIPLIED_TOTAL},
 * so rank I is +20% and rank II is +40%.</p>
 *
 * <p>Only wired up when Jobs+ is present (see {@link MmsJobs}); this class references Jobs+
 * types directly, so it must not load when the mod is absent.</p>
 */
public final class SprintEfficiency {

    private static final Identifier EXPLORER_JOB =
        Identifier.fromNamespaceAndPath("jobsplus", "explorer");
    private static final Identifier POWERUP_I =
        Identifier.fromNamespaceAndPath("jobsplus", "explorer/sprint_efficiency_i");
    private static final Identifier POWERUP_II =
        Identifier.fromNamespaceAndPath("jobsplus", "explorer/sprint_efficiency_ii");

    /** Stable id for our modifier, so we can find/update/remove exactly our own. */
    private static final Identifier MODIFIER_ID =
        Identifier.fromNamespaceAndPath("mms_jobs", "sprint_efficiency");

    // Match vanilla Speed I / II (0.2 * level, ADD_MULTIPLIED_TOTAL).
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

        double amount = player.isSprinting() ? bonusFor(player) : 0.0;

        if (amount <= 0.0) {
            speed.removeModifier(MODIFIER_ID);
            return;
        }

        AttributeModifier current = speed.getModifier(MODIFIER_ID);
        if (current == null || current.amount() != amount) {
            // addOrUpdate replaces cleanly when switching between rank I and II.
            speed.addOrUpdateTransientModifier(
                new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** 0 if the player is not an Explorer with a Sprint Efficiency powerup active. */
    private static double bonusFor(ServerPlayer player) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return 0.0;

        JobInstance explorer = JobManager.getInstance().getJobs().get(EXPLORER_JOB);
        if (explorer == null) return 0.0;

        Job job = jobsPlayer.jobsplus$getJob(explorer);
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
