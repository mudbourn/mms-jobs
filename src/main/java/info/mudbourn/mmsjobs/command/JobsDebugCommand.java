package info.mudbourn.mmsjobs.command;

import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobManager;
import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Self-targeted debug wrappers around the Jobs+ player API.
 *
 * <p>Jobs+ already ships {@code /job set level|experience|coins|powerup}, but every
 * subcommand takes an explicit target player, which makes a test loop expensive to
 * type.  These commands always act on the caller and add the bulk operations that
 * are actually useful when iterating on job/class balance: set every job at once,
 * wipe back to a clean slate, and watch XP decisions land live.</p>
 *
 * <p>Op-gated (permission level 2).  Registered only when Jobs+ is present.</p>
 */
public final class JobsDebugCommand {

    private JobsDebugCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mmsjob")
            .requires(source -> source.permissions().hasPermission(
                new net.minecraft.server.permissions.Permission.HasCommandLevel(
                    net.minecraft.server.permissions.PermissionLevel.byId(2))))

            .then(Commands.literal("list")
                .executes(ctx -> list(ctx.getSource())))

            .then(Commands.literal("watch")
                .executes(ctx -> watch(ctx.getSource())))

            .then(Commands.literal("reset")
                .executes(ctx -> reset(ctx.getSource())))

            .then(Commands.literal("coins")
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                    .executes(ctx -> coins(ctx.getSource(),
                        DoubleArgumentType.getDouble(ctx, "amount")))))

            .then(Commands.literal("level")
                .then(Commands.argument("job", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        jobIds().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("level", IntegerArgumentType.integer(0))
                        .executes(ctx -> level(ctx.getSource(),
                            StringArgumentType.getString(ctx, "job"),
                            IntegerArgumentType.getInteger(ctx, "level"))))))

            .then(Commands.literal("xp")
                .then(Commands.argument("job", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        jobIds().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                        .executes(ctx -> xp(ctx.getSource(),
                            StringArgumentType.getString(ctx, "job"),
                            DoubleArgumentType.getDouble(ctx, "amount"))))))

            .then(Commands.literal("all")
                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                    .executes(ctx -> all(ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "level")))))
        );
    }

    // ── Subcommands ──────────────────────────────────────────────────────

    private static int list(CommandSourceStack source) {
        JobsPlayer jobsPlayer = jobsPlayer(source);
        if (jobsPlayer == null) return 0;

        List<Job> jobs = jobsPlayer.jobsplus$getJobs();
        source.sendSuccess(() -> Component.literal("Coins: ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(String.format("%.1f", jobsPlayer.jobsplus$getCoins()))
                .withStyle(ChatFormatting.GOLD)), false);

        if (jobs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No jobs.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        for (Job job : jobs) {
            source.sendSuccess(() -> Component.literal("  " + shortId(job.getJobInstance()))
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(
                    " lvl " + job.getLevel()
                        + "  xp " + String.format("%.1f", job.getExperience())
                        + "/" + String.format("%.1f", job.getExperienceForNextLevel()))
                    .withStyle(ChatFormatting.GRAY)), false);
        }
        return jobs.size();
    }

    private static int watch(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) return 0;

        boolean on = JobsPlusActionCooldown.toggleWatch(player.getUUID());
        source.sendSuccess(() -> Component.literal(on
                ? "XP watch ON — every job XP grant will report its action type, cooldown category and verdict."
                : "XP watch OFF.")
            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        JobsPlayer jobsPlayer = jobsPlayer(source);
        if (jobsPlayer == null) return 0;

        // Copy first — removeJob mutates the backing list.
        List<JobInstance> held = new ArrayList<>(jobsPlayer.jobsplus$getJobInstances());
        held.forEach(jobsPlayer::jobsplus$removeJob);
        jobsPlayer.jobsplus$setCoins(0);

        int removed = held.size();
        source.sendSuccess(() -> Component.literal(
            "Reset: removed " + removed + " job(s), coins set to 0.")
            .withStyle(ChatFormatting.YELLOW), false);
        return removed;
    }

    private static int coins(CommandSourceStack source, double amount) {
        JobsPlayer jobsPlayer = jobsPlayer(source);
        if (jobsPlayer == null) return 0;

        jobsPlayer.jobsplus$setCoins(amount);
        source.sendSuccess(() -> Component.literal("Coins set to " + String.format("%.1f", amount))
            .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int level(CommandSourceStack source, String jobId, int level) {
        JobsPlayer jobsPlayer = jobsPlayer(source);
        if (jobsPlayer == null) return 0;

        JobInstance instance = resolve(source, jobId);
        if (instance == null) return 0;

        applyLevel(jobsPlayer, instance, level);
        source.sendSuccess(() -> Component.literal(shortId(instance) + " → level " + level)
            .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int xp(CommandSourceStack source, String jobId, double amount) {
        JobsPlayer jobsPlayer = jobsPlayer(source);
        if (jobsPlayer == null) return 0;

        JobInstance instance = resolve(source, jobId);
        if (instance == null) return 0;

        Job job = jobsPlayer.jobsplus$getJob(instance);
        if (job == null) job = jobsPlayer.jobsplus$addNewJob(instance);
        if (job == null) {
            source.sendFailure(Component.literal("Could not join job " + shortId(instance)));
            return 0;
        }

        // addExperienceWithoutEvent bypasses the ARC action pipeline, so the XP
        // cooldown mixin never sees it — that is what we want for a test grant.
        job.addExperienceWithoutEvent(amount);
        job.sendClientSyncPacket();

        final Job granted = job;
        source.sendSuccess(() -> Component.literal(
            shortId(instance) + " +" + String.format("%.1f", amount) + " xp"
                + " (now lvl " + granted.getLevel()
                + ", " + String.format("%.1f", granted.getExperience()) + ")")
            .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int all(CommandSourceStack source, int level) {
        JobsPlayer jobsPlayer = jobsPlayer(source);
        if (jobsPlayer == null) return 0;

        Map<Identifier, JobInstance> jobs = JobManager.getInstance().getJobs();
        jobs.values().forEach(instance -> applyLevel(jobsPlayer, instance, level));

        int count = jobs.size();
        source.sendSuccess(() -> Component.literal(
            "Set all " + count + " job(s) to level " + level + ".")
            .withStyle(ChatFormatting.YELLOW), false);
        return count;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Level 0 means "not employed" — drop the job rather than holding it at zero. */
    private static void applyLevel(JobsPlayer jobsPlayer, JobInstance instance, int level) {
        Job job = jobsPlayer.jobsplus$getJob(instance);

        if (level == 0) {
            if (job != null) jobsPlayer.jobsplus$removeJob(instance);
            return;
        }

        if (job == null) job = jobsPlayer.jobsplus$addNewJob(instance);
        if (job == null) return;

        job.setLevel(level);
        job.setExperience(0);
        job.markStatsDirty();
        job.markPowerupsDirty();
        job.updateArcActionHolders();
        job.sendClientSyncPacket();
        job.sendClientLevelPacket();
    }

    private static JobInstance resolve(CommandSourceStack source, String jobId) {
        Identifier id = jobId.contains(":")
            ? Identifier.tryParse(jobId)
            : Identifier.tryBuild("jobsplus", jobId);

        JobInstance instance = id == null ? null : JobManager.getInstance().getJobs().get(id);
        if (instance == null) {
            source.sendFailure(Component.literal(
                "Unknown job '" + jobId + "'. Known: " + String.join(", ", jobIds())));
        }
        return instance;
    }

    private static List<String> jobIds() {
        return JobManager.getInstance().getJobs().keySet().stream()
            .map(Identifier::getPath)
            .sorted()
            .toList();
    }

    private static String shortId(JobInstance instance) {
        return instance.getName().getString();
    }

    private static ServerPlayer player(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) return player;
        source.sendFailure(Component.literal("Players only — /mmsjob always targets the caller."));
        return null;
    }

    private static JobsPlayer jobsPlayer(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) return null;
        if (player instanceof JobsPlayer jobsPlayer) return jobsPlayer;
        source.sendFailure(Component.literal("Jobs+ player data unavailable."));
        return null;
    }
}
