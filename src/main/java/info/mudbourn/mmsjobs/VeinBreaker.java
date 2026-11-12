package info.mudbourn.mmsjobs;

import com.daqem.jobsplus.player.JobsPlayer;
import com.daqem.jobsplus.player.job.Job;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Lumberjack and Miner job tree felling and vein mining, plus both jobs' passive bonus drops.
public final class VeinBreaker {
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
    private static final int MAX_LOGS = 96;
    private static final int MAX_ORES = 32;
    private static final double MAX_REACH_SQUARED = 24.0 * 24.0;
    private static final int LEAF_SEARCH = 2;

    private record Ability(String job, String powerup, String origin) {}

    private static final Ability LUMBERJACK = new Ability("lumberjack", "timber", "lumberjack");
    private static final Ability MINER = new Ability("miner", "vein_miner", "miner");

    private static final class Run {
        final Deque<BlockPos> queue;
        final ItemStack tool;
        final int rank;
        int wait;

        Run(Deque<BlockPos> queue, ItemStack tool, int rank) {
            this.queue = queue;
            this.tool = tool;
            this.rank = rank;
        }
    }

    private static final Map<UUID, Run> RUNS = new ConcurrentHashMap<>();
    private static final Set<UUID> BREAKING = ConcurrentHashMap.newKeySet();

    private VeinBreaker() {}

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) return;

            bonusDrops(serverLevel, serverPlayer, pos, state);
            if (!BREAKING.contains(serverPlayer.getUUID())) start(serverLevel, serverPlayer, pos, state);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Run run = RUNS.get(player.getUUID());
                if (run != null) step(player, run);
            }
        });
    }

    public static void forget(UUID playerId) {
        RUNS.remove(playerId);
        BREAKING.remove(playerId);
    }

    private static Ability abilityFor(BlockState state, ItemStack tool) {
        if (state.is(BlockTags.LOGS) && tool.is(ItemTags.AXES)) return LUMBERJACK;
        if (state.is(ORES) && tool.is(ItemTags.PICKAXES)) return MINER;
        return null;
    }

    // Rolls a job-level-based chance at an extra copy of a log's or ore's drops.
    private static void bonusDrops(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        ItemStack tool = player.getMainHandItem();
        Ability ability = abilityFor(state, tool);
        if (ability == null) return;

        Job held = held(player, ability.job());
        int jobLevel = held == null ? 0 : held.getLevel();
        double chance = Math.min(0.12, jobLevel / 1000.0);
        if (ClassSynergy.has(player, ability.origin())) chance *= 2;
        if (chance <= 0 || player.getRandom().nextDouble() >= chance) return;

        for (ItemStack drop : Block.getDrops(state, level, pos, null, player, tool)) {
            Block.popResource(level, pos, drop);
        }
    }

    private static Job held(ServerPlayer player, String job) {
        if (!(player instanceof JobsPlayer jobsPlayer)) return null;
        return jobsPlayer.jobsplus$getJob(Identifier.fromNamespaceAndPath("jobsplus", job));
    }

    // Queues the rest of a tree or ore vein after a job holder breaks one block of it, unless sneaking.
    private static void start(ServerLevel level, ServerPlayer player, BlockPos origin, BlockState state) {
        if (player.isShiftKeyDown() || RUNS.containsKey(player.getUUID())) return;

        ItemStack tool = player.getMainHandItem();
        Ability ability = abilityFor(state, tool);
        if (ability == null) return;

        if (held(player, ability.job()) == null) return;

        boolean hasClass = ClassSynergy.has(player, ability.origin());
        if (ability == LUMBERJACK && hasClass) return;

        boolean logs = ability == LUMBERJACK;
        List<BlockPos> found = connected(level, origin, state, logs, logs ? MAX_LOGS : MAX_ORES);
        if (found.isEmpty() || (logs && !hasNaturalLeaves(level, found))) return;

        found.sort((a, b) -> Double.compare(a.distSqr(origin), b.distSqr(origin)));
        int rank = PowerupRanks.rank(player, ability.job(), ability.powerup());
        if (rank == 0 && hasClass) rank = 1;
        RUNS.put(player.getUUID(), new Run(new ArrayDeque<>(found), tool, rank));
    }

    // Whether a log belongs to a natural tree, judged by non-persistent leaves around its logs.
    static boolean isNaturalTree(ServerLevel level, BlockPos origin, BlockState state) {
        if (!state.is(BlockTags.LOGS)) return false;
        List<BlockPos> logs = connected(level, origin, state, true, MAX_LOGS);
        logs.add(origin);
        return hasNaturalLeaves(level, logs);
    }

    private static List<BlockPos> connected(ServerLevel level, BlockPos origin, BlockState state, boolean logs, int max) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        seen.add(origin);
        open.add(origin);
        while (!open.isEmpty() && found.size() < max) {
            BlockPos pos = open.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                BlockPos candidate = next.immutable();
                if (!seen.add(candidate)) continue;

                BlockState other = level.getBlockState(candidate);
                boolean matches = logs ? other.is(BlockTags.LOGS) : other.is(state.getBlock());
                if (!matches) continue;

                found.add(candidate);
                open.add(candidate);
                if (found.size() >= max) break;
            }
        }
        return found;
    }

    private static boolean hasNaturalLeaves(ServerLevel level, List<BlockPos> logs) {
        for (BlockPos log : logs) {
            BlockPos low = log.offset(-LEAF_SEARCH, -LEAF_SEARCH, -LEAF_SEARCH);
            BlockPos high = log.offset(LEAF_SEARCH, LEAF_SEARCH, LEAF_SEARCH);
            for (BlockPos near : BlockPos.betweenClosed(low, high)) {
                BlockState state = level.getBlockState(near);
                if (state.is(BlockTags.LEAVES)
                    && state.hasProperty(LeavesBlock.PERSISTENT)
                    && !state.getValue(LeavesBlock.PERSISTENT)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Breaks the next queued block once its delay has passed, sparing durability by rank.
    private static void step(ServerPlayer player, Run run) {
        if (run.wait-- > 0) return;
        run.wait = switch (run.rank) {
            case 4, 3 -> 0;
            case 2 -> 1;
            case 1 -> 3;
            default -> 5;
        };

        ItemStack tool = player.getMainHandItem();
        BlockPos pos = run.queue.poll();
        if (pos == null || tool != run.tool || tool.isEmpty() || pos.distSqr(player.blockPosition()) > MAX_REACH_SQUARED) {
            RUNS.remove(player.getUUID());
            return;
        }
        if (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 1) {
            RUNS.remove(player.getUUID());
            return;
        }

        int damageBefore = tool.getDamageValue();
        BREAKING.add(player.getUUID());
        try {
            player.gameMode.destroyBlock(pos);
        } finally {
            BREAKING.remove(player.getUUID());
        }
        if (!tool.isEmpty() && player.getRandom().nextDouble() < durabilitySaved(run.rank)) {
            tool.setDamageValue(damageBefore);
        }
    }

    private static double durabilitySaved(int rank) {
        return switch (rank) {
            case 4 -> 0.85;
            case 3 -> 0.7;
            case 2 -> 0.4;
            case 1 -> 0.2;
            default -> 0.0;
        };
    }
}
