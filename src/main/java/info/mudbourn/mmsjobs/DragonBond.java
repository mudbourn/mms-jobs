package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.player.JobsPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

// Useless Reptile dragons: Beastmaster-only taming, easier taming and calm curiosity towards Beastmasters.
public final class DragonBond {
    private static final Logger LOG = LoggerFactory.getLogger("mms_jobs");
    private static final String DRAGON_PACKAGE = "nordmods.uselessreptile.";
    private static final Identifier BEASTMASTER = Identifier.fromNamespaceAndPath("jobsplus", "beastmaster");
    private static final float TAMING_PROGRESS_KEPT = 0.6f;
    private static final double CURIOUS_RANGE = 16.0;
    private static final double CURIOUS_STOP = 5.0;

    private static Method getTamingItem;
    private static Method getTamingProgress;
    private static Method setTamingProgress;
    private static boolean reflectionFailed;

    private DragonBond() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isBeastmaster(player)) drawCurious(player);
            }
        });
    }

    public static boolean isDragon(Object entity) {
        return entity.getClass().getName().startsWith(DRAGON_PACKAGE);
    }

    public static boolean isBeastmaster(Player player) {
        return player instanceof JobsPlayer jobsPlayer && jobsPlayer.jobsplus$getJob(BEASTMASTER) != null;
    }

    // Stops a non-Beastmaster from taming a dragon, and speeds up taming for a Beastmaster.
    public static boolean blockTaming(TamableAnimal dragon, Player player, ItemStack stack) {
        if (dragon.isTame() || dragon.level().isClientSide() || !isTamingItem(dragon, stack)) return false;

        if (!isBeastmaster(player)) {
            player.displayClientMessage(
                Component.literal("Only a Beastmaster can tame a dragon.").withStyle(ChatFormatting.RED),
                true
            );
            return true;
        }

        int progress = tamingProgress(dragon);
        if (progress > 1) setTamingProgress(dragon, Math.max(1, Math.round(progress * TAMING_PROGRESS_KEPT)));
        return false;
    }

    // Whether an untamed dragon should refuse to target a Beastmaster who has not hurt it.
    public static boolean ignoresTarget(Mob mob, LivingEntity target) {
        if (!(target instanceof ServerPlayer player) || !(mob instanceof TamableAnimal dragon)) return false;
        if (dragon.isTame() || !isDragon(mob)) return false;
        return mob.getLastHurtByMob() != player && isBeastmaster(player);
    }

    private static void drawCurious(ServerPlayer player) {
        ServerLevel level = player.level();
        for (TamableAnimal dragon : level.getEntitiesOfClass(
            TamableAnimal.class,
            player.getBoundingBox().inflate(CURIOUS_RANGE),
            animal -> !animal.isTame() && isDragon(animal) && animal.getTarget() == null
        )) {
            dragon.getLookControl().setLookAt(player, 30.0f, 30.0f);
            if (dragon.distanceTo(player) > CURIOUS_STOP && dragon.getRandom().nextFloat() < 0.3f) {
                dragon.getNavigation().moveTo(player, 0.8);
            }
        }
    }

    private static boolean isTamingItem(TamableAnimal dragon, ItemStack stack) {
        if (stack.isEmpty() || !resolve(dragon)) return false;
        try {
            return getTamingItem.invoke(dragon, stack) != null;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static int tamingProgress(TamableAnimal dragon) {
        try {
            return (int) getTamingProgress.invoke(dragon);
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    private static void setTamingProgress(TamableAnimal dragon, int progress) {
        try {
            setTamingProgress.invoke(dragon, progress);
        } catch (ReflectiveOperationException e) {
            LOG.warn("Could not set dragon taming progress", e);
        }
    }

    private static boolean resolve(TamableAnimal dragon) {
        if (getTamingItem != null) return true;
        if (reflectionFailed) return false;
        try {
            Class<?> base = Class.forName(DRAGON_PACKAGE + "common.entity.base.URDragonEntity");
            getTamingItem = base.getMethod("getTamingItem", ItemStack.class);
            getTamingProgress = base.getMethod("getTamingProgress");
            setTamingProgress = base.getMethod("setTamingProgress", int.class);
            return true;
        } catch (ReflectiveOperationException e) {
            reflectionFailed = true;
            LOG.warn("Useless Reptile taming hooks unavailable", e);
            return false;
        }
    }
}
