package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.TimberYield;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Timber bonus planks for Lumberjack-class players.
@Mixin(ResultSlot.class)
public class TimberPlanksMixin {

    @Shadow @Final private Player player;
    @Shadow private int removeCount;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void mmsJobs$bonusPlanks(ItemStack stack, CallbackInfo ci) {
        TimberYield.craftedPlanks(player, stack, removeCount > 0 ? removeCount : stack.getCount());
    }
}
