package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.config.JobsPlusConfig;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobManager;
import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import com.daqem.jobsplus.player.job.powerup.Powerup;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// Folds the retired Rancher job and class into Farmer, adding Rancher XP onto Farmer.
public final class RancherMerge {
    private static final Identifier RANCHER = Identifier.fromNamespaceAndPath("jobsplus", "rancher");
    private static final Identifier FARMER = Identifier.fromNamespaceAndPath("jobsplus", "farmer");
    private static final String CLASS_LAYER = "origins-classes:class";

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

    private static void mergeJob(ServerPlayer player) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return;

        Job rancher = jobsPlayer.jobsplus$getJob(RANCHER);
        if (rancher == null) return;

        JobInstance farmerInstance = JobManager.getInstance().getJobs().get(FARMER);
        if (farmerInstance == null) return;

        double experience = JobRespec.totalExperience(rancher);
        double refund = 0;
        for (Powerup powerup : rancher.getPowerupManager().getAllPowerups()) {
            refund += powerup.getPowerupInstance().getPrice() * JobsPlusConfig.powerupPriceMultiplier.get();
        }

        Job farmer = jobsPlayer.jobsplus$getJob(farmerInstance);
        boolean hadFarmer = farmer != null;
        if (hadFarmer) {
            refund += rancher.getJobInstance().getPrice() * JobsPlusConfig.jobPriceMultiplier.get();
        }

        jobsPlayer.jobsplus$removeJob(rancher.getJobInstance());
        jobsPlayer.jobsplus$getInactiveJobs().removeIf(job -> job.getJobInstance() == null
            || RANCHER.equals(job.getJobInstance().getIdentifier()));
        if (farmer == null) farmer = jobsPlayer.jobsplus$addNewJob(farmerInstance);
        if (farmer == null) return;

        JobRespec.grantExperience(farmer, experience);
        jobsPlayer.jobsplus$addCoins(refund);
        player.sendSystemMessage(Component.literal(String.format(
            "Rancher has joined Farmer. Your %.0f Rancher XP was added to Farmer%s.",
            experience,
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
