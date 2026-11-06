package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.FarmersDelightCook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pays Cook XP when a player takes a meal from the Cooking Pot output slot.
 */
@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.container.CookingPotResultSlot")
public class FarmersDelightPotResultMixin {

    @Inject(method = "method_7667", at = @At("HEAD"))
    private void mmsJobs$mealTaken(Player player, ItemStack stack, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            FarmersDelightCook.potMeal(player);
        }
    }
}
