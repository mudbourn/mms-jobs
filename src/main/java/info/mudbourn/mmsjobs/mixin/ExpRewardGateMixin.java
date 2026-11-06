package info.mudbourn.mmsjobs.mixin;

import com.daqem.arc.api.action.result.ActionResult;
import com.daqem.arc.data.ActionData;
import com.daqem.arc.data.reward.experience.ExpReward;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExpReward.class)
public class ExpRewardGateMixin {

    // Skips vanilla XP unless the job XP of the same action was just paid
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void mmsJobs$gateVanillaXp(ActionData data, CallbackInfoReturnable<ActionResult> cir) {
        var holder = data.getSourceActionHolder();
        String jobId = holder == null ? null : holder.getIdentifier().toString();
        if (!JobsPlusActionCooldown.allowsVanillaXp(jobId)) {
            cir.setReturnValue(new ActionResult());
        }
    }
}
