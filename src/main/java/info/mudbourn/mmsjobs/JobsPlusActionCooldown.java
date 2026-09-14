package info.mudbourn.mmsjobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player-per-job hard-cooldown system for Jobs+ XP grants.
 *
 * Unlike the old debounce (which shaved XP multipliers), this blocks XP
 * entirely while a cooldown is active.  Two tiers:
 * <ul>
 *   <li><b>EASY</b> (80 ticks / 4 s) — semi-passive actions (swim, crouch, place, plant, till, strip, interact entity)</li>
 *   <li><b>HARD</b> (40 ticks / 2 s) — active actions (kill, break, craft, smelt, enchant, fish, harvest, breed, tame, brew, drink, anvil, grind, throw)</li>
 * </ul>
 *
 * The first XP grant for an action category pays full; all subsequent grants
 * within the cooldown window are zeroed.  After the cooldown expires the next
 * grant pays full again.
 *
 * <p>Action categories are detected by a companion mixin on
 * {@code ActionData.sendToAction()} which sets a ThreadLocal before the
 * original method runs.  The XP mixin on {@code Job.addExperience} reads
 * that ThreadLocal and either blocks or allows the grant.</p>
 *
 * Config: {@code config/mms_compat/jobsplus_xp_cooldown.json}, written with
 * defaults on first read.  Reloaded only on server start (no hot reload).
 */
public final class JobsPlusActionCooldown {

    private static final Logger LOG = LoggerFactory.getLogger("mms_compat");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Cooldown category detected by the ActionData mixin. */
    public enum CooldownCategory { EASY, HARD, NONE }

    // ── ThreadLocal for cross-mixin communication ────────────────────────
    private static final ThreadLocal<CooldownCategory> CURRENT_CATEGORY =
        ThreadLocal.withInitial(() -> CooldownCategory.NONE);

    public static void setCooldownType(String actionTypeId) {
        CURRENT_CATEGORY.set(categoryFor(actionTypeId));
        CURRENT_ACTION_ID.set(actionTypeId);
    }

    public static CooldownCategory getCooldownType() {
        return CURRENT_CATEGORY.get();
    }

    public static void clearCooldownType() {
        CURRENT_CATEGORY.remove();
        CURRENT_ACTION_ID.remove();
    }

    /** Raw action type id for the grant in flight — watch readout only. */
    private static final ThreadLocal<String> CURRENT_ACTION_ID = new ThreadLocal<>();

    public static String getCurrentActionId() {
        String id = CURRENT_ACTION_ID.get();
        return id == null ? "<none>" : id;
    }

    // ── XP watch (/mmsjob watch) ─────────────────────────────────────────
    private static final Set<UUID> WATCHERS = ConcurrentHashMap.newKeySet();

    /** @return {@code true} if watching is now on for this player. */
    public static boolean toggleWatch(UUID playerId) {
        if (WATCHERS.remove(playerId)) return false;
        WATCHERS.add(playerId);
        return true;
    }

    public static boolean isWatching(UUID playerId) {
        return !WATCHERS.isEmpty() && WATCHERS.contains(playerId);
    }

    // ── Config ───────────────────────────────────────────────────────────
    private static boolean enabled = true;
    private static int easyCooldownTicks = 80;
    private static int hardCooldownTicks = 40;

    private static final Set<String> EASY_ACTION_TYPES = new HashSet<>(Arrays.asList(
        "arc:on_swim",
        "arc:on_place_block",
        "arc:on_plant_crop",
        "arc:on_till_soil",
        "arc:on_strip_log",
        "arc:on_crouch",
        "arc:on_interact_entity"
    ));

    private static final Set<String> HARD_ACTION_TYPES = new HashSet<>(Arrays.asList(
        "arc:on_kill_entity",
        "arc:on_hurt_entity",
        "arc:on_break_block",
        "arc:on_craft_item",
        "arc:on_smelt_item",
        "arc:on_enchant_item",
        "arc:on_fished_up_item",
        "arc:on_harvest_crop",
        "arc:on_breed_animal",
        "arc:on_tame_animal",
        "arc:on_brew_potion",
        "arc:on_drink",
        "arc:on_use_anvil",
        "arc:on_grind_item",
        "arc:on_throw_item"
    ));

    private static boolean loaded = false;

    // ── Live cooldown state ──────────────────────────────────────────────
    // Key: "playerUUID|jobId|category", Value: game-time tick when cooldown expires
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();

    private JobsPlusActionCooldown() {}

    /**
     * Maps an action type identifier string to a cooldown category.
     */
    private static CooldownCategory categoryFor(String actionTypeId) {
        if (actionTypeId == null) return CooldownCategory.NONE;
        if (EASY_ACTION_TYPES.contains(actionTypeId)) return CooldownCategory.EASY;
        if (HARD_ACTION_TYPES.contains(actionTypeId)) return CooldownCategory.HARD;
        return CooldownCategory.NONE;
    }

    /**
     * Returns the cooldown duration in ticks for the given category.
     */
    private static int cooldownTicksFor(CooldownCategory category) {
        return switch (category) {
            case EASY -> easyCooldownTicks;
            case HARD -> hardCooldownTicks;
            default -> hardCooldownTicks; // NONE defaults to HARD
        };
    }

    /**
     * Checks whether the given player+job+category is currently on cooldown.
     *
     * @param playerId  player UUID
     * @param jobId     job identifier string (e.g. {@code jobsplus:hunter})
     * @param category  cooldown category detected from action type
     * @param gameTime  current world game time in ticks
     * @return {@code true} if still on cooldown (XP should be blocked)
     */
    public static boolean isOnCooldown(UUID playerId, String jobId, CooldownCategory category, long gameTime) {
        if (!loaded) load();
        if (!enabled) return false;
        if (category == CooldownCategory.NONE) category = CooldownCategory.HARD;

        String key = playerId + "|" + jobId + "|" + category.name();
        Long expiry = COOLDOWNS.get(key);
        if (expiry == null) return false;
        // Handle tick wrap-around
        return gameTime < expiry && gameTime >= 0;
    }

    /**
     * Sets a cooldown for the given player+job+category starting now.
     */
    public static void setCooldown(UUID playerId, String jobId, CooldownCategory category, long gameTime) {
        if (!loaded) load();
        if (!enabled) return;
        if (category == CooldownCategory.NONE) category = CooldownCategory.HARD;

        String key = playerId + "|" + jobId + "|" + category.name();
        COOLDOWNS.put(key, gameTime + cooldownTicksFor(category));
    }

    /** Drops all cooldown state for a player — call on disconnect. */
    public static void forget(UUID playerId) {
        COOLDOWNS.keySet().removeIf(key -> key.startsWith(playerId + "|"));
        WATCHERS.remove(playerId);
    }

    // ── Config I/O ───────────────────────────────────────────────────────

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;

        Path path = FabricLoader.getInstance().getConfigDir()
            .resolve("mms_compat").resolve("jobsplus_xp_cooldown.json");

        try {
            if (!Files.exists(path)) {
                writeDefaults(path);
                return;
            }
            try (Reader reader = Files.newBufferedReader(path)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) return;

                if (root.has("enabled")) enabled = root.get("enabled").getAsBoolean();
                if (root.has("easyCooldownTicks")) easyCooldownTicks = root.get("easyCooldownTicks").getAsInt();
                if (root.has("hardCooldownTicks")) hardCooldownTicks = root.get("hardCooldownTicks").getAsInt();

                if (root.has("easyActionTypes")) {
                    EASY_ACTION_TYPES.clear();
                    for (var el : root.getAsJsonArray("easyActionTypes")) {
                        EASY_ACTION_TYPES.add(el.getAsString());
                    }
                }
                if (root.has("hardActionTypes")) {
                    HARD_ACTION_TYPES.clear();
                    for (var el : root.getAsJsonArray("hardActionTypes")) {
                        HARD_ACTION_TYPES.add(el.getAsString());
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("[mms_compat] Failed to read Jobs+ XP cooldown config, using defaults", e);
        }
    }

    private static void writeDefaults(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        root.addProperty("easyCooldownTicks", easyCooldownTicks);
        root.addProperty("hardCooldownTicks", hardCooldownTicks);

        JsonArray easy = new JsonArray();
        EASY_ACTION_TYPES.stream().sorted().forEach(easy::add);
        root.add("easyActionTypes", easy);

        JsonArray hard = new JsonArray();
        HARD_ACTION_TYPES.stream().sorted().forEach(hard::add);
        root.add("hardActionTypes", hard);

        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(root, writer);
        }
        LOG.info("[mms_compat] Wrote default Jobs+ XP cooldown config to {}", path);
    }
}
