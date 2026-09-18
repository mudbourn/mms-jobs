package info.mudbourn.mmsjobs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MmsJobs implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("mms_jobs");

    @Override
    public void onInitialize() {
        // Strip embargoed JobsPlusTools weapon-dups from creative tabs and search
        JobsToolsEmbargo.register();

        // /mmsjob debug wrappers (op only) — self-targeted Jobs+ test harness
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jobsplus")) {
                info.mudbourn.mmsjobs.command.JobsDebugCommand.register(dispatcher);
            }
        });

        // Explorer "Sprint Efficiency" as a movement-speed attribute, not a Speed effect.
        // Gated on Jobs+ so SprintEfficiency (which imports Jobs+ types) only loads with it.
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jobsplus")) {
            SprintEfficiency.register();
        }

        // Drop Jobs+ XP cooldown state when a player leaves
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            JobsPlusActionCooldown.forget(handler.getPlayer().getUUID()));

        LOG.info("MMS Jobs loaded — Jobs+ level-up message fix, XP cooldown, warrior job.");
    }
}
