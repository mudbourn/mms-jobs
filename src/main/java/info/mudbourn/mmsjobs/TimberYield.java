package info.mudbourn.mmsjobs;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Timber for Lumberjack-class players: extra planks when crafting them and extra saplings from felled trees.
public final class TimberYield {
    private static final Map<UUID, BlockPos> PENDING_TREES = new ConcurrentHashMap<>();

    private TimberYield() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer
                && level instanceof ServerLevel serverLevel
                && rank(serverPlayer) > 0
                && VeinBreaker.isNaturalTree(serverLevel, pos, state)) {
                PENDING_TREES.put(serverPlayer.getUUID(), pos.immutable());
            }
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!pos.equals(PENDING_TREES.remove(serverPlayer.getUUID()))) return;

            Item sapling = saplingFor(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
            if (sapling != Items.AIR) Block.popResource(level, pos, new ItemStack(sapling, rank(serverPlayer)));
        });
    }

    // Adds bonus planks to a crafting result the player just took.
    public static void craftedPlanks(Player player, ItemStack result, int amount) {
        if (!(player instanceof ServerPlayer serverPlayer) || !result.is(ItemTags.PLANKS)) return;

        int rank = rank(serverPlayer);
        if (rank == 0) return;

        double bonus = amount * rank * 0.25;
        int extra = (int) bonus + (serverPlayer.getRandom().nextDouble() < bonus % 1 ? 1 : 0);
        if (extra > 0) serverPlayer.getInventory().placeItemBackInInventory(result.copyWithCount(extra));
    }

    public static void forget(UUID playerId) {
        PENDING_TREES.remove(playerId);
    }

    private static int rank(ServerPlayer player) {
        if (!ClassSynergy.has(player, "lumberjack")) return 0;
        return PowerupRanks.baseRank(player, "lumberjack", "timber");
    }

    private static Item saplingFor(Identifier log) {
        String wood = log.getPath()
            .replaceFirst("^stripped_", "")
            .replaceFirst("_(log|wood|stem|hyphae)$", "");
        String sapling = switch (wood) {
            case "mangrove" -> "mangrove_propagule";
            case "crimson", "warped" -> wood + "_fungus";
            default -> wood + "_sapling";
        };
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(log.getNamespace(), sapling));
    }
}
