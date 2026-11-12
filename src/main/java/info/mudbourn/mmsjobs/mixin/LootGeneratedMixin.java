package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.ExplorerPerks;
import net.minecraft.advancements.criterion.LootTableTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Explorer XP when a player opens fresh loot.
@Mixin(LootTableTrigger.class)
public class LootGeneratedMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void mmsJobs$lootGenerated(ServerPlayer player, ResourceKey<LootTable> lootTable, CallbackInfo ci) {
        ExplorerPerks.lootGenerated(player);
    }
}
