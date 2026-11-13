package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.BrewinChewinKeg;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// Pays Cook XP when a player pours a drink out of a Keg by hand.
@Pseudo
@Mixin(targets = "umpaz.brewinandchewin.common.block.KegBlock")
public class BrewinChewinKegPourMixin {

    @Inject(method = "applyExtractedItems", at = @At("HEAD"))
    private static void mmsJobs$poured(Player player, InteractionHand hand, List<ItemStack> items, CallbackInfo ci) {
        BrewinChewinKeg.extracted(player, items);
    }
}
