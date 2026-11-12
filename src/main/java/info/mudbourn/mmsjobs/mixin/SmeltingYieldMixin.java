package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.SmithCraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Smith Smelting Yield.
@Mixin(FurnaceResultSlot.class)
public class SmeltingYieldMixin {

    @Shadow @Final private Player player;
    @Shadow private int removeCount;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void mmsJobs$extraSmelted(ItemStack stack, CallbackInfo ci) {
        SmithCraft.smelted(player, stack, removeCount > 0 ? removeCount : stack.getCount());
    }
}
