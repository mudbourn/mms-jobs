package info.mudbourn.mmsjobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Smith Thrifty Forging (material refunds on crafted gear) and Smelting Yield (extra ingots and gems).
public final class SmithCraft {
    private static final List<TagKey<Item>> GEAR = List.of(tag("tools"), tag("armors"), tag("armor"));
    private static final List<TagKey<Item>> SMELTED = List.of(tag("ingots"), tag("gems"));
    private static final TagKey<Item> RODS = tag("rods");

    private SmithCraft() {}

    // Rolls a chance to hand back one unit of the main material after crafting a tool or armor piece.
    public static void craftedGear(Player player, ItemStack result, List<ItemStack> grid) {
        if (!(player instanceof ServerPlayer serverPlayer) || !isAny(result, GEAR)) return;

        int rank = PowerupRanks.rank(serverPlayer, "smith", "thrifty_forging");
        if (rank == 0 || serverPlayer.getRandom().nextFloat() >= rank * 0.1f) return;

        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack ingredient : grid) {
            if (!ingredient.isEmpty() && !ingredient.is(RODS)) counts.merge(ingredient.getItem(), 1, Integer::sum);
        }
        counts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresent(main -> serverPlayer.getInventory().placeItemBackInInventory(new ItemStack(main.getKey())));
    }

    // Adds a chance at one extra ingot or gem for each one the player takes from a furnace.
    public static void smelted(Player player, ItemStack result, int amount) {
        if (!(player instanceof ServerPlayer serverPlayer) || !isAny(result, SMELTED)) return;

        int rank = PowerupRanks.rank(serverPlayer, "smith", "smelting_yield");
        if (rank == 0) return;

        int extra = 0;
        for (int i = 0; i < amount; i++) {
            if (serverPlayer.getRandom().nextFloat() < rank * 0.1f) extra++;
        }
        if (extra > 0) serverPlayer.getInventory().placeItemBackInInventory(result.copyWithCount(extra));
    }

    private static boolean isAny(ItemStack stack, List<TagKey<Item>> tags) {
        return tags.stream().anyMatch(stack::is);
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }
}
