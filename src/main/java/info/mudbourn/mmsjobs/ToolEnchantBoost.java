package info.mudbourn.mmsjobs;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

// Lumberjack and Miner chance to raise one enchantment level on an axe or pickaxe from an enchanting table.
public final class ToolEnchantBoost {

    private ToolEnchantBoost() {}

    public static void enchanted(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer) || stack.isEmpty()) return;

        String job;
        String powerup;
        if (stack.is(ItemTags.AXES)) {
            job = "lumberjack";
            powerup = "honed_edge";
        } else if (stack.is(ItemTags.PICKAXES)) {
            job = "miner";
            powerup = "tempered_tip";
        } else {
            return;
        }

        int rank = PowerupRanks.rank(serverPlayer, job, powerup);
        float chance = rank * 0.15f;
        if (rank == 0 || serverPlayer.getRandom().nextFloat() >= chance) return;

        int overMax = rank >= 3 ? 1 : 0;
        EnchantmentHelper.updateEnchantments(stack, enchantments -> {
            List<Holder<Enchantment>> raisable = new ArrayList<>();
            for (Holder<Enchantment> enchantment : enchantments.keySet()) {
                int cap = enchantment.value().getMaxLevel() + overMax;
                if (enchantments.getLevel(enchantment) < cap) raisable.add(enchantment);
            }
            if (raisable.isEmpty()) return;

            Holder<Enchantment> chosen = raisable.get(serverPlayer.getRandom().nextInt(raisable.size()));
            enchantments.set(chosen, enchantments.getLevel(chosen) + 1);
        });
    }
}
