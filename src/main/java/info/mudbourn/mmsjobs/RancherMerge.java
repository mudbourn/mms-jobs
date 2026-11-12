package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.config.JobsPlusConfig;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobManager;
import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Folds the removed Rancher job and class into Farmer, moving saved Rancher XP and coins onto Farmer.
public final class RancherMerge {
    private static final String RANCHER = "jobsplus:rancher";
    private static final Identifier FARMER = Identifier.fromNamespaceAndPath("jobsplus", "farmer");
    private static final String CLASS_LAYER = "origins-classes:class";
    private static final double RANCHER_PRICE = 10;

    // Rancher progress read from a player's save, waiting to be paid onto Farmer
    private record Pending(double experience, double powerupCoins) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private RancherMerge() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                mergeJob(player);
                mergeClass(server, player);
            }
        });
    }

    // Reads the saved Rancher job before Jobs+ drops it as an unknown job.
    public static void read(UUID playerId, ValueInput input) {
        input.child("JobsPlus").ifPresent(jobsPlus -> {
            for (ValueInput job : jobsPlus.childrenListOrEmpty("jobs")) {
                if (!RANCHER.equals(job.getStringOr("job_instance", ""))) continue;

                double experience = job.getDoubleOr("experience", 0);
                int level = job.getIntOr("level", 0);
                for (int i = 0; i < level; i++) {
                    experience += Job.getExperienceToLevelUp(i);
                }

                double coins = 0;
                for (ValueInput powerup : job.childrenListOrEmpty("powerups")) {
                    String state = powerup.getStringOr("state", "");
                    if (!state.equals("ACTIVE") && !state.equals("INACTIVE")) continue;
                    coins += priceOf(powerup.getStringOr("powerup", ""));
                }
                PENDING.put(playerId, new Pending(experience, coins * JobsPlusConfig.powerupPriceMultiplier.get()));
            }
        });
    }

    // Price a Rancher powerup was bought at, from its rank suffix.
    private static double priceOf(String powerup) {
        if (powerup.endsWith("_iii")) return 16;
        if (powerup.endsWith("_ii")) return 10;
        if (powerup.endsWith("_i")) return 5;
        return 0;
    }

    private static void mergeJob(ServerPlayer player) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return;

        Pending pending = PENDING.get(player.getUUID());
        if (pending == null) return;

        JobInstance farmerInstance = JobManager.getInstance().getJobs().get(FARMER);
        if (farmerInstance == null) return;
        PENDING.remove(player.getUUID());

        double refund = pending.powerupCoins();
        Job farmer = jobsPlayer.jobsplus$getJob(farmerInstance);
        if (farmer != null) {
            refund += RANCHER_PRICE * JobsPlusConfig.jobPriceMultiplier.get();
        } else {
            farmer = jobsPlayer.jobsplus$addNewJob(farmerInstance);
        }
        if (farmer == null) return;

        JobRespec.grantExperience(farmer, pending.experience());
        jobsPlayer.jobsplus$addCoins(refund);
        player.sendSystemMessage(Component.literal(String.format(
            "Rancher has joined Farmer. Your %.0f Rancher XP was added to Farmer%s.",
            pending.experience(),
            refund > 0 ? String.format(" and %.0f coins were refunded", refund) : ""
        )).withStyle(ChatFormatting.GREEN));
    }

    private static void mergeClass(MinecraftServer server, ServerPlayer player) {
        if (!FabricLoader.getInstance().isModLoaded("origins-legacy-classes")) return;

        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        String name = player.getScoreboardName();
        try {
            int isRancher = server.getCommands().getDispatcher().execute(
                "origin has " + name + " " + CLASS_LAYER + " origins-classes:rancher",
                source
            );
            if (isRancher <= 0) return;

            server.getCommands().getDispatcher().execute(
                "origin set " + name + " " + CLASS_LAYER + " origins-classes:farmer",
                source
            );
            player.sendSystemMessage(Component.literal("The Rancher class has joined Farmer, so you are now a Farmer.")
                .withStyle(ChatFormatting.GREEN));
        } catch (CommandSyntaxException ignored) {
        }
    }
}
