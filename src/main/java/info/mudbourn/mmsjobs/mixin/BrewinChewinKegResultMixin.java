package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.BrewinChewinKeg;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Pays Cook XP when a player takes a result from the Keg output slot.
@Mixin(Slot.class)
public class BrewinChewinKegResultMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void mmsJobs$resultTaken(Player player, ItemStack stack, CallbackInfo ci) {
        BrewinChewinKeg.resultTaken((Slot) (Object) this, player, stack);
    }
}
