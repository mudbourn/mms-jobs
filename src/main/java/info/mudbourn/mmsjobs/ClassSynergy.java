package info.mudbourn.mmsjobs;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

// Whether a player's Origins class matches a job, for class and job synergy bonuses.
public final class ClassSynergy {
    private static final boolean CLASSES_LOADED = FabricLoader.getInstance().isModLoaded("origins-legacy-classes");

    // Job ids in the order used for the mms_jobs_class scoreboard value, starting at 1.
    private static final List<String> JOB_ORDER = List.of(
        "alchemist",
        "beastmaster",
        "cook",
        "explorer",
        "farmer",
        "hunter",
        "lumberjack",
        "merchant",
        "miner",
        "rancher",
        "rogue",
        "smith",
        "warrior"
    );

    private ClassSynergy() {}

    // Whether the player's class is origins-classes:<origin>.
    public static boolean has(ServerPlayer player, String origin) {
        return origin.equals(classOf(player));
    }

    // The path of the player's class origin, or null when they have none or Origins Classes is absent.
    public static String classOf(ServerPlayer player) {
        return CLASSES_LOADED ? OriginsClassReader.classOf(player) : null;
    }

    // The scoreboard number of the job paired with the player's class, or 0 when there is none.
    public static int classJobIndex(ServerPlayer player) {
        String origin = classOf(player);
        if (origin == null) return 0;

        for (int i = 0; i < JOB_ORDER.size(); i++) {
            if (PowerupRanks.classFor(JOB_ORDER.get(i)).equals(origin)) return i + 1;
        }
        return 0;
    }
}
