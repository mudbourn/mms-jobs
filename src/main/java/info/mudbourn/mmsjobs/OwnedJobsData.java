package info.mudbourn.mmsjobs;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The Jobs+ jobs, powerups and actions this mod ships. Anything else in the
 * {@code jobsplus} namespace comes from stock Jobs+ and is dropped at load.
 */
public final class OwnedJobsData {

    private static final Logger LOG = LoggerFactory.getLogger("mms_jobs");
    private static final String NAMESPACE = "jobsplus";

    private static Set<Identifier> holders;
    private static Set<Identifier> actions;

    private OwnedJobsData() {}

    /** @return true if {@code id} is outside the Jobs+ namespace or is a job or powerup this mod ships */
    public static boolean keepsHolder(Identifier id) {
        if (holders == null) {
            holders = new HashSet<>();
            scan("data/jobsplus/jobsplus/jobs", holders);
            scan("data/jobsplus/jobsplus/powerups", holders);
        }
        return id == null || !NAMESPACE.equals(id.getNamespace()) || holders.contains(id);
    }

    /** @return true if {@code id} is outside the Jobs+ namespace or is an action this mod ships */
    public static boolean keepsAction(Identifier id) {
        if (actions == null) {
            actions = new HashSet<>();
            scan("data/jobsplus/arc", actions);
        }
        return id == null || !NAMESPACE.equals(id.getNamespace()) || actions.contains(id);
    }

    private static void scan(String root, Set<Identifier> into) {
        ModContainer mod = FabricLoader.getInstance().getModContainer("mms_jobs").orElseThrow();
        Path base = mod.findPath(root).orElse(null);
        if (base == null) return;

        try (Stream<Path> files = Files.walk(base)) {
            files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                String relative = base.relativize(path).toString().replace('\\', '/');
                into.add(Identifier.fromNamespaceAndPath(NAMESPACE, relative.substring(0, relative.length() - ".json".length())));
            });
        } catch (IOException e) {
            LOG.error("Could not list owned Jobs+ data under {}", root, e);
        }
    }
}
