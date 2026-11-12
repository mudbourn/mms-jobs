package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.EnchantExtraction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Cleric Enchant Extraction, read from the grindstone inputs before they are cleared.
@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public class GrindstoneExtractMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void mmsJobs$extract(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.containerMenu instanceof GrindstoneMenuAccessor menu) {
            EnchantExtraction.ground(player, menu.mmsJobs$getRepairSlots());
        }
    }
}
