package info.mudbourn.mmsjobs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Explorer Lucky Finds rerolls for chest, vault and trial spawner loot: a pick may become a rarer entry of its pool, never adding items.
public final class ChestLootReroll {

    // Reroll chance at Lucky Finds rank 4 (rank III plus the Explorer class), before the Fairy doubling.
    private static final float MAX_RATE = 0.325F;

    // The player a trial spawner is ejecting a reward for, set only during that ejection.
    private static final ThreadLocal<ServerPlayer> REWARDEE = new ThreadLocal<>();

    private ChestLootReroll() {}

    // Sets or clears the player a trial spawner reward is being rolled for.
    public static void setRewardee(ServerPlayer player) {
        if (player == null) REWARDEE.remove();
        else REWARDEE.set(player);
    }

    // The chance each pick is rerolled for the player it is rolled for, or 0 outside chest, vault and trial spawner loot.
    public static float rate(LootContext context) {
        ServerPlayer rewardee = REWARDEE.get();
        if (rewardee != null) return rate(rewardee);

        ItemInstance tool = context.getOptional(LootContextParams.TOOL);
        boolean vault = tool != null && (tool.is(Items.TRIAL_KEY) || tool.is(Items.OMINOUS_TRIAL_KEY));
        if ((tool != null && !vault)
            || context.hasParameter(LootContextParams.BLOCK_STATE)
            || context.hasParameter(LootContextParams.DAMAGE_SOURCE)) return 0.0F;
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return 0.0F;
        return rate(player);
    }

    // The reroll chance from the player's Lucky Finds rank, doubled for a Fairy.
    private static float rate(ServerPlayer player) {
        int rank = PowerupRanks.rank(player, "explorer", "lucky_finds");
        if (rank == 0) return 0.0F;

        float rate = MAX_RATE * rank / 4.0F;
        return FairyOrigin.is(player) ? rate * 2.0F : rate;
    }

    // Vanilla's weighted pick, then a chance to replace a non-empty pick with a lower-weight entry's items.
    public static void pick(List<LootPoolEntryContainer> containers, Consumer<ItemStack> output, LootContext context, float rate) {
        float luck = context.getLuck();
        List<LootPoolEntry> entries = new ArrayList<>();
        for (LootPoolEntryContainer container : containers) {
            container.expand(context, entry -> {
                if (entry.getWeight(luck) > 0) entries.add(entry);
            });
        }
        if (entries.isEmpty()) return;

        RandomSource random = context.getRandom();
        LootPoolEntry chosen = weighted(entries, luck, random);
        List<ItemStack> items = new ArrayList<>();
        chosen.createItemStack(items::add, context);

        if (!items.isEmpty() && random.nextFloat() < rate) {
            int weight = chosen.getWeight(luck);
            List<LootPoolEntry> rarer = new ArrayList<>();
            for (LootPoolEntry entry : entries) {
                if (entry.getWeight(luck) < weight) rarer.add(entry);
            }
            if (!rarer.isEmpty()) {
                List<ItemStack> upgraded = new ArrayList<>();
                weighted(rarer, luck, random).createItemStack(upgraded::add, context);
                if (!upgraded.isEmpty()) items = upgraded;
            }
        }
        items.forEach(output);
    }

    // One entry drawn by weight.
    private static LootPoolEntry weighted(List<LootPoolEntry> entries, float luck, RandomSource random) {
        int total = 0;
        for (LootPoolEntry entry : entries) total += entry.getWeight(luck);
        int roll = random.nextInt(total);
        for (LootPoolEntry entry : entries) {
            roll -= entry.getWeight(luck);
            if (roll < 0) return entry;
        }
        return entries.get(entries.size() - 1);
    }
}
