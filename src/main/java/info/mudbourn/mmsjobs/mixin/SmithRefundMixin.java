package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.SmithCraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Smith Thrifty Forging, read from the crafting grid before its ingredients are used up.
@Mixin(ResultSlot.class)
public class SmithRefundMixin {

    @Shadow @Final private Player player;
    @Shadow @Final private CraftingContainer craftSlots;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void mmsJobs$refundMaterial(ItemStack stack, CallbackInfo ci) {
        SmithCraft.craftedGear(player, stack, craftSlots.getItems());
    }
}
