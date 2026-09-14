package info.mudbourn.mmsjobs.mixin;

import com.daqem.jobsplus.JobsPlus;
import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes the Jobs+ level-up broadcast sending the job name as a literal string
 * (resolved server-side where no lang file exists) instead of a translatable
 * component (resolved client-side from the lang file).
 *
 * Root cause: {@code jobInstance.getName().getString()} collapses the
 * translatable component into a raw key string before placing it in the
 * args array for the outer {@code jobsplus.job.level_up} translation.
 *
 * Fix: redirect the {@code JobsPlus.translatable} call and replace args[2]
 * with the unresolved {@code MutableComponent} from {@code getName()},
 * so the client recursively resolves the nested translation key.
 */
@Mixin(targets = "com.daqem.jobsplus.event.triggers.JobEvents")
public class JobLevelUpMessageMixin {

    @Redirect(
        method = "onJobLevelUp",
        at = @At(
            value = "INVOKE",
            target = "Lcom/daqem/jobsplus/JobsPlus;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
        ),
        // Cosmetic-only fix; tolerate a mismatched Jobs+ build rather than crash the server.
        require = 0
    )
    private static MutableComponent mmsCompat$useTranslatableJobName(String key, Object[] args, JobsPlayer player, Job job) {
        // args[2] is the job name — was .getName().getString() (literal).
        // Replace with the translatable component so the client resolves it.
        args[2] = job.getJobInstance().getName();
        return JobsPlus.translatable(key, args);
    }
}
