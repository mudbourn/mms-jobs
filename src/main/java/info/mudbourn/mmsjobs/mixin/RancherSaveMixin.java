package info.mudbourn.mmsjobs.mixin;

import info.mudbourn.mmsjobs.RancherMerge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Hands a player's saved Rancher job to RancherMerge before Jobs+ drops it.
@Mixin(ServerPlayer.class)
public class RancherSaveMixin {

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void mmsJobs$readRancher(ValueInput input, CallbackInfo ci) {
        RancherMerge.read(((ServerPlayer) (Object) this).getUUID(), input);
    }
}
