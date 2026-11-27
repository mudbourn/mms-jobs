package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Miner job area mining: a sneaking job holder breaks a square around the mined block with a pickaxe or shovel.
public final class MinerAreaBreak {
    private static final Identifier MINER = Identifier.fromNamespaceAndPath("jobsplus", "miner");
    private static final int SQUARE_LEVEL = 25;
    private static final int WIDE_LEVEL = 75;
    private static final Set<UUID> BREAKING = ConcurrentHashMap.newKeySet();

    private MinerAreaBreak() {}

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) return;
            if (!serverPlayer.isShiftKeyDown() || !BREAKING.add(serverPlayer.getUUID())) return;
            try {
                breakAround(serverLevel, serverPlayer, pos, state);
            } finally {
                BREAKING.remove(serverPlayer.getUUID());
            }
        });
    }

    private static void breakAround(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState mined) {
        ItemStack tool = player.getMainHandItem();
        if (!suits(tool, mined)) return;

        Job miner = player instanceof JobsPlayer jobsPlayer ? jobsPlayer.jobsplus$getJob(MINER) : null;
        if (miner == null || miner.getLevel() < SQUARE_LEVEL) return;

        int radius = miner.getLevel() >= WIDE_LEVEL ? 2 : 1;
        Direction.Axis axis = facingAxis(player);
        float hardness = mined.getDestroySpeed(level, origin);
        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                if (a == 0 && b == 0) continue;
                if (tool.isEmpty() || tool != player.getMainHandItem()) return;

                BlockPos pos = switch (axis) {
                    case X -> origin.offset(0, a, b);
                    case Y -> origin.offset(a, 0, b);
                    case Z -> origin.offset(a, b, 0);
                };
                BlockState state = level.getBlockState(pos);
                float other = state.getDestroySpeed(level, pos);
                if (state.isAir() || other < 0 || other > hardness + 1.0F || !suits(tool, state)) continue;

                player.gameMode.destroyBlock(pos);
            }
        }
    }

    // Whether a tool is a pickaxe or shovel meant for this block.
    private static boolean suits(ItemStack tool, BlockState state) {
        if (tool.is(ItemTags.PICKAXES)) return state.is(BlockTags.MINEABLE_WITH_PICKAXE) && tool.isCorrectToolForDrops(state);
        if (tool.is(ItemTags.SHOVELS)) return state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        return false;
    }

    // The axis the player looks along, so the square lies flat against the mined face.
    private static Direction.Axis facingAxis(ServerPlayer player) {
        if (Math.abs(player.getXRot()) > 45.0F) return Direction.Axis.Y;
        return player.getDirection().getAxis();
    }
}
