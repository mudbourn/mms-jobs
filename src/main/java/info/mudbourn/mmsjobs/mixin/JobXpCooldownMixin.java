package info.mudbourn.mmsjobs.mixin;

import com.daqem.jobsplus.integration.arc.holder.holders.job.JobInstance;
import com.daqem.jobsplus.player.JobsPlayer;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown;
import info.mudbourn.mmsjobs.JobsPlusActionCooldown.CooldownCategory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Blocks XP grants that arrive during the hard-cooldown window.
 *
 * The companion {@link ActionDataCooldownMixin} sets the ThreadLocal category
 * before {@code sendToAction()} runs; this mixin reads it at the point
 * {@code addExperience} is called.  If the category is on cooldown the XP is
 * zeroed; otherwise the cooldown is armed and XP passes through at 100%.
 *
 * <p>{@code addExperienceWithoutEvent} (admin/command path) is left alone.</p>
 */
@Mixin(targets = "com.daqem.jobsplus.player.job.Job")
public class JobXpCooldownMixin {

    @Shadow private JobsPlayer player;
    @Shadow @Final private JobInstance jobInstance;

    @ModifyVariable(method = "addExperience(D)V", at = @At("HEAD"), argsOnly = true)
    private double mmsCompat$cooldownBlockXp(double experience) {
        if (experience <= 0 || player == null || jobInstance == null) return experience;

        Player serverPlayer = player.jobsplus$getPlayer();
        if (serverPlayer == null || serverPlayer.level().isClientSide()) return experience;

        CooldownCategory category = JobsPlusActionCooldown.getCooldownType();
        String actionId = JobsPlusActionCooldown.isWatching(serverPlayer.getUUID())
            ? JobsPlusActionCooldown.getCurrentActionId()
            : null;
        String jobId = jobInstance.getIdentifier().toString();
        boolean weaponBlocked = JobsPlusActionCooldown.isWeaponBlocked(jobId);
        JobsPlusActionCooldown.clearCooldownType();

        if (weaponBlocked) {
            report(serverPlayer, jobId, actionId, category, false, experience, 0.0);
            return 0.0;
        }

        boolean unrecognised = category == CooldownCategory.NONE;
        if (unrecognised) {
            category = CooldownCategory.HARD;
        }

        long gameTime = serverPlayer.level().getGameTime();

        if (JobsPlusActionCooldown.isOnCooldown(serverPlayer.getUUID(), jobId, category, gameTime)) {
            report(serverPlayer, jobId, actionId, category, unrecognised, experience, 0.0);
            return 0.0;
        }

        JobsPlusActionCooldown.setCooldown(serverPlayer.getUUID(), jobId, category, gameTime);
        report(serverPlayer, jobId, actionId, category, unrecognised, experience, experience);
        return experience;
    }

    // Live readout for /mmsjob watch
    @org.spongepowered.asm.mixin.Unique
    private void report(
        Player player,
        String jobId,
        String actionId,
        CooldownCategory category,
        boolean unrecognised,
        double in,
        double out
    ) {
        if (actionId == null) return;

        boolean blocked = out <= 0;
        String job = jobId.contains(":") ? jobId.substring(jobId.indexOf(':') + 1) : jobId;

        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
            (blocked ? "§c✗ " : "§a✓ ") + job
                + " §7" + actionId
                + " §8[" + category + (unrecognised ? "*" : "") + "] "
                + (blocked
                    ? "§cblocked §8(" + String.format("%.1f", in) + " xp)"
                    : "§a+" + String.format("%.1f", out) + " xp")), false);
    }
}
