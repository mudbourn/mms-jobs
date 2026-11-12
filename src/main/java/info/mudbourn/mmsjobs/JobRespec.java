package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.config.JobsPlusConfig;
import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import com.daqem.jobsplus.player.job.powerup.Powerup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class JobRespec {
    private static final Logger LOG = LoggerFactory.getLogger("mms_jobs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "mms_jobs_respec.json";
    private static final double KEEP_RATE = 0.65;
    private static final int PAID_JOB_SLOT = 3;
    private static final String CLASS_LAYER = "origins-classes:class";

    private static final Map<UUID, Credit> CREDITS = new HashMap<>();
    private static Path file;

    private JobRespec() {
    }

    private static final class Credit {
        double experience;
        int slots;
        final Set<String> credited = new HashSet<>();
    }

    private record Totals(double savedCoins, double spentCoins, double experience, int jobCount, double freeSlotCoins) {
        double coinPool() {
            return (savedCoins + spentCoins) * KEEP_RATE + freeSlotCoins;
        }
    }

    // Hooks loading, saving and the once-a-second credit check.
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(JobRespec::load);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CREDITS.clear();
            file = null;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0 && !CREDITS.isEmpty()) applyCredits(server);
        });
    }

    // Tells the player what a respec would give them and how to confirm.
    public static void preview(ServerPlayer player) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return;

        Totals totals = totals(jobsPlayer);
        player.sendSystemMessage(Component.literal("Orb of Occupation: use again within 10 s to confirm.")
            .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(String.format(
            "You give up %d job(s) and your class. You keep %.0f coins and %.0f job XP%s.",
            totals.jobCount(),
            totals.coinPool(),
            totals.experience() * KEEP_RATE,
            totals.freeSlotCoins() > 0 ? ", and your third job is free" : ""
        )).withStyle(ChatFormatting.YELLOW));
    }

    // Wipes the player's jobs and class, refunds 65% of coins and stores 65% of XP for their new jobs.
    public static void respec(ServerPlayer player) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return;

        Totals totals = totals(jobsPlayer);
        for (JobInstance instance : new ArrayList<>(jobsPlayer.jobsplus$getJobInstances())) {
            jobsPlayer.jobsplus$removeJob(instance);
        }
        clearInactive(jobsPlayer);
        jobsPlayer.jobsplus$setCoins(totals.coinPool());

        if (totals.jobCount() > 0 && totals.experience() > 0) {
            Credit credit = new Credit();
            credit.experience = totals.experience() * KEEP_RATE;
            credit.slots = totals.jobCount();
            CREDITS.put(player.getUUID(), credit);
            save();
        }

        resetClass(player);
        player.sendSystemMessage(Component.literal(String.format(
            "Your jobs are cleared. You have %.0f coins. Join up to %d new job(s) to split %.0f job XP between them.",
            totals.coinPool(),
            totals.jobCount(),
            totals.experience() * KEEP_RATE
        )).withStyle(ChatFormatting.GREEN));
    }

    private static Totals totals(JobsPlayer jobsPlayer) {
        double powerupMultiplier = JobsPlusConfig.powerupPriceMultiplier.get();
        double spent = 0;
        double experience = 0;
        List<Job> jobs = jobsPlayer.jobsplus$getJobs();
        for (Job job : jobs) {
            for (Powerup powerup : job.getPowerupManager().getAllPowerups()) {
                spent += powerup.getPowerupInstance().getPrice() * powerupMultiplier;
            }
            experience += totalExperience(job);
        }

        double freeSlot = 0;
        if (jobs.size() >= PAID_JOB_SLOT && jobs.size() > JobsPlusConfig.amountOfFreeJobs.get()) {
            freeSlot = jobs.get(PAID_JOB_SLOT - 1).getJobInstance().getPrice() * JobsPlusConfig.jobPriceMultiplier.get();
        }
        return new Totals(jobsPlayer.jobsplus$getCoins(), spent, experience, jobs.size(), freeSlot);
    }

    static double totalExperience(Job job) {
        double total = job.getExperience();
        for (int level = 0; level < job.getLevel(); level++) {
            total += Job.getExperienceToLevelUp(level);
        }
        return total;
    }

    // Sets a job's level and XP from a pooled XP amount without firing level-up rewards.
    static void grantExperience(Job job, double amount) {
        double remaining = totalExperience(job) + amount;
        int maxLevel = JobsPlusConfig.maxLevel.get();
        int level = 0;
        while (level < maxLevel && remaining >= Job.getExperienceToLevelUp(level)) {
            remaining -= Job.getExperienceToLevelUp(level);
            level++;
        }
        if (level >= maxLevel) remaining = 0;

        job.setLevel(level);
        job.setExperience(remaining, false, false);
        job.markStatsDirty();
        job.markPowerupsDirty();
        job.updateArcActionHolders();
        job.sendClientSyncPacket();
        job.sendClientLevelPacket();
    }

    private static void clearInactive(JobsPlayer jobsPlayer) {
        try {
            jobsPlayer.jobsplus$getInactiveJobs().clear();
        } catch (UnsupportedOperationException e) {
            LOG.warn("Could not clear inactive jobs for {}", jobsPlayer.jobsplus$getName());
        }
    }

    private static void resetClass(ServerPlayer player) {
        if (!FabricLoader.getInstance().isModLoaded("origins-legacy-classes")) return;

        MinecraftServer server = player.level().getServer();
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack().withSuppressedOutput(),
            "origin gui " + player.getScoreboardName() + " " + CLASS_LAYER
        );
    }

    private static void applyCredits(MinecraftServer server) {
        boolean changed = false;
        for (Map.Entry<UUID, Credit> entry : new ArrayList<>(CREDITS.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (!(player instanceof JobsPlayer jobsPlayer)) continue;

            Credit credit = entry.getValue();
            for (Job job : jobsPlayer.jobsplus$getJobs()) {
                String id = job.getJobInstance().getIdentifier().toString();
                if (credit.credited.size() >= credit.slots || !credit.credited.add(id)) continue;

                grantExperience(job, credit.experience / credit.slots);
                player.sendSystemMessage(Component.literal(String.format(
                    "%s received %.0f job XP from your Orb of Occupation.",
                    job.getJobInstance().getName().getString(),
                    credit.experience / credit.slots
                )).withStyle(ChatFormatting.AQUA));
                changed = true;
            }
            if (credit.credited.size() >= credit.slots) CREDITS.remove(entry.getKey());
        }
        if (changed) save();
    }

    private static void load(MinecraftServer server) {
        CREDITS.clear();
        file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE_NAME);
        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                JsonObject object = entry.getValue().getAsJsonObject();
                Credit credit = new Credit();
                credit.experience = object.get("experience").getAsDouble();
                credit.slots = object.get("slots").getAsInt();
                object.getAsJsonArray("credited").forEach(id -> credit.credited.add(id.getAsString()));
                CREDITS.put(UUID.fromString(entry.getKey()), credit);
            }
        } catch (IOException | RuntimeException e) {
            LOG.error("Could not read {}", file, e);
        }
    }

    private static void save() {
        if (file == null) return;

        JsonObject root = new JsonObject();
        CREDITS.forEach((uuid, credit) -> {
            JsonObject object = new JsonObject();
            object.addProperty("experience", credit.experience);
            object.addProperty("slots", credit.slots);
            JsonArray credited = new JsonArray();
            credit.credited.forEach(credited::add);
            object.add("credited", credited);
            root.add(uuid.toString(), object);
        });
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            LOG.error("Could not write {}", file, e);
        }
    }
}
