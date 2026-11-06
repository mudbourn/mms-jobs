package info.mudbourn.mmsjobs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import info.mudbourn.mmsjobs.FarmersDelightCook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Pays Cook XP when a player serves a meal from a Cooking Pot with a container.
 */
@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.CookingPotBlock")
public class FarmersDelightPotServeMixin {

    @WrapOperation(
        method = "method_55765",
        at = @At(
            value = "INVOKE",
            target = "Lvectorwing/farmersdelight/common/block/entity/CookingPotBlockEntity;useHeldItemOnMeal(Lnet/minecraft/class_1799;)Lnet/minecraft/class_1799;"
        )
    )
    private ItemStack mmsJobs$mealServed(
        @Coerce Object pot,
        ItemStack held,
        Operation<ItemStack> original,
        @Local(argsOnly = true) Player player
    ) {
        ItemStack served = original.call(pot, held);
        if (!served.isEmpty()) {
            FarmersDelightCook.potMeal(player);
        }
        return served;
    }
}
