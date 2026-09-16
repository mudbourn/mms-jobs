package info.mudbourn.mmsjobs.mixin;

import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Drops the stock Jobs+ jobs that have no matching Origins Classes class, so the
 * playable roster lines up with {@code origins-classes:class}.
 *
 * <p>The removed jobs are hidden from the job menu and stop granting XP. Every
 * other job, including custom jobs from other namespaces, passes through.</p>
 */
@Mixin(targets = "com.daqem.jobsplus.integration.arc.holder.holders.job.JobManager")
public class JobManagerFilterMixin {

    @org.spongepowered.asm.mixin.Unique
    private static final Set<String> mmsCompat$REMOVED_JOBS = Set.of(
        "jobsplus:fisherman",
        "jobsplus:enchanter",
        "jobsplus:digger",
        "jobsplus:builder"
    );

    @ModifyReturnValue(method = "getJobs", at = @At("RETURN"))
    private Map<Identifier, JobInstance> mmsCompat$filterStockJobs(Map<Identifier, JobInstance> jobs) {
        Map<Identifier, JobInstance> filtered = new LinkedHashMap<>(jobs);
        filtered.keySet().removeIf(id -> mmsCompat$REMOVED_JOBS.contains(id.toString()));
        return filtered;
    }
}
