package info.mudbourn.mmsjobs.mixin;

import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.UUID;

// Reads the players a trial spawner still owes a reward.
@Mixin(TrialSpawnerStateData.class)
public interface TrialSpawnerDataAccessor {

    @Accessor("detectedPlayers")
    Set<UUID> mmsJobs$detectedPlayers();
}
