package info.mudbourn.mmsjobs.mixin;

import com.daqem.arc.api.action.IAction;
import com.daqem.arc.api.action.holder.IActionHolder;
import info.mudbourn.mmsjobs.OwnedJobsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers only the Jobs+ jobs, powerups and actions this mod ships, so the
 * stock Jobs+ data never reaches players.
 */
@Mixin(targets = "com.daqem.arc.data.ActionHolderManager")
public class StockJobsFilterMixin {

    @ModifyVariable(method = "registerActionHolders", at = @At("HEAD"), argsOnly = true)
    private List<IActionHolder> mmsJobs$keepOwnedHolders(List<IActionHolder> holders) {
        List<IActionHolder> kept = new ArrayList<>(holders);
        kept.removeIf(holder -> !OwnedJobsData.keepsHolder(holder.getIdentifier()));
        return kept;
    }

    @ModifyVariable(method = "registerActions", at = @At("HEAD"), argsOnly = true)
    private List<IAction> mmsJobs$keepOwnedActions(List<IAction> actions) {
        List<IAction> kept = new ArrayList<>(actions);
        kept.removeIf(action -> !OwnedJobsData.keepsAction(action.getIdentifier()));
        return kept;
    }
}
