package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.ChestLootReroll;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.UUID;

// Explorer Lucky Finds rerolls for a trial spawner reward, credited to the player it is ejected for.
@Mixin(TrialSpawner.class)
public class TrialRewardRerollMixin {

    @Inject(method = "ejectReward", at = @At("HEAD"))
    private void mmsJobs$rewardFor(ServerLevel level, BlockPos pos, ResourceKey<LootTable> table, CallbackInfo ci) {
        Iterator<UUID> owed = ((TrialSpawnerDataAccessor) ((TrialSpawner) (Object) this).getStateData()).mmsJobs$detectedPlayers().iterator();
        if (owed.hasNext() && level.getPlayerByUUID(owed.next()) instanceof ServerPlayer player) {
            ChestLootReroll.setRewardee(player);
        }
    }

    @Inject(method = "ejectReward", at = @At("RETURN"))
    private void mmsJobs$rewardDone(ServerLevel level, BlockPos pos, ResourceKey<LootTable> table, CallbackInfo ci) {
        ChestLootReroll.setRewardee(null);
    }
}
