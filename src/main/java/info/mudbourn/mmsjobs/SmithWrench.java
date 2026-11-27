package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

// Smith job armor stand work: a sneaking empty-handed Smith repairs the stand's gear with their experience, or toggles its arms.
public final class SmithWrench {
    private static final Identifier SMITH = Identifier.fromNamespaceAndPath("jobsplus", "smith");
    private static final int REPAIR_LEVEL = 25;
    private static final int MASTER_LEVEL = 60;

    private SmithWrench() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(entity instanceof ArmorStand stand)) {
                return InteractionResult.PASS;
            }
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            Job smith = player instanceof JobsPlayer jobsPlayer ? jobsPlayer.jobsplus$getJob(SMITH) : null;
            if (smith == null) return InteractionResult.PASS;

            if (smith.getLevel() >= REPAIR_LEVEL && repair(serverPlayer, stand, smith.getLevel())) {
                level.playSound(null, stand.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6F, 1.2F);
                return InteractionResult.SUCCESS_SERVER;
            }
            stand.setShowArms(!stand.showArms());
            level.playSound(null, stand.blockPosition(), SoundEvents.ARMOR_STAND_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS_SERVER;
        });
    }

    // Repairs every damaged item on the stand, two durability per experience point, or three for a master Smith.
    private static boolean repair(ServerPlayer player, ArmorStand stand, int jobLevel) {
        int perPoint = jobLevel >= MASTER_LEVEL ? 3 : 2;
        boolean repaired = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = stand.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamaged()) continue;

            int points = Math.min(player.totalExperience, (stack.getDamageValue() + perPoint - 1) / perPoint);
            if (points <= 0) break;

            stack.setDamageValue(Math.max(0, stack.getDamageValue() - points * perPoint));
            player.giveExperiencePoints(-points);
            repaired = true;
        }
        return repaired;
    }
}
