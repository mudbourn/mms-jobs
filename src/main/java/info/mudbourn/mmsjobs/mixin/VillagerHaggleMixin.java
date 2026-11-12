package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.ExplorerPerks;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Explorer Silver Tongue villager discount.
@Mixin(Villager.class)
public class VillagerHaggleMixin {

    @Inject(method = "updateSpecialPrices", at = @At("RETURN"))
    private void mmsJobs$haggle(Player player, CallbackInfo ci) {
        ExplorerPerks.haggle(player, ((Villager) (Object) this).getOffers());
    }
}
