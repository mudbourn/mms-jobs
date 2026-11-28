package info.mudbourn.mmsjobs;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

// Luck's boost to block and mob drops: pool rolls scale linearly up to 2.2x at Luck 8, Lucky Finds III with the Explorer class.
public final class LuckyDrops {
    private static final float FULL_LUCK = 8.0F;
    private static final float MAX_BONUS = 1.2F;

    private LuckyDrops() {}

    // The rolls a pool makes for the player behind a block break or mob kill, scaled by their Luck and rounded by chance.
    public static int rolls(int rolls, LootContext context) {
        if (rolls <= 0) return rolls;

        Player player = player(context);
        if (player == null) return rolls;

        float luck = Mth.clamp(player.getLuck(), 0.0F, FULL_LUCK);
        if (luck <= 0.0F) return rolls;

        float scaled = rolls * (1.0F + MAX_BONUS * luck / FULL_LUCK);
        int whole = Mth.floor(scaled);
        return context.getRandom().nextFloat() < scaled - whole ? whole + 1 : whole;
    }

    // The breaking player of block loot or the killing player of mob loot; null for chests, fishing and the rest.
    private static @Nullable Player player(LootContext context) {
        if (context.hasParameter(LootContextParams.BLOCK_STATE)) {
            return context.getOptional(LootContextParams.THIS_ENTITY) instanceof Player breaker ? breaker : null;
        }
        if (!context.hasParameter(LootContextParams.DAMAGE_SOURCE)) return null;
        if (context.getOptional(LootContextParams.LAST_DAMAGE_PLAYER) instanceof Player killer) return killer;
        return context.getOptional(LootContextParams.ATTACKING_ENTITY) instanceof Player attacker ? attacker : null;
    }
}
