package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.JobXpGrants;
import info.mudbourn.mmsjobs.JobXpSource;
import net.minecraft.advancements.criterion.CuredZombieVillagerTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Merchant XP for curing a zombie villager.
@Mixin(CuredZombieVillagerTrigger.class)
public class CureVillagerMixin {

    @Inject(method = "trigger", at = @At("HEAD"))
    private void mmsJobs$cured(ServerPlayer player, Zombie zombie, Villager villager, CallbackInfo ci) {
        JobXpGrants.grant(player, "merchant", JobXpGrants.HARD, JobXpSource.CURE_VILLAGER);
    }
}
