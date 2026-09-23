package info.mudbourn.mmsjobs.mixin;

import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drops the stock Jobs+ jobs that have no matching Origins Classes class, so the
 * playable roster lines up with {@code origins-classes:class}.
 *
 * <p>The removed jobs are hidden from the job menu and stop granting XP. Every
 * other job, including custom jobs from other namespaces, passes through.</p>
 */
@Mixin(targets = "com.daqem.jobsplus.integration.arc.holder.holders.job.JobManager")
public class JobManagerFilterMixin {

    @Unique
    private static final List<Identifier> mmsCompat$REMOVED_JOBS = List.of(
        Identifier.fromNamespaceAndPath("jobsplus", "fisherman"),
        Identifier.fromNamespaceAndPath("jobsplus", "enchanter"),
        Identifier.fromNamespaceAndPath("jobsplus", "digger"),
        Identifier.fromNamespaceAndPath("jobsplus", "builder")
    );

    @ModifyReturnValue(method = "getJobs", at = @At("RETURN"))
    private Map<Identifier, JobInstance> mmsCompat$filterStockJobs(Map<Identifier, JobInstance> jobs) {
        try {
            for (Identifier id : mmsCompat$REMOVED_JOBS) {
                jobs.remove(id);
            }
            return jobs;
        } catch (UnsupportedOperationException immutable) {
            Map<Identifier, JobInstance> filtered = new LinkedHashMap<>(jobs);
            mmsCompat$REMOVED_JOBS.forEach(filtered::remove);
            return filtered;
        }
    }
}
