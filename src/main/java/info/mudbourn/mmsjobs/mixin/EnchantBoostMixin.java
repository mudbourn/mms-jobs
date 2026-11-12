package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.ToolEnchantBoost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Lumberjack Honed Edge and Miner Tempered Tip.
@Mixin(EnchantmentMenu.class)
public class EnchantBoostMixin {

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void mmsJobs$boostEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        ToolEnchantBoost.enchanted(player, ((EnchantmentMenu) (Object) this).getSlot(0).getItem());
    }
}
