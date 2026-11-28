package info.mudbourn.mmsjobs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsjobs.LuckyDrops;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Scales a loot pool's rolls by the Luck of the player breaking the block or killing the mob.
@Mixin(LootPool.class)
public class LuckyDropsMixin {

    @WrapOperation(
        method = "addRandomItems",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/providers/number/ints/ContextIntProvider;getInt(Lnet/minecraft/world/level/storage/loot/LootContext;)I"
        )
    )
    private int mmsJobs$luckyRolls(ContextIntProvider rolls, LootContext context, Operation<Integer> original) {
        return LuckyDrops.rolls(original.call(rolls, context), context);
    }
}
