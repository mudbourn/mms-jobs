package info.mudbourn.mmsjobs;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Cleric Enchant Extraction: grinding an enchanted tool spends a book from the inventory to keep its enchantments.
public final class EnchantExtraction {

    private EnchantExtraction() {}

    public static void ground(Player player, Container inputs) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        int rank = PowerupRanks.rank(serverPlayer, "alchemist", "enchant_extraction");
        if (rank == 0) return;

        for (int slot = 0; slot < inputs.getContainerSize(); slot++) {
            ItemStack input = inputs.getItem(slot);
            if (input.isEmpty() || input.is(Items.ENCHANTED_BOOK) || !input.isEnchanted()) continue;

            List<Object2IntMap.Entry<Holder<Enchantment>>> kept = new ArrayList<>();
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : input.getEnchantments().entrySet()) {
                if (!entry.getKey().is(EnchantmentTags.CURSE)) kept.add(entry);
            }
            if (kept.isEmpty() || !spendBook(serverPlayer.getInventory())) continue;

            Collections.shuffle(kept, new Random(serverPlayer.getRandom().nextLong()));
            int take = rank >= 3 ? kept.size() : Math.min(rank, kept.size());
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentHelper.updateEnchantments(book, stored -> {
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : kept.subList(0, take)) {
                    stored.set(entry.getKey(), entry.getIntValue());
                }
            });
            serverPlayer.getInventory().placeItemBackInInventory(book);
        }
    }

    private static boolean spendBook(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.BOOK)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
