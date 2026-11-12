package info.mudbourn.mmsjobs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

// Explorer loot XP and the Silver Tongue villager and piglin discounts.
public final class ExplorerPerks {
    private static final Map<Piglin, UUID> BARTERERS = new WeakHashMap<>();

    private ExplorerPerks() {}

    // Pays Explorer XP the first time a player rolls a container's or brushed block's loot.
    public static void lootGenerated(ServerPlayer player) {
        JobXpGrants.grant(player, "explorer", JobXpGrants.HARD, 6, 10);
    }

    // Takes a share off each trade's base cost for an Explorer with Silver Tongue.
    public static void haggle(Player player, MerchantOffers offers) {
        float rate = switch (PowerupRanks.rank(player, "explorer", "silver_tongue")) {
            case 4 -> 0.20f;
            case 3 -> 0.15f;
            case 2 -> 0.10f;
            case 1 -> 0.05f;
            default -> 0.0f;
        };
        if (rate == 0.0f) return;

        for (MerchantOffer offer : offers) {
            int base = offer.getBaseCostA().getCount();
            offer.addToSpecialPriceDiff(-Math.max(1, (int) Math.ceil(base * rate)));
        }
    }

    // Remembers who threw the gold a piglin picked up.
    public static void goldPickedUp(Piglin piglin, Entity thrower) {
        if (thrower instanceof ServerPlayer player) BARTERERS.put(piglin, player.getUUID());
    }

    // Rolls a chance to double a barter for the Explorer who paid for it.
    public static void barter(Piglin piglin, List<ItemStack> items) {
        UUID payer = BARTERERS.remove(piglin);
        if (payer == null || piglin.level().getServer() == null) return;

        ServerPlayer player = piglin.level().getServer().getPlayerList().getPlayer(payer);
        if (player == null) return;

        float chance = switch (PowerupRanks.rank(player, "explorer", "silver_tongue")) {
            case 4 -> 0.60f;
            case 3 -> 0.45f;
            case 2 -> 0.30f;
            case 1 -> 0.15f;
            default -> 0.0f;
        };
        if (chance == 0.0f || piglin.getRandom().nextFloat() >= chance) return;

        List<ItemStack> copies = items.stream().map(ItemStack::copy).toList();
        items.addAll(copies);
    }
}
