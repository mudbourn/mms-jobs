package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.FarmersDelightCook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pays Cook XP when a player cuts the item on a Cutting Board.
 */
@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity")
public class FarmersDelightCuttingBoardMixin {

    @Inject(method = "processStoredItemUsingTool", at = @At("RETURN"))
    private void mmsJobs$cut(ItemStack tool, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            FarmersDelightCook.cut(player);
        }
    }
}
