package info.mudbourn.mmsjobs;

import info.mudbourn.mmsjobs.command.JobsDebugCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MmsJobs implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("mms_jobs");

    @Override
    public void onInitialize() {
        JobsToolsEmbargo.register();
        RespecBadge.register();

        if (FabricLoader.getInstance().isModLoaded("jobsplus")) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                JobsDebugCommand.register(dispatcher));
            PowerupAttributes.register();
            TamedAura.register();
            CookingSpeed.register();
            JobRespec.register();
            RogueStealth.register();
            RancherMerge.register();
            PetBond.register();
            JobTags.register();
            VeinBreaker.register();
            TimberYield.register();
            if (FabricLoader.getInstance().isModLoaded("uselessreptile")) DragonBond.register();
        }

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            JobsPlusActionCooldown.forget(handler.getPlayer().getUUID());
            RogueStealth.forget(handler.getPlayer().getUUID());
            PetBond.forget(handler.getPlayer().getUUID());
            VeinBreaker.forget(handler.getPlayer().getUUID());
            TimberYield.forget(handler.getPlayer().getUUID());
        });

        LOG.info("MMS Jobs loaded.");
    }
}
