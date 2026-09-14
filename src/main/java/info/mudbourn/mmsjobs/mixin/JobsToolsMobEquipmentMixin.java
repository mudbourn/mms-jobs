package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.JobsToolsEmbargo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the embargoed JobsPlusTools weapon-duplicates out of mob hands.
 *
 * <p>Mirrors the creative-menu strip in {@link JobsToolsEmbargo}: spawn
 * equipment is not an item pipeline, so a mob equipped with an embargoed
 * longsword or compound bow would otherwise wield it (and drop it on death)
 * despite the creative and recipe-viewer hiding.
 *
 * <p>{@code setItemSlot} is declared on {@link LivingEntity}, not {@link Mob}
 * (as of 1.21.11 Mob no longer overrides it), so target LivingEntity and narrow
 * to mobs with an instanceof guard, leaving players untouched.
 */
@Mixin(LivingEntity.class)
public abstract class JobsToolsMobEquipmentMixin {

    @Inject(method = "setItemSlot", at = @At("HEAD"), cancellable = true)
    private void mmsJobs$refuseEmbargoedEquipment(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if ((Object) this instanceof Mob && !stack.isEmpty() && JobsToolsEmbargo.isEmbargoed(stack)) {
            ci.cancel();
        }
    }
}
