package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.ExplorerPerks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// Explorer Silver Tongue bonus barters.
@Mixin(PiglinAi.class)
public class PiglinBarterMixin {

    @Inject(method = "pickUpItem", at = @At("HEAD"))
    private static void mmsJobs$recordThrower(ServerLevel level, Piglin piglin, ItemEntity item, CallbackInfo ci) {
        ExplorerPerks.goldPickedUp(piglin, item.getOwner());
    }

    @Inject(method = "getBarterResponseItems", at = @At("RETURN"), cancellable = true)
    private static void mmsJobs$bonusBarter(Piglin piglin, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> items = new ArrayList<>(cir.getReturnValue());
        ExplorerPerks.barter(piglin, items);
        cir.setReturnValue(items);
    }
}
