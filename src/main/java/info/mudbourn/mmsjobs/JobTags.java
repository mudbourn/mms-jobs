package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashSet;
import java.util.Set;

// Mirrors held jobs as entity tags (mms_jobs.<job>) and the class-paired job as the mms_jobs_class score.
public final class JobTags {
    private static final String PREFIX = "mms_jobs.";
    private static final String CLASS_OBJECTIVE = "mms_jobs_class";

    private JobTags() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            Objective objective = classObjective(server.getScoreboard());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sync(player);
                server.getScoreboard().getOrCreatePlayerScore(player, objective).set(ClassSynergy.classJobIndex(player));
            }
        });
    }

    private static Objective classObjective(Scoreboard scoreboard) {
        Objective objective = scoreboard.getObjective(CLASS_OBJECTIVE);
        if (objective != null) return objective;
        return scoreboard.addObjective(
            CLASS_OBJECTIVE,
            ObjectiveCriteria.DUMMY,
            Component.literal(CLASS_OBJECTIVE),
            ObjectiveCriteria.RenderType.INTEGER,
            false,
            null
        );
    }

    private static void sync(ServerPlayer player) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return;

        Set<String> wanted = new HashSet<>();
        for (Job job : jobsPlayer.jobsplus$getJobs()) {
            wanted.add(PREFIX + job.getJobInstance().getIdentifier().getPath());
        }
        for (String tag : Set.copyOf(player.getTags())) {
            if (tag.startsWith(PREFIX) && !wanted.contains(tag)) player.removeTag(tag);
        }
        for (String tag : wanted) {
            player.addTag(tag);
        }
    }
}
