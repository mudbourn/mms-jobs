package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.ChestLootReroll;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

// Explorer Lucky Finds chest rerolls, which swap a common pick for a rarer one in the same pool.
@Mixin(LootPool.class)
public class ChestLootRerollMixin {

    @Shadow @Final private List<LootPoolEntryContainer> entries;

    @Inject(method = "addRandomItem", at = @At("HEAD"), cancellable = true)
    private void mmsJobs$reroll(Consumer<ItemStack> output, LootContext context, CallbackInfo ci) {
        float rate = ChestLootReroll.rate(context);
        if (rate <= 0.0F) return;
        ci.cancel();
        ChestLootReroll.pick(entries, output, context, rate);
    }
}
